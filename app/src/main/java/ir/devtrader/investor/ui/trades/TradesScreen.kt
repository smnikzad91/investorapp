package ir.devtrader.investor.ui.trades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.devtrader.investor.data.repository.InvestorRepository
import ir.devtrader.investor.ui.common.Banner
import ir.devtrader.investor.ui.common.FullScreenError
import ir.devtrader.investor.ui.common.FullScreenLoading
import ir.devtrader.investor.ui.common.GenericRecordCard

@Composable
fun TradesScreen(investorRepository: InvestorRepository, modifier: Modifier = Modifier) {
    val viewModel: TradesViewModel = viewModel(factory = TradesViewModel.factory(investorRepository))
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> FullScreenLoading()
        uiState.error != null -> FullScreenError(uiState.error!!, onRetry = viewModel::refresh)
        !uiState.hasApiKey -> Text(
            "Bind your API key first to see your trade history.",
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
        )
        uiState.trades.isEmpty() -> Text(
            "No closed trades yet",
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.accountError?.let { error ->
                item { Banner(message = error, isError = true) }
            }
            items(uiState.trades.size) { index ->
                GenericRecordCard(uiState.trades[index])
            }
        }
    }
}
