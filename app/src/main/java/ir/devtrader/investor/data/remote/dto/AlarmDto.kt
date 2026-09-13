package ir.devtrader.investor.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class Alarm(
    val _id: String,
    val symbol: String,
    val condition: String,
    val price: Double,
    val isEnabled: Boolean = true,
    val shouldMessage: Boolean = false,
    val shouldCall: Boolean = false,
    val createdAt: String,
)

@Serializable
data class AlarmsResponse(
    val status: Boolean,
    val alarms: List<Alarm> = emptyList(),
)

@Serializable
data class CreateAlarmRequest(
    val symbol: String,
    val condition: String,
    val price: Double,
    val sms: Boolean = false,
    val call: Boolean = false,
)

@Serializable
data class CreateAlarmResponse(
    val status: Boolean,
    val alarm: Alarm? = null,
    val message: String? = null,
)

@Serializable
data class SymbolsResponse(
    val status: Boolean,
    val symbols: List<String> = emptyList(),
)

/** Socket.IO `alarmTriggered` event payload — the alarm is already deleted server-side by the time this arrives. */
@Serializable
data class AlarmTriggeredEvent(
    val _id: String,
    val symbol: String,
    val condition: String,
    val price: Double,
)
