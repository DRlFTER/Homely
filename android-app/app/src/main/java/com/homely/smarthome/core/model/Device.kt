package com.homely.smarthome.core.model

import com.google.firebase.Timestamp

enum class DeviceType {
    OUTLET,
    MULTI_SWITCH,
    LIGHT,
    SAFETY_OUTLET,
    CAMERA,
}

enum class DeviceStatus {
    ON,
    OFF,
    ERROR,
    DISCONNECTED,
}

data class Device(
    val id: String = "",
    val floorId: String = "",
    val name: String = "",
    val type: DeviceType = DeviceType.OUTLET,
    val status: DeviceStatus = DeviceStatus.OFF,
    val gridX: Int = 0,
    val gridY: Int = 0,
    val capabilities: Map<String, Any?> = emptyMap(),
    val updatedAt: Timestamp? = null,
    val updatedBy: String = "",
)

data class DeviceSwitch(
    val id: String,
    val label: String,
    val isOn: Boolean,
)

val Device.isOn: Boolean
    get() = status == DeviceStatus.ON || capabilities["isOn"] == true

val Device.switches: List<DeviceSwitch>
    get() = (capabilities["switches"] as? List<*>)
        .orEmpty()
        .mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val id = map["id"] as? String ?: return@mapNotNull null
            val label = map["label"] as? String ?: return@mapNotNull null
            DeviceSwitch(id = id, label = label, isOn = map["isOn"] == true)
        }

fun Device.numberCapability(key: String, fallback: Int = 0): Int =
    (capabilities[key] as? Number)?.toInt() ?: fallback

fun Device.timestampCapability(key: String): Timestamp? = capabilities[key] as? Timestamp
