import {getApps, initializeApp} from "firebase-admin/app";
import {
  FieldValue,
  Timestamp,
  getFirestore,
  type DocumentReference,
} from "firebase-admin/firestore";
import {getMessaging} from "firebase-admin/messaging";
import {logger, setGlobalOptions} from "firebase-functions/v2";
import {onDocumentUpdated} from "firebase-functions/v2/firestore";
import {onRequest} from "firebase-functions/v2/https";
import {onSchedule} from "firebase-functions/v2/scheduler";
import {
  executionKey,
  isSafetyOverdue,
  localClock,
  safeDocumentId,
  scheduleAction,
  usageDurationSeconds,
} from "./domain.js";
import type {Device, Schedule, UsageEndReason} from "./model.js";

getApps()[0] ?? initializeApp();
const db = getFirestore();

setGlobalOptions({
  region: "asia-south1",
  maxInstances: 10,
});

/** Lightweight endpoint used to verify the Functions emulator/deployment. */
export const health = onRequest((_request, response) => {
  response.status(200).json({service: "homely-functions", status: "ok"});
});

function timestamp(value: unknown): Timestamp | null {
  return value instanceof Timestamp ? value : null;
}

function numberValue(value: unknown): number | null {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function endReason(updatedBy: unknown): UsageEndReason {
  if (typeof updatedBy === "string" && updatedBy.startsWith("schedule:")) {
    return "SCHEDULE";
  }
  if (updatedBy === "safety-worker") {
    return "SAFETY_CUTOFF";
  }
  return "USER";
}

export const onDeviceStateChanged = onDocumentUpdated(
  "homes/{homeId}/devices/{deviceId}",
  async (event) => {
    const change = event.data;
    if (!change) return;

    const before = change.before.data() as Device;
    const after = change.after.data() as Device;
    const beforeStatus = before.status;
    const afterStatus = after.status;
    const homeId = event.params.homeId;
    const deviceId = event.params.deviceId;
    const deviceRef = change.after.ref;
    const homeRef = deviceRef.parent.parent;
    if (!homeRef) return;

    if (beforeStatus !== "ERROR" && afterStatus === "ERROR") {
      const alertId = safeDocumentId(`error-${deviceId}-${event.id}`);
      await homeRef.collection("alerts").doc(alertId).set({
        deviceId,
        type: "DEVICE_ERROR",
        message: `${after.name} reported an error.`,
        createdAt: FieldValue.serverTimestamp(),
        acknowledged: false,
        sourceEventId: event.id,
      }, {merge: true});
    }

    if (beforeStatus === afterStatus || after.type === "CAMERA") return;

    if (afterStatus === "ON") {
      await startUsageSession(deviceRef, homeRef, deviceId, after, event.id);
      return;
    }

    if (beforeStatus === "ON") {
      await closeUsageSession(deviceRef, homeRef, deviceId, before, after, event.id);
    }
  },
);

async function startUsageSession(
  deviceRef: DocumentReference,
  homeRef: DocumentReference,
  deviceId: string,
  transitionDevice: Device,
  eventId: string,
): Promise<void> {
  await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(deviceRef);
    const current = snapshot.data() as Device | undefined;
    if (!current || current.status !== "ON") return;

    const currentCapabilities = {...current.capabilities};
    if (typeof currentCapabilities.activeUsageRecordId === "string") return;

    const now = Timestamp.now();
    const recordId = safeDocumentId(`usage-${deviceId}-${eventId}`);
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
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.set(homeRef.collection("usageRecords").doc(recordId), {
      deviceId,
      startedAt,
      endedAt: null,
      durationSeconds: null,
      endedReason: null,
      sourceEventId: eventId,
    }, {merge: true});
  });
}

