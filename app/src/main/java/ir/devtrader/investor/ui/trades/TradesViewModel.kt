package ir.devtrader.investor.ui.trades

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
import kotlinx.serialization.json.JsonObject

data class TradesUiState(
    val isLoading: Boolean = true,
    val hasApiKey: Boolean = true,
    val trades: List<JsonObject> = emptyList(),
    val accountError: String? = null,
    val error: String? = null,
)

class TradesViewModel(private val investorRepository: InvestorRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TradesUiState())
    val uiState: StateFlow<TradesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = investorRepository.getTrades()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasApiKey = result.data.hasApiKey,
                    trades = result.data.trades,
                    accountError = result.data.accountError,
                )
                is ApiResult.Error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    companion object {
        fun factory(investorRepository: InvestorRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { TradesViewModel(investorRepository) }
        }
    }
}
