package ir.devtrader.investor.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.devtrader.investor.R
import ir.devtrader.investor.ui.theme.LossRed
import ir.devtrader.investor.util.firstNumberField
import ir.devtrader.investor.util.rawString
import ir.devtrader.investor.util.stringField
import kotlinx.serialization.json.JsonObject
import java.util.Locale

@Composable
fun FullScreenLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun FullScreenError(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.common_retry))
            }
        }
    }
}

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content,
    )
}

/**
 * Positions/trades come back as raw, shape-not-guaranteed JSON (see backend API notes) — this
 * surfaces the common symbol/side/pnl-style fields prominently and lists whatever else is present
 * generically, treating unknown fields as opaque per spec.
 */
@Composable
fun GenericRecordCard(record: JsonObject) {
    val symbol = record.stringField("symbol")
    val side = record.stringField("side")
    val pnl = record.firstNumberField("realizedPNL", "unrealizedPNL", "pnl", "profit")
    val knownKeys = setOf("symbol", "side", "realizedPNL", "unrealizedPNL", "pnl", "profit")
    val otherKeys = record.keys.filterNot { it in knownKeys }

    SectionCard {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(symbol ?: stringResource(R.string.common_unknown_symbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                side?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            pnl?.let {
                Text(
                    "%+.2f".format(Locale.US, it),
                    color = if (it >= 0) MaterialTheme.colorScheme.primary else LossRed,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        if (otherKeys.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                otherKeys.forEach { key ->
                    Text("$key: ${record.rawString(key)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun Banner(message: String, isError: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
