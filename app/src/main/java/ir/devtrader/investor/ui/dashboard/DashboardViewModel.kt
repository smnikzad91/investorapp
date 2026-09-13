package ir.devtrader.investor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import ir.devtrader.investor.data.remote.dto.DashboardResponse
import ir.devtrader.investor.data.repository.InvestorRepository
import ir.devtrader.investor.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val dashboard: DashboardResponse? = null,
    val error: String? = null,
)

class DashboardViewModel(private val investorRepository: InvestorRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = investorRepository.getDashboard()) {
                is ApiResult.Success ->
                    _uiState.value = _uiState.value.copy(isLoading = false, dashboard = result.data)
                is ApiResult.Error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    companion object {
        fun factory(investorRepository: InvestorRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { DashboardViewModel(investorRepository) }
        }
    }
}
