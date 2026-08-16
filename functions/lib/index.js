"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.applySchedules = exports.enforceSafetyCutoffs = exports.onDeviceStateChanged = exports.health = void 0;
const app_1 = require("firebase-admin/app");
const firestore_1 = require("firebase-admin/firestore");
const messaging_1 = require("firebase-admin/messaging");
const v2_1 = require("firebase-functions/v2");
const firestore_2 = require("firebase-functions/v2/firestore");
const https_1 = require("firebase-functions/v2/https");
const scheduler_1 = require("firebase-functions/v2/scheduler");
const domain_js_1 = require("./domain.js");
(0, app_1.getApps)()[0] ?? (0, app_1.initializeApp)();
const db = (0, firestore_1.getFirestore)();
(0, v2_1.setGlobalOptions)({
    region: "asia-south1",
    maxInstances: 10,
});
/** Lightweight endpoint used to verify the Functions emulator/deployment. */
exports.health = (0, https_1.onRequest)((_request, response) => {
    response.status(200).json({ service: "homely-functions", status: "ok" });
});
function timestamp(value) {
    return value instanceof firestore_1.Timestamp ? value : null;
}
function numberValue(value) {
    return typeof value === "number" && Number.isFinite(value) ? value : null;
}
function endReason(updatedBy) {
    if (typeof updatedBy === "string" && updatedBy.startsWith("schedule:")) {
        return "SCHEDULE";
    }
    if (updatedBy === "safety-worker") {
        return "SAFETY_CUTOFF";
    }
    return "USER";
}
exports.onDeviceStateChanged = (0, firestore_2.onDocumentUpdated)("homes/{homeId}/devices/{deviceId}", async (event) => {
    const change = event.data;
    if (!change)
        return;
    const before = change.before.data();
    const after = change.after.data();
    const beforeStatus = before.status;
    const afterStatus = after.status;
    const homeId = event.params.homeId;
    const deviceId = event.params.deviceId;
    const deviceRef = change.after.ref;
    const homeRef = deviceRef.parent.parent;
    if (!homeRef)
        return;
    if (beforeStatus !== "ERROR" && afterStatus === "ERROR") {
        const alertId = (0, domain_js_1.safeDocumentId)(`error-${deviceId}-${event.id}`);
        await homeRef.collection("alerts").doc(alertId).set({
            deviceId,
            type: "DEVICE_ERROR",
            message: `${after.name} reported an error.`,
            createdAt: firestore_1.FieldValue.serverTimestamp(),
            acknowledged: false,
            sourceEventId: event.id,
        }, { merge: true });
    }
    if (beforeStatus === afterStatus || after.type === "CAMERA")
        return;
    if (afterStatus === "ON") {
        await startUsageSession(deviceRef, homeRef, deviceId, after, event.id);
        return;
    }
    if (beforeStatus === "ON") {
        await closeUsageSession(deviceRef, homeRef, deviceId, before, after, event.id);
    }
});
async function startUsageSession(deviceRef, homeRef, deviceId, transitionDevice, eventId) {
    await db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(deviceRef);
        const current = snapshot.data();
        if (!current || current.status !== "ON")
            return;
        const currentCapabilities = { ...current.capabilities };
        if (typeof currentCapabilities.activeUsageRecordId === "string")
            return;
        const now = firestore_1.Timestamp.now();
        const recordId = (0, domain_js_1.safeDocumentId)(`usage-${deviceId}-${eventId}`);
        const startedAt = current.type === "SAFETY_OUTLET"
            ? timestamp(currentCapabilities.onSince) ?? now
            : timestamp(transitionDevice.updatedAt) ?? now;
        currentCapabilities.activeUsageRecordId = recordId;
        currentCapabilities.isOn = true;
        if (current.type === "SAFETY_OUTLET") {
            currentCapabilities.onSince = startedAt;
        }
        transaction.update(deviceRef, {
            capabilities: currentCapabilities,
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        transaction.set(homeRef.collection("usageRecords").doc(recordId), {
            deviceId,
            startedAt,
            endedAt: null,
            durationSeconds: null,
            endedReason: null,
            sourceEventId: eventId,
        }, { merge: true });
    });
}
async function closeUsageSession(deviceRef, homeRef, deviceId, before, after, eventId) {
    await db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(deviceRef);
        const current = snapshot.data();
        if (!current || current.status === "ON")
            return;
        const beforeCapabilities = before.capabilities ?? {};
        const currentCapabilities = { ...current.capabilities };
        const activeRecordId = typeof beforeCapabilities.activeUsageRecordId === "string"
            ? beforeCapabilities.activeUsageRecordId
            : typeof currentCapabilities.activeUsageRecordId === "string"
                ? currentCapabilities.activeUsageRecordId
                : (0, domain_js_1.safeDocumentId)(`usage-${deviceId}-${eventId}`);
        const endedAt = firestore_1.Timestamp.now();
        const startedAt = timestamp(beforeCapabilities.onSince)
            ?? timestamp(before.updatedAt)
            ?? endedAt;
        currentCapabilities.isOn = false;
        currentCapabilities.onSince = null;
        currentCapabilities.activeUsageRecordId = null;
        transaction.update(deviceRef, {
            capabilities: currentCapabilities,
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        transaction.set(homeRef.collection("usageRecords").doc(activeRecordId), {
            deviceId,
            startedAt,
            endedAt,
            durationSeconds: (0, domain_js_1.usageDurationSeconds)(startedAt.toMillis(), endedAt.toMillis()),
            endedReason: endReason(after.updatedBy),
            closedByEventId: eventId,
        }, { merge: true });
    });
}
exports.enforceSafetyCutoffs = (0, scheduler_1.onSchedule)("every 1 minutes", async () => {
    await runSafetyCutoffScan("scheduler");
});
async function runSafetyCutoffScan(source) {
    const snapshot = await db.collectionGroup("devices")
        .where("type", "==", "SAFETY_OUTLET")
        .where("status", "==", "ON")
        .get();
    const notices = (await Promise.all(snapshot.docs.map(enforceSafetyDevice)))
        .filter((notice) => notice !== null);
    await Promise.all(notices.map(sendSafetyNotification));
    v2_1.logger.info("Safety cutoff scan complete", {
        source,
        scanned: snapshot.size,
        cutoffs: notices.length,
    });
}
/**
 * Cloud Scheduler is not emulated locally. Keep the production scheduler above,
 * and run the same server-side scan while the Functions emulator is active.
 */
