package com.homely.smarthome.core.data

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import com.homely.smarthome.core.model.AlertType
import com.homely.smarthome.core.model.Device
import com.homely.smarthome.core.model.DeviceSchedule
import com.homely.smarthome.core.model.DeviceStatus
import com.homely.smarthome.core.model.DeviceSwitch
import com.homely.smarthome.core.model.DeviceType
import com.homely.smarthome.core.model.Floor
import com.homely.smarthome.core.model.HomeAlert
import com.homely.smarthome.core.model.UsageEndReason
import com.homely.smarthome.core.model.UsageRecord
import com.homely.smarthome.core.model.switches
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

private const val DemoHomeId = "demo-home"
private const val DemoEmail = "demo@homely.local"
private const val DemoPassword = "homely-demo-123"

class FirebaseHomeRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val messaging: FirebaseMessaging = Firebase.messaging,
) : HomeRepository {
    private val homeDocument get() = firestore.collection("homes").document(DemoHomeId)

    override suspend fun signInDemo() {
        if (auth.currentUser != null) return
        auth.signInWithEmailAndPassword(DemoEmail, DemoPassword).awaitResult()
    }

    override fun observeFloors(): Flow<List<Floor>> = snapshots(
        query = homeDocument.collection("floors").orderBy("sortOrder", Query.Direction.ASCENDING),
        mapper = ::floor,
    )

    override fun observeDevices(): Flow<List<Device>> = snapshots(
        query = homeDocument.collection("devices"),
        mapper = ::device,
    )

    override fun observeSchedules(): Flow<List<DeviceSchedule>> = snapshots(
        query = homeDocument.collection("schedules"),
        mapper = ::schedule,
    )

    override fun observeUsage(): Flow<List<UsageRecord>> = snapshots(
        query = homeDocument.collection("usageRecords").orderBy("startedAt", Query.Direction.DESCENDING),
        mapper = ::usageRecord,
    )

    override fun observeAlerts(): Flow<List<HomeAlert>> = snapshots(
        query = homeDocument.collection("alerts").orderBy("createdAt", Query.Direction.DESCENDING),
        mapper = ::homeAlert,
    )

    override suspend fun setDevicePower(device: Device, isOn: Boolean) {
        updateDevice(
            device = device,
            fields = mapOf(
                "status" to if (isOn) DeviceStatus.ON.name else DeviceStatus.OFF.name,
                "capabilities" to (device.capabilities + ("isOn" to isOn)),
            ),
        )
    }

    override suspend fun setSwitch(device: Device, switchId: String, isOn: Boolean) {
        val nextSwitches = device.switches.map { item ->
            if (item.id == switchId) item.copy(isOn = isOn) else item
        }
        updateDevice(
            device = device,
            fields = mapOf(
                "status" to if (nextSwitches.any(DeviceSwitch::isOn)) DeviceStatus.ON.name else DeviceStatus.OFF.name,
                "capabilities" to (device.capabilities + (
                    "switches" to nextSwitches.map { item ->
                        mapOf("id" to item.id, "label" to item.label, "isOn" to item.isOn)
                    }
                )),
            ),
        )
    }

    override suspend fun setSafetyDuration(device: Device, minutes: Int) {
        require(minutes in 1..120) { "Safety duration must be between 1 and 120 minutes." }
        updateDevice(
            device = device,
            fields = mapOf(
                "capabilities" to (device.capabilities + ("maxOnDurationMinutes" to minutes)),
            ),
        )
    }

    override suspend fun saveSchedule(schedule: DeviceSchedule) {
        require(TIME_PATTERN.matches(schedule.startTime)) { "Start time must use HH:mm." }
        require(TIME_PATTERN.matches(schedule.endTime)) { "End time must use HH:mm." }
        require(schedule.startTime != schedule.endTime) { "Start and end times must be different." }
        require(schedule.daysOfWeek.isNotEmpty()) { "Choose at least one day." }
        val id = schedule.id.ifBlank { schedule.deviceId }
        homeDocument.collection("schedules").document(id).set(
            mapOf(
                "deviceId" to schedule.deviceId,
                "enabled" to schedule.enabled,
                "startTime" to schedule.startTime,
                "endTime" to schedule.endTime,
                "daysOfWeek" to schedule.daysOfWeek.sorted(),
                "maxOnDurationMinutes" to schedule.maxOnDurationMinutes,
            ),
        ).awaitResult()
    }

    override suspend fun acknowledgeAlert(alertId: String) {
        homeDocument.collection("alerts").document(alertId)
            .update("acknowledged", true)
            .awaitResult()
    }

    override suspend fun registerMessagingToken() {
        val user = auth.currentUser ?: return
        val token = messaging.token.awaitResult()
        val tokenId = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .take(12)
            .joinToString("") { byte -> "%02x".format(byte) }
        homeDocument.collection("fcmTokens").document(tokenId).set(
            mapOf(
                "token" to token,
                "userId" to user.uid,
                "platform" to "android",
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).awaitResult()
    }

    private suspend fun updateDevice(device: Device, fields: Map<String, Any?>) {
        val uid = requireNotNull(auth.currentUser?.uid) { "Sign in before controlling devices." }
        homeDocument.collection("devices").document(device.id).update(
            fields + mapOf(
                "updatedAt" to FieldValue.serverTimestamp(),
                "updatedBy" to uid,
            ),
        ).awaitResult()
    }

    private fun <T> snapshots(
        query: Query,
        mapper: (DocumentSnapshot) -> T?,
    ): Flow<List<T>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().mapNotNull(mapper))
        }
        awaitClose { registration.remove() }
    }

    private fun floor(document: DocumentSnapshot): Floor = Floor(
        id = document.id,
        name = document.getString("name").orEmpty(),
        imageUrl = document.getString("imageUrl").orEmpty(),
        gridRows = document.getLong("gridRows")?.toInt() ?: 8,
        gridColumns = document.getLong("gridColumns")?.toInt() ?: 12,
        sortOrder = document.getLong("sortOrder")?.toInt() ?: 0,
    )

    private fun device(document: DocumentSnapshot): Device? {
        val type = document.getString("type")?.enumValueOrNull<DeviceType>() ?: return null
        val status = document.getString("status")?.enumValueOrNull<DeviceStatus>() ?: DeviceStatus.OFF
        @Suppress("UNCHECKED_CAST")
        val capabilities = document.get("capabilities") as? Map<String, Any?> ?: emptyMap()
        return Device(
            id = document.id,
            floorId = document.getString("floorId").orEmpty(),
            name = document.getString("name").orEmpty(),
            type = type,
            status = status,
            gridX = document.getLong("gridX")?.toInt() ?: 0,
            gridY = document.getLong("gridY")?.toInt() ?: 0,
            capabilities = capabilities,
            updatedAt = document.getTimestamp("updatedAt"),
            updatedBy = document.getString("updatedBy").orEmpty(),
        )
    }

    private fun schedule(document: DocumentSnapshot): DeviceSchedule = DeviceSchedule(
        id = document.id,
        deviceId = document.getString("deviceId").orEmpty(),
        enabled = document.getBoolean("enabled") ?: false,
        startTime = document.getString("startTime") ?: "18:00",
        endTime = document.getString("endTime") ?: "22:00",
        daysOfWeek = (document.get("daysOfWeek") as? List<*>)
            .orEmpty().mapNotNull { (it as? Number)?.toInt() },
        maxOnDurationMinutes = document.getLong("maxOnDurationMinutes")?.toInt(),
    )

    private fun usageRecord(document: DocumentSnapshot): UsageRecord = UsageRecord(
        id = document.id,
        deviceId = document.getString("deviceId").orEmpty(),
        startedAt = document.getTimestamp("startedAt"),
        endedAt = document.getTimestamp("endedAt"),
        durationSeconds = document.getLong("durationSeconds")?.toInt(),
        endedReason = document.getString("endedReason")?.enumValueOrNull<UsageEndReason>(),
    )

    private fun homeAlert(document: DocumentSnapshot): HomeAlert = HomeAlert(
        id = document.id,
        deviceId = document.getString("deviceId").orEmpty(),
        type = document.getString("type")?.enumValueOrNull<AlertType>() ?: AlertType.DEVICE_ERROR,
        message = document.getString("message").orEmpty(),
        createdAt = document.getTimestamp("createdAt"),
        acknowledged = document.getBoolean("acknowledged") ?: false,
    )

    private companion object {
        val TIME_PATTERN = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
    addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
    addOnCanceledListener { continuation.cancel() }
}

private inline fun <reified T : Enum<T>> String.enumValueOrNull(): T? =
    enumValues<T>().firstOrNull { it.name == this }
