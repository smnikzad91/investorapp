package ir.devtrader.investor.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BindKeyRequest(
    val key: String,
    val secret: String,
)

@Serializable
data class BindKeyResponse(
    val status: Boolean,
    val available: Double? = null,
    val equity: Double? = null,
    val tradePermissionWarning: String? = null,
    val message: String? = null,
)
