package ir.devtrader.investor.util

import ir.devtrader.investor.data.remote.dto.Investor
import java.util.Locale

enum class AccountStatusLevel { PENDING_APPROVAL, FROZEN, TRADING_PAUSED, ACTIVE }

data class AccountStatus(val level: AccountStatusLevel, val title: String, val subtext: String)

/**
 * Computed banner, not a single API field — mirrors the web dashboard's own priority order
 * exactly (don't reorder): Pending Approval > Frozen > Trading Paused > Active.
 */
fun computeAccountStatus(investor: Investor, hasApiKey: Boolean): AccountStatus = when {
    !investor.isActive -> AccountStatus(
        AccountStatusLevel.PENDING_APPROVAL,
        "Pending Approval",
        if (hasApiKey) {
            "Your API key is bound — an admin still needs to activate mirror trading."
        } else {
            "Bind your API key in Settings, then an admin will review and activate your account."
        },
    )
    investor.frozen -> AccountStatus(
        AccountStatusLevel.FROZEN,
        "Frozen",
        buildString {
            append("New mirrored trades are paused — you have an outstanding balance of $")
            append("%.2f".format(Locale.US, investor.debt))
            append(". Anything already open still closes normally. Contact the admin to settle it.")
            investor.frozenReason?.let { append(" ($it)") }
        },
    )
    !investor.tradingEnabled -> AccountStatus(
        AccountStatusLevel.TRADING_PAUSED,
        "Trading Paused",
        "You've paused new mirrored trades yourself — anything already open still closes normally. " +
            "Turn it back on anytime in Settings.",
    )
    else -> AccountStatus(
        AccountStatusLevel.ACTIVE,
        "Active",
        "Mirror trading is on — every position opened on the main account opens on yours too, " +
            "sized to your own margin ratio (%.2f%%).".format(Locale.US, investor.marginRatio),
    )
}
