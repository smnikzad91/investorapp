package ir.devtrader.investor.data.local

import ir.devtrader.investor.data.remote.dto.Investor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for "are we logged in". The auth interceptor clears the token
 * synchronously on any 401 (no refresh-token flow exists yet); the nav graph observes
 * [isLoggedIn] and bounces back to Login whenever it flips to false.
 */
class SessionManager(private val tokenManager: TokenManager) {

    private val _isLoggedIn = MutableStateFlow(tokenManager.getToken() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentInvestor = MutableStateFlow<Investor?>(null)
    val currentInvestor: StateFlow<Investor?> = _currentInvestor.asStateFlow()

    fun token(): String? = tokenManager.getToken()

    fun onLoginSuccess(token: String, investor: Investor) {
        tokenManager.saveToken(token)
        _currentInvestor.value = investor
        _isLoggedIn.value = true
    }

    fun clearSession() {
        tokenManager.clearToken()
        _currentInvestor.value = null
        _isLoggedIn.value = false
    }
}
