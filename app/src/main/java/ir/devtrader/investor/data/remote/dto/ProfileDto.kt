package ir.devtrader.investor.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileRequest(
    val first: String,
    val last: String,
    val phone: String,
)

@Serializable
data class ProfileResponse(
    val status: Boolean,
    val message: String? = null,
)
