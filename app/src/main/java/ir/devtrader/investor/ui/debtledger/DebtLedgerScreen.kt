package ir.devtrader.investor.ui.debtledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.devtrader.investor.R
import ir.devtrader.investor.data.remote.dto.DebtLedgerEntry
import ir.devtrader.investor.data.repository.InvestorRepository
import ir.devtrader.investor.ui.common.FullScreenError
import ir.devtrader.investor.ui.common.FullScreenLoading
import ir.devtrader.investor.ui.theme.LossRed
import java.util.Locale

@Composable
fun DebtLedgerScreen(investorRepository: InvestorRepository, modifier: Modifier = Modifier) {
    val viewModel: DebtLedgerViewModel = viewModel(factory = DebtLedgerViewModel.factory(investorRepository))
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading && uiState.ledger == null -> FullScreenLoading()
        uiState.error != null && uiState.ledger == null ->
            FullScreenError(uiState.error!!, onRetry = viewModel::refresh)
        uiState.ledger != null -> {
            val ledger = uiState.ledger!!
            LazyColumn(
                modifier = modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
            ) {
                item {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(stringResource(R.string.debt_ledger_current_balance), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "%.2f".format(Locale.US, ledger.debt),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (ledger.debt > 0) LossRed else MaterialTheme.colorScheme.primary,
                        )
                    }
                    HorizontalDivider()
                }
                items(ledger.entries, key = { it._id }) { entry ->
                    DebtLedgerRow(entry)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun DebtLedgerRow(entry: DebtLedgerEntry) {
    val deltaColor = when {
        entry.debtDelta > 0 -> LossRed
        entry.debtDelta < 0 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.symbol ?: entry.type.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
            )
            entry.side?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            entry.note?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Text(entry.createdAt, style = MaterialTheme.typography.bodyMedium)
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text(
                text = "%+.2f".format(Locale.US, entry.debtDelta),
                color = deltaColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.debt_ledger_balance_after, "%.2f".format(Locale.US, entry.balanceAfter)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
