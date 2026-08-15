package com.homely.smarthome.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.homely.smarthome.BuildConfig
import com.homely.smarthome.core.data.FirebaseHomeRepository
import com.homely.smarthome.core.data.HomeRepository
import com.homely.smarthome.core.model.Device
import com.homely.smarthome.core.model.DeviceSchedule
import com.homely.smarthome.core.model.Floor
import com.homely.smarthome.core.model.FloorValidation
import com.homely.smarthome.core.model.HomeAlert
import com.homely.smarthome.core.model.UsageRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val signedIn: Boolean = false,
    val floors: List<Floor> = emptyList(),
    val devices: List<Device> = emptyList(),
    val schedules: List<DeviceSchedule> = emptyList(),
    val usage: List<UsageRecord> = emptyList(),
    val alerts: List<HomeAlert> = emptyList(),
    val selectedFloorId: String = "",
    val selectedDeviceId: String? = null,
    val pendingIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val confirmationMessage: String? = null,
) {
    val selectedFloor: Floor?
        get() = floors.firstOrNull { it.id == selectedFloorId } ?: floors.firstOrNull()

    val selectedDevice: Device?
        get() = devices.firstOrNull { it.id == selectedDeviceId }

    val floorDevices: List<Device>
        get() = devices.filter { it.floorId == selectedFloor?.id }

    val unacknowledgedAlertCount: Int
        get() = alerts.count { !it.acknowledged }
}

class HomeViewModel(
    private val repository: HomeRepository,
) : ViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(FirebaseHomeRepository()) as T
        }
    }

    private val mutableState = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()
    private var floorsLoaded = false
    private var devicesLoaded = false

    init {
        viewModelScope.launch {
            try {
                repository.signInDemo()
                mutableState.update { it.copy(signedIn = true) }
                observeCollections()
                if (!BuildConfig.USE_FIREBASE_EMULATORS) {
                    runCatching { repository.registerMessagingToken() }
                }
            } catch (error: Exception) {
                showError(error)
                mutableState.update { it.copy(loading = false) }
            }
        }
    }

    fun selectFloor(floorId: String) {
        mutableState.update { it.copy(selectedFloorId = floorId, selectedDeviceId = null) }
    }

    fun saveFloor(floor: Floor) = runCommand(FloorValidation.operationId(floor)) {
        repository.saveFloor(floor.copy(name = floor.name.trim(), imageUrl = floor.imageUrl.trim()))
        mutableState.update { it.copy(confirmationMessage = "Floor saved") }
    }

    fun deleteFloor(floorId: String) = runCommand(floorId) {
        repository.deleteFloor(floorId)
        mutableState.update { current ->
            if (current.selectedFloorId == floorId) {
                current.copy(selectedFloorId = "", selectedDeviceId = null, confirmationMessage = "Floor deleted")
            } else {
                current.copy(confirmationMessage = "Floor deleted")
            }
        }
    }

    fun selectDevice(deviceId: String?) {
        mutableState.update { it.copy(selectedDeviceId = deviceId) }
    }

    fun setDevicePower(device: Device, isOn: Boolean) = runCommand(device.id) {
        repository.setDevicePower(device, isOn)
    }

    fun setSwitch(device: Device, switchId: String, isOn: Boolean) = runCommand(device.id) {
        repository.setSwitch(device, switchId, isOn)
    }

    fun setSafetyDuration(device: Device, minutes: Int) = runCommand(device.id) {
        repository.setSafetyDuration(device, minutes)
    }

    fun saveSchedule(schedule: DeviceSchedule) = runCommand(schedule.id.ifBlank { schedule.deviceId }) {
        repository.saveSchedule(schedule)
        mutableState.update { it.copy(confirmationMessage = "Schedule saved") }
    }

    fun acknowledgeAlert(alertId: String) = runCommand(alertId) {
        repository.acknowledgeAlert(alertId)
    }

    fun clearMessage() {
        mutableState.update { it.copy(errorMessage = null, confirmationMessage = null) }
    }

    private fun observeCollections() {
        observe(repository.observeFloors()) { floors ->
            floorsLoaded = true
            mutableState.update { current ->
                val selectedFloorId = when {
                    current.selectedFloorId.isBlank() -> floors.firstOrNull()?.id.orEmpty()
                    floors.any { it.id == current.selectedFloorId } -> current.selectedFloorId
                    else -> floors.firstOrNull()?.id.orEmpty()
                }
                current.copy(
                    floors = floors,
                    selectedFloorId = selectedFloorId,
                    selectedDeviceId = if (selectedFloorId == current.selectedFloorId) {
                        current.selectedDeviceId
                    } else {
                        null
                    },
                    loading = !(floorsLoaded && devicesLoaded),
                )
            }
        }
        observe(repository.observeDevices()) { devices ->
            devicesLoaded = true
            mutableState.update { it.copy(devices = devices, loading = !(floorsLoaded && devicesLoaded)) }
        }
        observe(repository.observeSchedules()) { schedules ->
            mutableState.update { it.copy(schedules = schedules) }
        }
        observe(repository.observeUsage()) { usage ->
            mutableState.update { it.copy(usage = usage) }
        }
        observe(repository.observeAlerts()) { alerts ->
            mutableState.update { it.copy(alerts = alerts) }
        }
    }

    private fun <T> observe(flow: Flow<T>, updateState: (T) -> Unit) {
        viewModelScope.launch {
            flow.catch { error -> showError(error) }.collect(updateState)
        }
    }

    private fun runCommand(id: String, command: suspend () -> Unit) {
        viewModelScope.launch {
            mutableState.update { it.copy(pendingIds = it.pendingIds + id, errorMessage = null) }
            try {
                command()
            } catch (error: Exception) {
                showError(error)
            } finally {
                mutableState.update { it.copy(pendingIds = it.pendingIds - id) }
            }
        }
    }

    private fun showError(error: Throwable) {
        mutableState.update {
            it.copy(errorMessage = error.message ?: "Firebase could not complete the operation.")
        }
    }
}
