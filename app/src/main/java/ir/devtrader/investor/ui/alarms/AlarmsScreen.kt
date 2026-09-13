package ir.devtrader.investor.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.devtrader.investor.data.remote.RealtimeGateway
import ir.devtrader.investor.data.remote.dto.Alarm
import ir.devtrader.investor.data.repository.InvestorRepository
import ir.devtrader.investor.data.repository.SymbolsCache
import ir.devtrader.investor.ui.common.FullScreenError
import ir.devtrader.investor.ui.common.FullScreenLoading
import ir.devtrader.investor.ui.common.SectionCard
import ir.devtrader.investor.ui.theme.LossRed

private val CONDITIONS = listOf("above", "below")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    investorRepository: InvestorRepository,
    realtimeGateway: RealtimeGateway,
    symbolsCache: SymbolsCache,
    modifier: Modifier = Modifier,
) {
    val viewModel: AlarmsViewModel = viewModel(
        factory = AlarmsViewModel.factory(investorRepository, realtimeGateway, symbolsCache),
    )
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading && uiState.alarms.isEmpty() && uiState.error != null ->
            FullScreenError(uiState.error!!, onRetry = viewModel::refresh)
        uiState.isLoading && uiState.alarms.isEmpty() -> FullScreenLoading()
        else -> LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AddAlarmForm(uiState = uiState, viewModel = viewModel)
            }
            if (uiState.alarms.isEmpty()) {
                item {
                    Text(
                        "No price alarms yet",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                items(uiState.alarms, key = { it._id }) { alarm ->
                    AlarmRow(alarm = alarm, onDelete = { viewModel.deleteAlarm(alarm._id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAlarmForm(uiState: AlarmsUiState, viewModel: AlarmsViewModel) {
    var symbolExpanded by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }

    SectionCard {
        Text("New alarm", style = MaterialTheme.typography.titleMedium)

        // Filtered/searchable: typing narrows filteredSymbols, but the field stays free-text so
        // an unlisted symbol can still be submitted, matching the API's own leniency.
        ExposedDropdownMenuBox(
            expanded = symbolExpanded && uiState.filteredSymbols.isNotEmpty(),
            onExpandedChange = { symbolExpanded = it },
            modifier = Modifier.padding(top = 12.dp),
        ) {
            OutlinedTextField(
                value = uiState.symbol,
                onValueChange = {
                    viewModel.onSymbolChange(it)
                    symbolExpanded = true
                },
                label = { Text("Symbol (e.g. BTC)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = symbolExpanded && uiState.filteredSymbols.isNotEmpty(),
                onDismissRequest = { symbolExpanded = false },
            ) {
                uiState.filteredSymbols.forEach { symbol ->
                    DropdownMenuItem(
                        text = { Text(symbol) },
                        onClick = {
                            viewModel.onSymbolChange(symbol)
                            symbolExpanded = false
                        },
                    )
                }
            }
        }
        ExposedDropdownMenuBox(
            expanded = conditionExpanded,
            onExpandedChange = { conditionExpanded = it },
            modifier = Modifier.padding(top = 12.dp),
        ) {
            OutlinedTextField(
                value = uiState.condition,
                onValueChange = {},
                readOnly = true,
                label = { Text("Condition") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = conditionExpanded,
                onDismissRequest = { conditionExpanded = false },
            ) {
                CONDITIONS.forEach { condition ->
                    DropdownMenuItem(
                        text = { Text(condition) },
                        onClick = {
                            viewModel.onConditionChange(condition)
                            conditionExpanded = false
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = uiState.price,
            onValueChange = viewModel::onPriceChange,
            label = { Text("Price") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text("Text me (SMS)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = uiState.sms, onCheckedChange = viewModel::onSmsChange)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Call me", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = uiState.call, onCheckedChange = viewModel::onCallChange)
        }
        if (uiState.sms || uiState.call) {
            Text(
                "Uses the phone number on your profile",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        uiState.formError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Button(
            onClick = viewModel::addAlarm,
            enabled = !uiState.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Add alarm")
            }
        }
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(alarm.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${alarm.condition} ${alarm.price}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (alarm.condition == "above") MaterialTheme.colorScheme.primary else LossRed,
            )
            if (alarm.shouldMessage || alarm.shouldCall) {
                val alerts = listOfNotNull(
                    "SMS".takeIf { alarm.shouldMessage },
                    "Call".takeIf { alarm.shouldCall },
                ).joinToString(" + ")
                Text(alerts, style = MaterialTheme.typography.bodyMedium)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete alarm")
        }
    }
}
