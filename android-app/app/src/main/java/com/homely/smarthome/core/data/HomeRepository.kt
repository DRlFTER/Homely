package com.homely.smarthome.core.data

import com.homely.smarthome.core.model.Device
import com.homely.smarthome.core.model.DeviceSchedule
import com.homely.smarthome.core.model.Floor
import com.homely.smarthome.core.model.HomeAlert
import com.homely.smarthome.core.model.UsageRecord
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    suspend fun signInDemo()
    fun observeFloors(): Flow<List<Floor>>
    fun observeDevices(): Flow<List<Device>>
    fun observeSchedules(): Flow<List<DeviceSchedule>>
    fun observeUsage(): Flow<List<UsageRecord>>
    fun observeAlerts(): Flow<List<HomeAlert>>
    suspend fun setDevicePower(device: Device, isOn: Boolean)
    suspend fun setSwitch(device: Device, switchId: String, isOn: Boolean)
    suspend fun setSafetyDuration(device: Device, minutes: Int)
    suspend fun saveSchedule(schedule: DeviceSchedule)
    suspend fun acknowledgeAlert(alertId: String)
    suspend fun registerMessagingToken()
}