async function closeUsageSession(
  deviceRef: DocumentReference,
  homeRef: DocumentReference,
  deviceId: string,
  before: Device,
  after: Device,
  eventId: string,
): Promise<void> {
  await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(deviceRef);
    const current = snapshot.data() as Device | undefined;
    if (!current || current.status === "ON") return;

    const beforeCapabilities = before.capabilities ?? {};
    const currentCapabilities = {...current.capabilities};
    const activeRecordId = typeof beforeCapabilities.activeUsageRecordId === "string"
      ? beforeCapabilities.activeUsageRecordId
      : typeof currentCapabilities.activeUsageRecordId === "string"
        ? currentCapabilities.activeUsageRecordId
        : safeDocumentId(`usage-${deviceId}-${eventId}`);
    const endedAt = Timestamp.now();
    const startedAt = timestamp(beforeCapabilities.onSince)
      ?? timestamp(before.updatedAt)
      ?? endedAt;

    currentCapabilities.isOn = false;
    currentCapabilities.onSince = null;
    currentCapabilities.activeUsageRecordId = null;
    transaction.update(deviceRef, {
      capabilities: currentCapabilities,
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.set(homeRef.collection("usageRecords").doc(activeRecordId), {
      deviceId,
      startedAt,
      endedAt,
      durationSeconds: usageDurationSeconds(startedAt.toMillis(), endedAt.toMillis()),
      endedReason: endReason(after.updatedBy),
      closedByEventId: eventId,
    }, {merge: true});
  });
}

interface CutoffNotice {
  homeRef: DocumentReference;
  deviceId: string;
  deviceName: string;
}

export const enforceSafetyCutoffs = onSchedule("every 1 minutes", async () => {
  await runSafetyCutoffScan("scheduler");
});

async function runSafetyCutoffScan(source: "scheduler" | "emulator"): Promise<void> {
  const snapshot = await db.collectionGroup("devices")
    .where("type", "==", "SAFETY_OUTLET")
    .where("status", "==", "ON")
    .get();
  const notices = (await Promise.all(snapshot.docs.map(enforceSafetyDevice)))
    .filter((notice): notice is CutoffNotice => notice !== null);
  await Promise.all(notices.map(sendSafetyNotification));
  logger.info("Safety cutoff scan complete", {
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
    void runSafetyCutoffScan("emulator").catch((error: unknown) => {
      logger.error("Local safety cutoff scan failed", {error});
    });
  }, 1_000);
  localSafetyScan.unref();
}

async function enforceSafetyDevice(
  snapshot: FirebaseFirestore.QueryDocumentSnapshot,
): Promise<CutoffNotice | null> {
  const deviceRef = snapshot.ref;
  const homeRef = deviceRef.parent.parent;
  if (!homeRef) return null;

  return db.runTransaction(async (transaction) => {
    const freshSnapshot = await transaction.get(deviceRef);
    const device = freshSnapshot.data() as Device | undefined;
    if (!device || device.type !== "SAFETY_OUTLET" || device.status !== "ON") {
      return null;
    }

    const capabilities = {...device.capabilities};
    const maxMinutes = numberValue(capabilities.maxOnDurationMinutes);
    const now = Timestamp.now();
    const onSince = timestamp(capabilities.onSince);
    if (!onSince) {
      capabilities.onSince = now;
      transaction.update(deviceRef, {capabilities, updatedAt: FieldValue.serverTimestamp()});
      return null;
    }
    if (!maxMinutes || !isSafetyOverdue(now.toMillis(), onSince.toMillis(), maxMinutes)) {
      return null;
    }

    const cutoffKey = `${deviceRef.id}-${onSince.toMillis()}`;
    const activeRecordId = typeof capabilities.activeUsageRecordId === "string"
      ? capabilities.activeUsageRecordId
      : safeDocumentId(`usage-${cutoffKey}`);
    const alertId = safeDocumentId(`safety-${cutoffKey}`);
    capabilities.isOn = false;
    capabilities.onSince = null;
    capabilities.activeUsageRecordId = null;
    capabilities.lastSafetyCutoffAt = now;

    transaction.update(deviceRef, {
      status: "OFF",
      capabilities,
      updatedAt: FieldValue.serverTimestamp(),
      updatedBy: "safety-worker",
    });
    transaction.set(homeRef.collection("usageRecords").doc(activeRecordId), {
      deviceId: deviceRef.id,
      startedAt: onSince,
      endedAt: now,
      durationSeconds: usageDurationSeconds(onSince.toMillis(), now.toMillis()),
      endedReason: "SAFETY_CUTOFF",
      cutoffKey,
    }, {merge: true});
    transaction.set(homeRef.collection("alerts").doc(alertId), {
      deviceId: deviceRef.id,
      type: "SAFETY_CUTOFF",
      message: `${device.name} was turned off after ${maxMinutes} minutes.`,
      createdAt: now,
      acknowledged: false,
      cutoffKey,
    }, {merge: true});

    return {homeRef, deviceId: deviceRef.id, deviceName: device.name};
  });
}

