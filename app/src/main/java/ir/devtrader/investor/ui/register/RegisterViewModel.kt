package ir.devtrader.investor.ui.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.devtrader.investor.R
import ir.devtrader.investor.data.repository.AuthRepository
import ir.devtrader.investor.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val first: String = "",
    val last: String = "",
    val phone: String = "",
    val acceptedTerms: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class RegisterViewModel(application: Application, private val authRepository: AuthRepository) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, error = null)
    }

    fun onFirstChange(value: String) {
        _uiState.value = _uiState.value.copy(first = value, error = null)
    }

    fun onLastChange(value: String) {
        _uiState.value = _uiState.value.copy(last = value, error = null)
    }

    fun onPhoneChange(value: String) {
        _uiState.value = _uiState.value.copy(phone = value, error = null)
    }

    fun onAcceptedTermsChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(acceptedTerms = value, error = null)
    }

    fun register() {
        val context = getApplication<Application>()
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank() || state.first.isBlank() ||
            state.last.isBlank() || state.phone.isBlank()
        ) {
            _uiState.value = state.copy(error = context.getString(R.string.register_error_missing_fields))
            return
        }
        if (state.password.length < 6) {
            _uiState.value = state.copy(error = context.getString(R.string.register_error_password_length))
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(error = context.getString(R.string.register_error_password_mismatch))
            return
        }
        if (!state.acceptedTerms) {
            _uiState.value = state.copy(error = context.getString(R.string.register_error_terms_required))
            return
        }
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.register(
                email = state.email.trim(),
                password = state.password,
                first = state.first.trim(),
                last = state.last.trim(),
                phone = state.phone.trim(),
                acceptedTerms = state.acceptedTerms,
            )
            when (result) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    companion object {
        fun factory(application: Application, authRepository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { RegisterViewModel(application, authRepository) }
        }
    }
}
