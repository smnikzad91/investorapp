package ir.devtrader.investor.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.devtrader.investor.data.repository.InvestorRepository
import ir.devtrader.investor.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class SettingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // Profit share — read-only, admin-set.
    val profitSharePercent: Double? = null,

    // Trading control.
    val tradingEnabled: Boolean = false,
    val isTogglingTrading: Boolean = false,
    val tradingToggleError: String? = null,

    // Margin ratio.
    val marginRatioInput: String = "",
    val isSavingMarginRatio: Boolean = false,
    val marginRatioResultMessage: String? = null,
    val isMarginRatioResultError: Boolean = false,

    // Bind API key.
    val key: String = "",
    val secret: String = "",
    val isSubmittingKey: Boolean = false,
    val showBindKeyConfirmDialog: Boolean = false,
    val bindKeyResultMessage: String? = null,
    val isBindKeyResultError: Boolean = false,
    val bindKeyWarning: String? = null,
)

/** Four independent sections on one screen, per spec — Profit Share, Trading Control, Margin Ratio, Bind API Key. */
class SettingsViewModel(private val investorRepository: InvestorRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = investorRepository.getDashboard()) {
                is ApiResult.Success -> {
                    val investor = result.data.investor
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profitSharePercent = investor.profitSharePercent,
                        tradingEnabled = investor.tradingEnabled,
                        marginRatioInput = "%.2f".format(Locale.US, investor.marginRatio),
                    )
                }
                is ApiResult.Error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun toggleTrading() {
        _uiState.value = _uiState.value.copy(isTogglingTrading = true, tradingToggleError = null)
        viewModelScope.launch {
            when (val result = investorRepository.toggleTradingEnabled()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isTogglingTrading = false,
                    tradingEnabled = result.data.tradingEnabled ?: _uiState.value.tradingEnabled,
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isTogglingTrading = false,
                    tradingToggleError = result.message,
                )
            }
        }
    }

    fun onMarginRatioChange(value: String) {
        _uiState.value = _uiState.value.copy(marginRatioInput = value, marginRatioResultMessage = null)
    }

    fun saveMarginRatio() {
        val state = _uiState.value
        val ratio = state.marginRatioInput.toDoubleOrNull()
        if (ratio == null || ratio <= 0 || ratio > 100) {
            _uiState.value = state.copy(
                marginRatioResultMessage = "Margin ratio must be a number between 0 and 100.",
                isMarginRatioResultError = true,
            )
            return
        }
        _uiState.value = state.copy(isSavingMarginRatio = true, marginRatioResultMessage = null)
        viewModelScope.launch {
            when (val result = investorRepository.updateMarginRatio(ratio)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isSavingMarginRatio = false,
                    marginRatioResultMessage = "Margin ratio saved",
                    isMarginRatioResultError = false,
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isSavingMarginRatio = false,
                    marginRatioResultMessage = result.message,
                    isMarginRatioResultError = true,
                )
            }
        }
    }

    fun onKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(key = value, bindKeyResultMessage = null)
    }

    fun onSecretChange(value: String) {
        _uiState.value = _uiState.value.copy(secret = value, bindKeyResultMessage = null)
    }

    fun requestBindKeyConfirmation() {
        if (_uiState.value.key.isBlank() || _uiState.value.secret.isBlank()) {
            _uiState.value = _uiState.value.copy(
                bindKeyResultMessage = "Enter both the API key and secret",
                isBindKeyResultError = true,
            )
            return
        }
        _uiState.value = _uiState.value.copy(showBindKeyConfirmDialog = true)
    }

    fun dismissBindKeyConfirmation() {
        _uiState.value = _uiState.value.copy(showBindKeyConfirmDialog = false)
    }

    fun confirmBindKey() {
        val state = _uiState.value
        _uiState.value = state.copy(showBindKeyConfirmDialog = false, isSubmittingKey = true, bindKeyResultMessage = null)
        viewModelScope.launch {
            when (val result = investorRepository.bindKey(state.key.trim(), state.secret.trim())) {
                is ApiResult.Success -> {
                    val body = result.data
                    _uiState.value = _uiState.value.copy(
                        isSubmittingKey = false,
                        bindKeyResultMessage = "API key saved. Available: ${body.available ?: "-"}, equity: ${body.equity ?: "-"}",
                        isBindKeyResultError = false,
                        bindKeyWarning = body.tradePermissionWarning,
                        key = "",
                        secret = "",
                    )
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isSubmittingKey = false,
                    bindKeyResultMessage = result.message,
                    isBindKeyResultError = true,
                )
            }
        }
    }

    companion object {
        fun factory(investorRepository: InvestorRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(investorRepository) }
        }
    }
}
