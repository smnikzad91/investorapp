package ir.devtrader.investor.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.devtrader.investor.data.remote.dto.NotificationItem
import ir.devtrader.investor.data.repository.NotificationsCenter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<NotificationItem> = emptyList(),
    val error: String? = null,
)

/**
 * Notifications are owned by the app-scoped [NotificationsCenter] (so the drawer badge and the
 * socket-pushed live feed stay correct even when this screen isn't open) — this ViewModel just
 * mirrors that shared state and triggers a refresh + mark-all-seen when the screen is opened.
 */
class NotificationsViewModel(private val notificationsCenter: NotificationsCenter) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = combine(
        notificationsCenter.notifications,
        notificationsCenter.isLoading,
        notificationsCenter.error,
    ) { notifications, isLoading, error ->
        NotificationsUiState(isLoading = isLoading, notifications = notifications, error = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            notificationsCenter.refresh()
            notificationsCenter.markAllSeen()
        }
    }

    companion object {
        fun factory(notificationsCenter: NotificationsCenter): ViewModelProvider.Factory = viewModelFactory {
            initializer { NotificationsViewModel(notificationsCenter) }
        }
    }
}