async function sendSafetyNotification(notice: CutoffNotice): Promise<void> {
  const tokens = await notice.homeRef.collection("fcmTokens").get();
  const tokenValues = tokens.docs
    .map((document) => document.get("token"))
    .filter((token): token is string => typeof token === "string" && token.length > 0);
  if (tokenValues.length === 0) return;

  try {
    await getMessaging().sendEachForMulticast({
      tokens: tokenValues.slice(0, 500),
      notification: {
        title: "Safety cutoff",
        body: `${notice.deviceName} was switched off automatically.`,
      },
      data: {homeId: notice.homeRef.id, deviceId: notice.deviceId},
      android: {priority: "high"},
    });
  } catch (error) {
    logger.error("Unable to send safety notification", {error, ...notice});
  }
}

export const applySchedules = onSchedule("every 1 minutes", async () => {
  const schedules = await db.collectionGroup("schedules")
    .where("enabled", "==", true)
    .get();
  const results = await Promise.all(schedules.docs.map(applySchedule));
  logger.info("Schedule scan complete", {
    scanned: schedules.size,
    applied: results.filter(Boolean).length,
  });
});

async function applySchedule(
  scheduleSnapshot: FirebaseFirestore.QueryDocumentSnapshot,
): Promise<boolean> {
  const homeRef = scheduleSnapshot.ref.parent.parent;
  if (!homeRef) return false;
  const homeSnapshot = await homeRef.get();
  const timeZone = homeSnapshot.get("timeZone");
  const clock = localClock(new Date(), typeof timeZone === "string" ? timeZone : "UTC");
  const schedule = scheduleSnapshot.data() as Schedule;
  const action = scheduleAction(schedule, clock);
  if (!action) return false;
  const key = executionKey(clock, action);

  return db.runTransaction(async (transaction) => {
    const freshScheduleSnapshot = await transaction.get(scheduleSnapshot.ref);
    const freshSchedule = freshScheduleSnapshot.data() as Schedule | undefined;
    if (!freshSchedule || !freshSchedule.enabled || freshSchedule.lastExecutionKey === key) {
      return false;
    }
    const deviceRef = homeRef.collection("devices").doc(freshSchedule.deviceId);
    const deviceSnapshot = await transaction.get(deviceRef);
    const device = deviceSnapshot.data() as Device | undefined;
    if (!device || device.type === "CAMERA") return false;

    const capabilities: Record<string, unknown> = {
      ...device.capabilities,
      isOn: action === "ON",
    };
    if (device.type === "SAFETY_OUTLET" && action === "ON") {
      capabilities.onSince = Timestamp.now();
      if (typeof freshSchedule.maxOnDurationMinutes === "number") {
        capabilities.maxOnDurationMinutes = freshSchedule.maxOnDurationMinutes;
      }
    }
    transaction.update(deviceRef, {
      status: action,
      capabilities,
      updatedAt: FieldValue.serverTimestamp(),
      updatedBy: `schedule:${scheduleSnapshot.id}`,
    });
    transaction.update(scheduleSnapshot.ref, {
      lastExecutionKey: key,
      lastExecutedAt: FieldValue.serverTimestamp(),
    });
    return true;
  });
}
