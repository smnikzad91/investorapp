package ir.devtrader.investor.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class Assets(
    val status: Boolean = false,
    val available: Double = 0.0,
    val margin: Double = 0.0,
    val crossUnrealizedPNL: Double = 0.0,
    val isolationUnrealizedPNL: Double = 0.0,
)

@Serializable
data class TradeSummary(
    val totalTrades: Int = 0,
    val sampleSize: Int = 0,
    val totalRealizedPnl: Double = 0.0,
    val winRate: Double? = null,
)

@Serializable
data class DashboardResponse(
    val status: Boolean,
    val investor: Investor,
    val hasApiKey: Boolean,
    val assets: Assets? = null,
    val openPositionsCount: Int = 0,
    val summary: TradeSummary = TradeSummary(),
    val accountError: String? = null,
)
