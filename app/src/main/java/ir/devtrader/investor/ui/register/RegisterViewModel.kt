package ir.devtrader.investor.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

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
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank() || state.first.isBlank() ||
            state.last.isBlank() || state.phone.isBlank()
        ) {
            _uiState.value = state.copy(error = "Fill in all fields")
            return
        }
        if (state.password.length < 6) {
            _uiState.value = state.copy(error = "Password must be at least 6 characters")
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(error = "Passwords don't match")
            return
        }
        if (!state.acceptedTerms) {
            _uiState.value = state.copy(error = "You must accept the risk disclosure / terms to register")
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
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { RegisterViewModel(authRepository) }
        }
    }
}