const runningInEmulator = process.env.FUNCTIONS_EMULATOR === "true"
    || Boolean(process.env.FIRESTORE_EMULATOR_HOST);
if (runningInEmulator) {
    const localSafetyScan = setInterval(() => {
        void runSafetyCutoffScan("emulator").catch((error) => {
            v2_1.logger.error("Local safety cutoff scan failed", { error });
        });
    }, 1_000);
    localSafetyScan.unref();
}
async function enforceSafetyDevice(snapshot) {
    const deviceRef = snapshot.ref;
    const homeRef = deviceRef.parent.parent;
    if (!homeRef)
        return null;
    return db.runTransaction(async (transaction) => {
        const freshSnapshot = await transaction.get(deviceRef);
        const device = freshSnapshot.data();
        if (!device || device.type !== "SAFETY_OUTLET" || device.status !== "ON") {
            return null;
        }
        const capabilities = { ...device.capabilities };
        const maxMinutes = numberValue(capabilities.maxOnDurationMinutes);
        const now = firestore_1.Timestamp.now();
        const onSince = timestamp(capabilities.onSince);
        if (!onSince) {
            capabilities.onSince = now;
            transaction.update(deviceRef, { capabilities, updatedAt: firestore_1.FieldValue.serverTimestamp() });
            return null;
        }
        if (!maxMinutes || !(0, domain_js_1.isSafetyOverdue)(now.toMillis(), onSince.toMillis(), maxMinutes)) {
            return null;
        }
        const cutoffKey = `${deviceRef.id}-${onSince.toMillis()}`;
        const activeRecordId = typeof capabilities.activeUsageRecordId === "string"
            ? capabilities.activeUsageRecordId
            : (0, domain_js_1.safeDocumentId)(`usage-${cutoffKey}`);
        const alertId = (0, domain_js_1.safeDocumentId)(`safety-${cutoffKey}`);
        capabilities.isOn = false;
        capabilities.onSince = null;
        capabilities.activeUsageRecordId = null;
        capabilities.lastSafetyCutoffAt = now;
        transaction.update(deviceRef, {
            status: "OFF",
            capabilities,
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
            updatedBy: "safety-worker",
        });
        transaction.set(homeRef.collection("usageRecords").doc(activeRecordId), {
            deviceId: deviceRef.id,
            startedAt: onSince,
            endedAt: now,
            durationSeconds: (0, domain_js_1.usageDurationSeconds)(onSince.toMillis(), now.toMillis()),
            endedReason: "SAFETY_CUTOFF",
            cutoffKey,
        }, { merge: true });
        transaction.set(homeRef.collection("alerts").doc(alertId), {
            deviceId: deviceRef.id,
            type: "SAFETY_CUTOFF",
            message: `${device.name} was turned off after ${maxMinutes} minutes.`,
            createdAt: now,
            acknowledged: false,
            cutoffKey,
        }, { merge: true });
        return { homeRef, deviceId: deviceRef.id, deviceName: device.name };
    });
}
async function sendSafetyNotification(notice) {
    const tokens = await notice.homeRef.collection("fcmTokens").get();
    const tokenValues = tokens.docs
        .map((document) => document.get("token"))
        .filter((token) => typeof token === "string" && token.length > 0);
    if (tokenValues.length === 0)
        return;
    try {
        await (0, messaging_1.getMessaging)().sendEachForMulticast({
            tokens: tokenValues.slice(0, 500),
            notification: {
                title: "Safety cutoff",
                body: `${notice.deviceName} was switched off automatically.`,
            },
            data: { homeId: notice.homeRef.id, deviceId: notice.deviceId },
            android: { priority: "high" },
        });
    }
    catch (error) {
        v2_1.logger.error("Unable to send safety notification", { error, ...notice });
    }
}
exports.applySchedules = (0, scheduler_1.onSchedule)("every 1 minutes", async () => {
    const schedules = await db.collectionGroup("schedules")
        .where("enabled", "==", true)
        .get();
    const results = await Promise.all(schedules.docs.map(applySchedule));
    v2_1.logger.info("Schedule scan complete", {
        scanned: schedules.size,
        applied: results.filter(Boolean).length,
    });
});
async function applySchedule(scheduleSnapshot) {
    const homeRef = scheduleSnapshot.ref.parent.parent;
    if (!homeRef)
        return false;
    const homeSnapshot = await homeRef.get();
    const timeZone = homeSnapshot.get("timeZone");
    const clock = (0, domain_js_1.localClock)(new Date(), typeof timeZone === "string" ? timeZone : "UTC");
    const schedule = scheduleSnapshot.data();
    const action = (0, domain_js_1.scheduleAction)(schedule, clock);
    if (!action)
        return false;
    const key = (0, domain_js_1.executionKey)(clock, action);
    return db.runTransaction(async (transaction) => {
        const freshScheduleSnapshot = await transaction.get(scheduleSnapshot.ref);
        const freshSchedule = freshScheduleSnapshot.data();
        if (!freshSchedule || !freshSchedule.enabled || freshSchedule.lastExecutionKey === key) {
            return false;
        }
        const deviceRef = homeRef.collection("devices").doc(freshSchedule.deviceId);
        const deviceSnapshot = await transaction.get(deviceRef);
        const device = deviceSnapshot.data();
        if (!device || device.type === "CAMERA")
            return false;
        const capabilities = {
            ...device.capabilities,
            isOn: action === "ON",
        };
        if (device.type === "SAFETY_OUTLET" && action === "ON") {
            capabilities.onSince = firestore_1.Timestamp.now();
            if (typeof freshSchedule.maxOnDurationMinutes === "number") {
                capabilities.maxOnDurationMinutes = freshSchedule.maxOnDurationMinutes;
            }
        }
        transaction.update(deviceRef, {
            status: action,
            capabilities,
            updatedAt: firestore_1.FieldValue.serverTimestamp(),
            updatedBy: `schedule:${scheduleSnapshot.id}`,
        });
        transaction.update(scheduleSnapshot.ref, {
            lastExecutionKey: key,
            lastExecutedAt: firestore_1.FieldValue.serverTimestamp(),
        });
        return true;
    });
}
//# sourceMappingURL=index.js.map