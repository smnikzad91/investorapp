package ir.devtrader.investor.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class Investor(
    val id: String,
    val email: String,
    val first: String,
    val last: String,
    val phone: String = "",
    val isActive: Boolean = false,
    val frozen: Boolean = false,
    val frozenReason: String? = null,
    val tradingEnabled: Boolean = false,
    val marginRatio: Double = 0.0,
    val profitSharePercent: Double? = null,
    val debt: Double = 0.0,
)

@Serializable
data class LoginResponse(
    val status: Boolean,
    val token: String? = null,
    val investor: Investor? = null,
    val error: String? = null,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val first: String,
    val last: String,
    val phone: String,
    val acceptedTerms: Boolean,
)

@Serializable
data class RegisterResponse(
    val status: Boolean,
    val token: String? = null,
    val investor: Investor? = null,
    val error: String? = null,
)
