package ir.devtrader.investor.ui.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.devtrader.investor.data.remote.RealtimeGateway
import ir.devtrader.investor.data.remote.dto.Alarm
import ir.devtrader.investor.data.repository.InvestorRepository
import ir.devtrader.investor.data.repository.SymbolsCache
import ir.devtrader.investor.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlarmsUiState(
    val isLoading: Boolean = true,
    val alarms: List<Alarm> = emptyList(),
    val error: String? = null,
    val symbols: List<String> = emptyList(),
    val symbol: String = "",
    val condition: String = "above",
    val price: String = "",
    val sms: Boolean = false,
    val call: Boolean = false,
    val isSubmitting: Boolean = false,
    val formError: String? = null,
) {
    val filteredSymbols: List<String>
        get() = if (symbol.isBlank()) symbols else symbols.filter { it.contains(symbol, ignoreCase = true) }
}

class AlarmsViewModel(
    private val investorRepository: InvestorRepository,
    private val realtimeGateway: RealtimeGateway,
    symbolsCache: SymbolsCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmsUiState())
    val uiState: StateFlow<AlarmsUiState> = _uiState.asStateFlow()

    init {
        refresh()
        // Symbols were already fetched into SymbolsCache while the app was loading (or at
        // login) — just mirror that in-memory list here, no network call from this screen, so
        // the dropdown is populated instantly.
        viewModelScope.launch {
            symbolsCache.symbols.collect { symbols ->
                _uiState.value = _uiState.value.copy(symbols = symbols)
            }
        }
        // Prune live rather than waiting for a re-fetch — the alarm is already gone server-side
        // by the time this event arrives (alarms are one-shot). The app-level toast for this
        // same event is handled separately (see NavGraph), independent of whether this screen
        // is open.
        viewModelScope.launch {
            realtimeGateway.alarmTriggered.collect { event ->
                _uiState.value = _uiState.value.copy(
                    alarms = _uiState.value.alarms.filterNot { it._id == event._id },
                )
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = investorRepository.getAlarms()) {
                is ApiResult.Success ->
                    _uiState.value = _uiState.value.copy(isLoading = false, alarms = result.data.alarms)
                is ApiResult.Error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun onSymbolChange(value: String) {
        _uiState.value = _uiState.value.copy(symbol = value, formError = null)
    }

    fun onConditionChange(value: String) {
        _uiState.value = _uiState.value.copy(condition = value, formError = null)
    }

    fun onPriceChange(value: String) {
        _uiState.value = _uiState.value.copy(price = value, formError = null)
    }

    fun onSmsChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(sms = value)
    }

    fun onCallChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(call = value)
    }

    fun addAlarm() {
        val state = _uiState.value
        val price = state.price.toDoubleOrNull()
        if (state.symbol.isBlank()) {
            _uiState.value = state.copy(formError = "Symbol is required")
            return
        }
        if (price == null || price <= 0) {
            _uiState.value = state.copy(formError = "Price must be a positive number")
            return
        }
        _uiState.value = state.copy(isSubmitting = true, formError = null)
        viewModelScope.launch {
            val result = investorRepository.createAlarm(
                symbol = state.symbol.trim(),
                condition = state.condition,
                price = price,
                sms = state.sms,
                call = state.call,
            )
            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        symbol = "",
                        price = "",
                        sms = false,
                        call = false,
                    )
                    refresh()
                }
                is ApiResult.Error ->
                    _uiState.value = _uiState.value.copy(isSubmitting = false, formError = result.message)
            }
        }
    }

    fun deleteAlarm(id: String) {
        val previous = _uiState.value.alarms
        _uiState.value = _uiState.value.copy(alarms = previous.filterNot { it._id == id })
        viewModelScope.launch {
            val result = investorRepository.deleteAlarm(id)
            if (result is ApiResult.Error) {
                _uiState.value = _uiState.value.copy(alarms = previous, error = result.message)
            }
        }
    }

    companion object {
        fun factory(
            investorRepository: InvestorRepository,
            realtimeGateway: RealtimeGateway,
            symbolsCache: SymbolsCache,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { AlarmsViewModel(investorRepository, realtimeGateway, symbolsCache) }
        }
    }
}
