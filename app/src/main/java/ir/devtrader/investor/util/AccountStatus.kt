package ir.devtrader.investor.util

import android.content.Context
import ir.devtrader.investor.R
import ir.devtrader.investor.data.remote.dto.Investor
import java.util.Locale

enum class AccountStatusLevel { PENDING_APPROVAL, FROZEN, TRADING_PAUSED, ACTIVE }

data class AccountStatus(val level: AccountStatusLevel, val title: String, val subtext: String)

/**
 * Computed banner, not a single API field — mirrors the web dashboard's own priority order
 * exactly (don't reorder): Pending Approval > Frozen > Trading Paused > Active.
 */
fun computeAccountStatus(context: Context, investor: Investor, hasApiKey: Boolean): AccountStatus = when {
    !investor.isActive -> AccountStatus(
        AccountStatusLevel.PENDING_APPROVAL,
        context.getString(R.string.account_status_pending_title),
        if (hasApiKey) {
            context.getString(R.string.account_status_pending_body_with_key)
        } else {
            context.getString(R.string.account_status_pending_body_no_key)
        },
    )
    investor.frozen -> AccountStatus(
        AccountStatusLevel.FROZEN,
        context.getString(R.string.account_status_frozen_title),
        buildString {
            append(
                context.getString(
                    R.string.account_status_frozen_body,
                    "%.2f".format(Locale.US, investor.debt),
                ),
            )
            investor.frozenReason?.let {
                append(context.getString(R.string.account_status_frozen_reason_suffix, it))
            }
        },
    )
    !investor.tradingEnabled -> AccountStatus(
        AccountStatusLevel.TRADING_PAUSED,
        context.getString(R.string.account_status_trading_paused_title),
        context.getString(R.string.account_status_trading_paused_body),
    )
    else -> AccountStatus(
        AccountStatusLevel.ACTIVE,
        context.getString(R.string.account_status_active_title),
        context.getString(
            R.string.account_status_active_body,
            "%.2f".format(Locale.US, investor.marginRatio),
        ),
    )
}
