package com.homely.smarthome.core.model

import com.google.firebase.Timestamp

data class DeviceSchedule(
    val id: String = "",
    val deviceId: String = "",
    val enabled: Boolean = false,
    val startTime: String = "18:00",
    val endTime: String = "22:00",
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5),
    val maxOnDurationMinutes: Int? = null,
)

enum class UsageEndReason {
    USER,
    SCHEDULE,
    SAFETY_CUTOFF,
}

data class UsageRecord(
    val id: String = "",
    val deviceId: String = "",
    val startedAt: Timestamp? = null,
    val endedAt: Timestamp? = null,
    val durationSeconds: Int? = null,
    val endedReason: UsageEndReason? = null,
)

enum class AlertType {
    SAFETY_CUTOFF,
    DEVICE_ERROR,
}

data class HomeAlert(
    val id: String = "",
    val deviceId: String = "",
    val type: AlertType = AlertType.DEVICE_ERROR,
    val message: String = "",
    val createdAt: Timestamp? = null,
    val acknowledged: Boolean = false,
)
