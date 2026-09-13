package ir.devtrader.investor.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DebtLedgerEntry(
    val _id: String,
    val type: String,
    val symbol: String? = null,
    val side: String? = null,
    val entryPrice: String? = null,
    val closePrice: String? = null,
    val realizedPnl: Double? = null,
    val profitSharePercent: Double? = null,
    val debtDelta: Double,
    val balanceAfter: Double,
    val note: String? = null,
    val createdAt: String,
)

@Serializable
data class DebtLedgerResponse(
    val status: Boolean,
    val debt: Double,
    val entries: List<DebtLedgerEntry> = emptyList(),
)
