package ir.devtrader.investor.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Raw Bitunix shape, not guaranteed — kept as opaque JSON and rendered generically. */
@Serializable
data class PositionsResponse(
    val status: Boolean,
    val hasApiKey: Boolean = true,
    val positions: List<JsonObject> = emptyList(),
    val accountError: String? = null,
)

/** Raw Bitunix shape, not guaranteed — kept as opaque JSON and rendered generically. */
@Serializable
data class TradesResponse(
    val status: Boolean,
    val hasApiKey: Boolean = true,
    val trades: List<JsonObject> = emptyList(),
    val accountError: String? = null,
)
