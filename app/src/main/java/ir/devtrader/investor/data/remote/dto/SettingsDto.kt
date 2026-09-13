package ir.devtrader.investor.data.remote.dto

import kotlinx.serialization.Serializable

/** No request body — this flips whatever the current value is server-side. */
@Serializable
data class ToggleTradingResponse(
    val status: Boolean,
    val tradingEnabled: Boolean? = null,
    val message: String? = null,
)

@Serializable
data class MarginRatioRequest(
    val marginRatio: Double,
)

@Serializable
data class MarginRatioResponse(
    val status: Boolean,
    val message: String? = null,
)
