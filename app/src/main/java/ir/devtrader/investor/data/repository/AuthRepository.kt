package ir.devtrader.investor.data.repository

import ir.devtrader.investor.data.local.SessionManager
import ir.devtrader.investor.data.remote.ApiService
import ir.devtrader.investor.data.remote.dto.LoginRequest
import ir.devtrader.investor.data.remote.dto.LoginResponse
import ir.devtrader.investor.data.remote.dto.RegisterRequest
import ir.devtrader.investor.data.remote.dto.RegisterResponse
import ir.devtrader.investor.util.ApiResult
import ir.devtrader.investor.util.toApiResultWithError
import kotlinx.serialization.json.Json

class AuthRepository(
    private val api: ApiService,
    private val sessionManager: SessionManager,
    private val json: Json,
) {
    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            val result = response.toApiResultWithError(json, LoginResponse.serializer()) { it.error }
            if (result is ApiResult.Success) {
                val body = result.data
                if (body.status && body.token != null && body.investor != null) {
                    sessionManager.onLoginSuccess(body.token, body.investor)
                } else {
                    return ApiResult.Error(body.error ?: "Login failed")
                }
            }
            result
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error, please try again")
        }
    }

    suspend fun register(
        email: String,
        password: String,
        first: String,
        last: String,
        phone: String,
        acceptedTerms: Boolean,
    ): ApiResult<RegisterResponse> {
        return try {
            val response = api.register(RegisterRequest(email, password, first, last, phone, acceptedTerms))
            val result = response.toApiResultWithError(json, RegisterResponse.serializer()) { it.error }
            if (result is ApiResult.Success) {
                val body = result.data
                if (body.status && body.token != null && body.investor != null) {
                    sessionManager.onLoginSuccess(body.token, body.investor)
                } else {
                    return ApiResult.Error(body.error ?: "Registration failed")
                }
            }
            result
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error, please try again")
        }
    }

    fun logout() = sessionManager.clearSession()
}
