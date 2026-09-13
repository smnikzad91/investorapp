package ir.devtrader.investor.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.devtrader.investor.data.remote.dto.DashboardResponse
import ir.devtrader.investor.data.repository.InvestorRepository
import ir.devtrader.investor.ui.common.Banner
import ir.devtrader.investor.ui.common.FullScreenError
import ir.devtrader.investor.ui.common.FullScreenLoading
import ir.devtrader.investor.ui.common.SectionCard
import ir.devtrader.investor.ui.theme.LossRed
import ir.devtrader.investor.ui.theme.WarningAmber
import ir.devtrader.investor.util.AccountStatusLevel
import ir.devtrader.investor.util.computeAccountStatus
import java.util.Locale

@Composable
fun DashboardScreen(
    investorRepository: InvestorRepository,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(investorRepository))
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading && uiState.dashboard == null -> FullScreenLoading()
        uiState.error != null && uiState.dashboard == null ->
            FullScreenError(uiState.error!!, onRetry = viewModel::refresh)
        uiState.dashboard != null -> DashboardContent(
            modifier = modifier,
            dashboard = uiState.dashboard!!,
            accountError = uiState.dashboard!!.accountError,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun DashboardContent(
    modifier: Modifier = Modifier,
    dashboard: DashboardResponse,
    accountError: String?,
    onOpenSettings: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AccountStatusBanner(dashboard = dashboard)
        }

        item {
            Text(
                text = "Welcome back, ${dashboard.investor.first}",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        if (accountError != null) {
            item { Banner(message = accountError, isError = true) }
        }

        if (!dashboard.hasApiKey) {
            item {
                SectionCard {
                    Text("Bind your exchange API key", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Connect your Bitunix API key to see your wallet and trading activity here.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )
                    Button(onClick = onOpenSettings) { Text("Bind API key") }
                }
            }
        } else {
            item {
                SectionCard {
                    Text("Wallet", style = MaterialTheme.typography.titleMedium)
                    val assets = dashboard.assets
                    StatRow("Available", assets?.available.formatMoney())
                    StatRow("Margin in use", assets?.margin.formatMoney())
                    StatRow("Cross unrealized PNL", assets?.crossUnrealizedPNL.formatMoney())
                    StatRow("Isolated unrealized PNL", assets?.isolationUnrealizedPNL.formatMoney())
                }
            }
            item {
                SectionCard {
                    Text("Positions & performance", style = MaterialTheme.typography.titleMedium)
                    StatRow("Open positions", dashboard.openPositionsCount.toString())
                    StatRow("Total trades", dashboard.summary.totalTrades.toString())
                    StatRow("Sample size", dashboard.summary.sampleSize.toString())
                    StatRow(
                        "Win rate",
                        dashboard.summary.winRate?.let { "%.1f%%".format(Locale.US, it * 100) } ?: "—",
                    )
                    StatRow("Total realized PNL", dashboard.summary.totalRealizedPnl.formatMoney())
                }
            }
        }
    }
}

@Composable
private fun AccountStatusBanner(dashboard: DashboardResponse) {
    val status = computeAccountStatus(dashboard.investor, dashboard.hasApiKey)
    val accentColor = when (status.level) {
        AccountStatusLevel.PENDING_APPROVAL -> WarningAmber
        AccountStatusLevel.FROZEN -> LossRed
        AccountStatusLevel.TRADING_PAUSED -> WarningAmber
        AccountStatusLevel.ACTIVE -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Column {
            Text(
                text = status.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
            Text(
                text = status.subtext,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun Double?.formatMoney(): String =
    if (this == null) "—" else "%.2f".format(Locale.US, this)
