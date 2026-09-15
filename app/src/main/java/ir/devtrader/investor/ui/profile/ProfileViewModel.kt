package ir.devtrader.investor.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.devtrader.investor.R
import ir.devtrader.investor.data.repository.InvestorRepository
import ir.devtrader.investor.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val email: String = "",
    val first: String = "",
    val last: String = "",
    val phone: String = "",
    val isSaving: Boolean = false,
    val resultMessage: String? = null,
    val isResultError: Boolean = false,
    val error: String? = null,
)

/** Pre-filled from GET /dashboard's investor object, per spec — there's no dedicated GET /profile. */
class ProfileViewModel(application: Application, private val investorRepository: InvestorRepository) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

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
                        email = investor.email,
                        first = investor.first,
                        last = investor.last,
                        phone = investor.phone,
                    )
                }
                is ApiResult.Error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun onFirstChange(value: String) {
        _uiState.value = _uiState.value.copy(first = value, resultMessage = null)
    }

    fun onLastChange(value: String) {
        _uiState.value = _uiState.value.copy(last = value, resultMessage = null)
    }

    fun onPhoneChange(value: String) {
        _uiState.value = _uiState.value.copy(phone = value, resultMessage = null)
    }

    fun save() {
        val state = _uiState.value
        if (state.first.isBlank() || state.last.isBlank() || state.phone.isBlank()) {
            _uiState.value = state.copy(
                resultMessage = getApplication<Application>().getString(R.string.profile_error_missing_fields),
                isResultError = true,
            )
            return
        }
        _uiState.value = state.copy(isSaving = true, resultMessage = null)
        viewModelScope.launch {
            when (val result = investorRepository.updateProfile(state.first.trim(), state.last.trim(), state.phone.trim())) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    resultMessage = getApplication<Application>().getString(R.string.profile_saved),
                    isResultError = false,
                )
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    resultMessage = result.message,
                    isResultError = true,
                )
            }
        }
    }

    companion object {
        fun factory(application: Application, investorRepository: InvestorRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProfileViewModel(application, investorRepository) }
        }
    }
}
