package ir.devtrader.investor.ui.settings

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.devtrader.investor.R
import ir.devtrader.investor.data.repository.InvestorRepository
import ir.devtrader.investor.ui.common.Banner
import ir.devtrader.investor.ui.common.FullScreenError
import ir.devtrader.investor.ui.common.FullScreenLoading
import ir.devtrader.investor.ui.common.SectionCard
import ir.devtrader.investor.ui.theme.WarningAmber
import ir.devtrader.investor.util.LanguageManager

@Composable
fun SettingsScreen(investorRepository: InvestorRepository, modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(application, investorRepository))
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
            item { LanguageSection() }
        }
    }

    if (uiState.showBindKeyConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissBindKeyConfirmation,
            title = { Text(stringResource(R.string.settings_bind_key_confirm_title)) },
            text = { Text(stringResource(R.string.settings_bind_key_confirm_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmBindKey) { Text(stringResource(R.string.settings_bind_key_confirm_button)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissBindKeyConfirmation) { Text(stringResource(R.string.settings_bind_key_cancel_button)) }
            },
        )
    }
}

@Composable
private fun ProfitShareSection(profitSharePercent: Double?) {
    SectionCard {
        Text(stringResource(R.string.settings_profit_share_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = profitSharePercent?.let { "%.2f%%".format(java.util.Locale.US, it) }
                ?: stringResource(R.string.settings_profit_share_not_configured),
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
                Text(stringResource(R.string.settings_trading_control_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (uiState.tradingEnabled) {
                        stringResource(R.string.settings_trading_active)
                    } else {
                        stringResource(R.string.settings_trading_paused)
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
        Text(stringResource(R.string.settings_margin_ratio_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.settings_margin_ratio_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        OutlinedTextField(
            value = uiState.marginRatioInput,
            onValueChange = viewModel::onMarginRatioChange,
            label = { Text(stringResource(R.string.settings_margin_ratio_label)) },
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
                Text(stringResource(R.string.settings_save_button))
            }
        }
    }
}

@Composable
private fun BindKeySection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard {
        Text(stringResource(R.string.settings_bind_key_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.settings_bind_key_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        OutlinedTextField(
            value = uiState.key,
            onValueChange = viewModel::onKeyChange,
            label = { Text(stringResource(R.string.settings_api_key_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.secret,
            onValueChange = viewModel::onSecretChange,
            label = { Text(stringResource(R.string.settings_api_secret_label)) },
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
                Text(stringResource(R.string.settings_save_api_key_button))
            }
        }
    }
}

/**
 * MainActivity is AppCompatActivity (required for per-app language on API < 33, see
 * MainActivity's own doc comment), so appcompat 1.6.0+'s automatic recreate-on-locale-change
 * should already fire on its own — this still calls recreate() explicitly too as a cheap,
 * harmless fallback in case that hook doesn't fire on some OS version/state.
 */
@Composable
private fun LanguageSection() {
    val activity = LocalContext.current as ComponentActivity
    var showPicker by remember { mutableStateOf(false) }
    val currentLanguage = LanguageManager.currentLanguage()
    val currentLanguageLabel = if (currentLanguage == LanguageManager.PERSIAN) {
        stringResource(R.string.settings_language_persian)
    } else {
        stringResource(R.string.settings_language_english)
    }

    SectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPicker = true },
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_language_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    currentLanguageLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.settings_language_picker_title)) },
            text = {
                Column {
                    LanguageOptionRow(
                        label = stringResource(R.string.settings_language_english),
                        selected = currentLanguage == LanguageManager.ENGLISH,
                        onClick = {
                            showPicker = false
                            LanguageManager.setLanguage(LanguageManager.ENGLISH)
                            activity.recreate()
                        },
                    )
                    LanguageOptionRow(
                        label = stringResource(R.string.settings_language_persian),
                        selected = currentLanguage == LanguageManager.PERSIAN,
                        onClick = {
                            showPicker = false
                            LanguageManager.setLanguage(LanguageManager.PERSIAN)
                            activity.recreate()
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.settings_bind_key_cancel_button)) }
            },
        )
    }
}

@Composable
private fun LanguageOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}
