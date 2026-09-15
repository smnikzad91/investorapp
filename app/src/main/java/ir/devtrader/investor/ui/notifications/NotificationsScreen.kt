package ir.devtrader.investor.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import ir.devtrader.investor.data.remote.dto.NotificationItem
import ir.devtrader.investor.data.repository.NotificationsCenter
import ir.devtrader.investor.ui.common.FullScreenError
import ir.devtrader.investor.ui.common.FullScreenLoading
import ir.devtrader.investor.ui.theme.LossRed
import ir.devtrader.investor.ui.theme.WarningAmber

@Composable
fun NotificationsScreen(notificationsCenter: NotificationsCenter, modifier: Modifier = Modifier) {
    val viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.factory(notificationsCenter))
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading && uiState.notifications.isEmpty() -> FullScreenLoading()
        uiState.error != null && uiState.notifications.isEmpty() ->
            FullScreenError(uiState.error!!, onRetry = viewModel::refresh)
        uiState.notifications.isEmpty() -> Text(
            stringResource(R.string.notifications_empty),
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(uiState.notifications, key = { it._id }) { notification ->
                NotificationRow(notification)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: NotificationItem) {
    val accentColor = when (notification.type) {
        "error" -> LossRed
        "warning" -> WarningAmber
        "success" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (!notification.seen) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = notification.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (!notification.seen) FontWeight.Bold else FontWeight.Normal,
            color = accentColor,
        )
        Text(
            text = notification.message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = notification.createdAt,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
