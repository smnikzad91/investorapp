package ir.devtrader.investor.data.remote.dto

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class NotificationItem(
    val _id: String,
    val type: String,
    val title: String,
    val message: String,
    val seen: Boolean,
    val seenAt: String? = null,
    val createdAt: String,
)

@Serializable
data class NotificationsResponse(
    val status: Boolean,
    val notifications: List<NotificationItem> = emptyList(),
    val unseenCount: Int = 0,
)

@Serializable
data class SimpleStatusResponse(
    val status: Boolean,
)

/** Socket.IO `notification` event payload — the live-push side of GET /notifications. */
@Serializable
data class SocketNotification(
    val type: String,
    val title: String,
    val message: String,
    val ts: Long,
)

fun SocketNotification.toNotificationItem(): NotificationItem = NotificationItem(
    _id = "live-$ts",
    type = type,
    title = title,
    message = message,
    seen = false,
    seenAt = null,
    createdAt = Instant.ofEpochMilli(ts).toString(),
)
