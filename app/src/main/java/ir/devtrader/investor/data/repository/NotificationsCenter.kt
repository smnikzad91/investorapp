package ir.devtrader.investor.data.repository

import ir.devtrader.investor.data.remote.RealtimeGateway
import ir.devtrader.investor.data.remote.dto.NotificationItem
import ir.devtrader.investor.data.remote.dto.toNotificationItem
import ir.devtrader.investor.util.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * App-scoped (not screen-scoped): the drawer header's unread badge needs to stay correct even
 * when the Notifications screen itself isn't open, and the socket `notification` event has to be
 * handled at this level too or it'd be silently missed whenever the user isn't on that screen.
 */
class NotificationsCenter(
    private val investorRepository: InvestorRepository,
    private val realtimeGateway: RealtimeGateway,
    appScope: CoroutineScope,
) {
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _unseenCount = MutableStateFlow(0)
    val unseenCount: StateFlow<Int> = _unseenCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        appScope.launch {
            realtimeGateway.notifications.collect { event ->
                _notifications.value = listOf(event.toNotificationItem()) + _notifications.value
                _unseenCount.value += 1
            }
        }
    }

    suspend fun refresh() {
        _isLoading.value = true
        when (val result = investorRepository.getNotifications()) {
            is ApiResult.Success -> {
                _notifications.value = result.data.notifications
                _unseenCount.value = result.data.unseenCount
                _error.value = null
            }
            is ApiResult.Error -> _error.value = result.message
        }
        _isLoading.value = false
    }

    suspend fun markAllSeen() {
        if (_unseenCount.value == 0) return
        investorRepository.markNotificationsSeen()
        _unseenCount.value = 0
        _notifications.value = _notifications.value.map { it.copy(seen = true) }
    }
}
