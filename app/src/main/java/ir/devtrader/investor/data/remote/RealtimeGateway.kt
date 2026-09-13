package ir.devtrader.investor.data.remote

import ir.devtrader.investor.data.remote.dto.AlarmTriggeredEvent
import ir.devtrader.investor.data.remote.dto.SocketNotification
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * Wraps the backend's Socket.IO connection (root domain, NOT under /api/investor). Connected
 * once after login/register and kept open while the app is foregrounded; there is no FCM/push
 * yet, so this is the only source of live updates (see NotificationsCenter for the poll fallback).
 */
class RealtimeGateway(private val json: Json) {

    private var socket: Socket? = null

    private val _notifications = MutableSharedFlow<SocketNotification>(extraBufferCapacity = 16)
    val notifications: SharedFlow<SocketNotification> = _notifications

    private val _alarmTriggered = MutableSharedFlow<AlarmTriggeredEvent>(extraBufferCapacity = 16)
    val alarmTriggered: SharedFlow<AlarmTriggeredEvent> = _alarmTriggered

    fun connect(token: String) {
        disconnect()
        val options = IO.Options()
        options.auth = mapOf("token" to token)
        val socket = IO.socket(SOCKET_URL, options)

        socket.on("notification") { args ->
            (args.firstOrNull() as? JSONObject)?.let { payload ->
                runCatching { json.decodeFromString(SocketNotification.serializer(), payload.toString()) }
                    .onSuccess { _notifications.tryEmit(it) }
            }
        }
        socket.on("alarmTriggered") { args ->
            (args.firstOrNull() as? JSONObject)?.let { payload ->
                runCatching { json.decodeFromString(AlarmTriggeredEvent.serializer(), payload.toString()) }
                    .onSuccess { _alarmTriggered.tryEmit(it) }
            }
        }

        socket.connect()
        this.socket = socket
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    companion object {
        private const val SOCKET_URL = "https://devtrader.ir"
    }
}
