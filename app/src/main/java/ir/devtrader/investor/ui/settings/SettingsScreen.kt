package ir.devtrader.investor.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.devtrader.investor.data.repository.InvestorRepository
import ir.devtrader.investor.ui.common.Banner
import ir.devtrader.investor.ui.common.FullScreenError
import ir.devtrader.investor.ui.common.FullScreenLoading
import ir.devtrader.investor.ui.common.SectionCard
import ir.devtrader.investor.ui.theme.WarningAmber

@Composable
fun SettingsScreen(investorRepository: InvestorRepository, modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(investorRepository))
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> FullScreenLoading()
        uiState.error != null -> FullScreenError(uiState.error!!, onRetry = viewModel::refresh)
        else -> LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ProfitShareSection(uiState.profitSharePercent) }
            item { TradingControlSection(uiState = uiState, viewModel = viewModel) }
            item { MarginRatioSection(uiState = uiState, viewModel = viewModel) }
            item { BindKeySection(uiState = uiState, viewModel = viewModel) }
        }
    }

    if (uiState.showBindKeyConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissBindKeyConfirmation,
            title = { Text("Confirm binding") },
            text = {
                Text(
                    "This will connect your Bitunix account for live trading through this " +
                        "platform. Only continue if you trust this key was generated for this purpose.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmBindKey) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissBindKeyConfirmation) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ProfitShareSection(profitSharePercent: Double?) {
    SectionCard {
        Text("Profit Share", style = MaterialTheme.typography.titleMedium)
        Text(
            text = profitSharePercent?.let { "%.2f%%".format(java.util.Locale.US, it) }
                ?: "Not configured yet — an admin sets this before your account can be activated.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun TradingControlSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mirror Trading", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (uiState.tradingEnabled) {
                        "Active — every position opened on the main account opens on yours too."
                    } else {
                        "Trading Paused — anything already open still closes normally."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Switch(
                checked = uiState.tradingEnabled,
                onCheckedChange = { viewModel.toggleTrading() },
                enabled = !uiState.isTogglingTrading,
            )
        }
        uiState.tradingToggleError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun MarginRatioSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard {
        Text("Margin Ratio", style = MaterialTheme.typography.titleMedium)
        Text(
            "How much of your available balance a mirrored trade risks: available balance × margin " +
                "ratio, before the main account's own leverage is applied on top.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        OutlinedTextField(
            value = uiState.marginRatioInput,
            onValueChange = viewModel::onMarginRatioChange,
            label = { Text("Margin ratio (%)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        uiState.marginRatioResultMessage?.let {
            Banner(message = it, isError = uiState.isMarginRatioResultError)
        }
        Button(
            onClick = viewModel::saveMarginRatio,
            enabled = !uiState.isSavingMarginRatio,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            if (uiState.isSavingMarginRatio) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Save")
            }
        }
    }
}

@Composable
private fun BindKeySection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard {
        Text("Bind API Key", style = MaterialTheme.typography.titleMedium)
        Text(
            "Paste your Bitunix API key and secret. This links your exchange account so trades " +
                "placed by the platform are mirrored on your own account.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        OutlinedTextField(
            value = uiState.key,
            onValueChange = viewModel::onKeyChange,
            label = { Text("API key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.secret,
            onValueChange = viewModel::onSecretChange,
            label = { Text("API secret") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp),
        )

        uiState.bindKeyResultMessage?.let {
            Banner(message = it, isError = uiState.isBindKeyResultError)
        }
        uiState.bindKeyWarning?.let {
            Text(
                text = it,
                color = WarningAmber,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Button(
            onClick = viewModel::requestBindKeyConfirmation,
            enabled = !uiState.isSubmittingKey,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            if (uiState.isSubmittingKey) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Save API key")
            }
        }
    }
}
