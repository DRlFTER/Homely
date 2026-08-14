import {getApps, initializeApp} from "firebase-admin/app";
import {getAuth} from "firebase-admin/auth";
import {FieldValue, Timestamp, getFirestore} from "firebase-admin/firestore";

const PROJECT_ID = "demo-smart-home";
const DEMO_UID = "demo-user";
const DEMO_EMAIL = "demo@homely.local";
const DEMO_PASSWORD = "homely-demo-123";
const HOME_ID = "demo-home";

process.env.GCLOUD_PROJECT ??= PROJECT_ID;
process.env.FIREBASE_AUTH_EMULATOR_HOST ??= "127.0.0.1:9099";
process.env.FIRESTORE_EMULATOR_HOST ??= "127.0.0.1:8080";

if (!process.env.FIRESTORE_EMULATOR_HOST || !process.env.FIREBASE_AUTH_EMULATOR_HOST) {
  throw new Error("The seed script may only run against Firebase emulators.");
}

const app = getApps()[0] ?? initializeApp({
  projectId: PROJECT_ID,
});

const auth = getAuth(app);
const db = getFirestore(app);

async function upsertDemoUser(): Promise<void> {
  try {
    await auth.getUser(DEMO_UID);
    await auth.updateUser(DEMO_UID, {
      email: DEMO_EMAIL,
      password: DEMO_PASSWORD,
      displayName: "Homely Demo",
    });
  } catch (error) {
    const code = (error as {code?: string}).code;
    if (code !== "auth/user-not-found") {
      throw error;
    }

    await auth.createUser({
      uid: DEMO_UID,
      email: DEMO_EMAIL,
      password: DEMO_PASSWORD,
      displayName: "Homely Demo",
      emailVerified: true,
    });
  }
}

async function seedFirestore(): Promise<void> {
  const homeRef = db.collection("homes").doc(HOME_ID);
  const batch = db.batch();

  batch.set(homeRef, {
    name: "Homely Demo House",
    ownerId: DEMO_UID,
    memberIds: [],
    timeZone: "Asia/Colombo",
    createdAt: FieldValue.serverTimestamp(),
  });

  batch.set(homeRef.collection("floors").doc("ground-floor"), {
    name: "Ground floor",
    imageUrl: "/floorplans/ground-floor.svg",
    gridRows: 8,
    gridColumns: 12,
    sortOrder: 0,
  });

  batch.set(homeRef.collection("floors").doc("first-floor"), {
    name: "First floor",
    imageUrl: "/floorplans/first-floor.svg",
    gridRows: 8,
    gridColumns: 12,
    sortOrder: 1,
  });

  const devices = {
    "living-room-light": {
      floorId: "ground-floor",
      name: "Living room light",
      type: "LIGHT",
      status: "OFF",
      gridX: 3,
      gridY: 3,
      capabilities: {isOn: false},
    },
    "kitchen-outlet": {
      floorId: "ground-floor",
      name: "Kitchen outlet",
      type: "OUTLET",
      status: "OFF",
      gridX: 9,
      gridY: 5,
      capabilities: {isOn: false},
    },
    "hall-switches": {
      floorId: "ground-floor",
      name: "Hall switches",
      type: "MULTI_SWITCH",
      status: "ON",
      gridX: 6,
      gridY: 4,
      capabilities: {
        switches: [
          {id: "switch-1", label: "Living room", isOn: false},
          {id: "switch-2", label: "Hallway", isOn: true},
        ],
      },
    },
    "bedroom-iron": {
      floorId: "first-floor",
      name: "Iron safety outlet",
      type: "SAFETY_OUTLET",
      status: "OFF",
      gridX: 4,
      gridY: 5,
      capabilities: {
        isOn: false,
        onSince: null,
        maxOnDurationMinutes: 2,
      },
    },
    "entry-camera": {
      floorId: "ground-floor",
      name: "Entry camera",
      type: "CAMERA",
      status: "ON",
      gridX: 1,
      gridY: 1,
      capabilities: {
        snapshotUrl: "/camera/entry-camera.svg",
        lastSnapshotAt: Timestamp.now(),
        streamUrl: "mock://camera/entry",
      },
    },
    "bedroom-light": {
      floorId: "first-floor",
      name: "Bedroom light",
      type: "LIGHT",
      status: "OFF",
      gridX: 8,
      gridY: 3,
      capabilities: {isOn: false},
    },
  } as const;

  for (const [deviceId, device] of Object.entries(devices)) {
    batch.set(homeRef.collection("devices").doc(deviceId), {
      ...device,
      updatedAt: FieldValue.serverTimestamp(),
      updatedBy: "seed",
    });
  }

  batch.set(homeRef.collection("schedules").doc("weekday-living-room"), {
    deviceId: "living-room-light",
    enabled: false,
    startTime: "18:00",
    endTime: "22:30",
    daysOfWeek: [1, 2, 3, 4, 5],
    maxOnDurationMinutes: null,
  });

  batch.set(homeRef.collection("schedules").doc("iron-demo-cutoff"), {
    deviceId: "bedroom-iron",
    enabled: false,
    startTime: "10:00",
    endTime: "10:15",
    daysOfWeek: [1, 2, 3, 4, 5, 6, 7],
    maxOnDurationMinutes: 2,
  });

  const now = Date.now();
  batch.set(homeRef.collection("usageRecords").doc("seed-living-room-session"), {
    deviceId: "living-room-light",
    startedAt: Timestamp.fromMillis(now - 3_600_000),
    endedAt: Timestamp.fromMillis(now - 2_880_000),
    durationSeconds: 720,
    endedReason: "USER",
    seeded: true,
  });

  batch.set(homeRef.collection("usageRecords").doc("seed-iron-session"), {
    deviceId: "bedroom-iron",
    startedAt: Timestamp.fromMillis(now - 7_200_000),
    endedAt: Timestamp.fromMillis(now - 7_080_000),
    durationSeconds: 120,
    endedReason: "SAFETY_CUTOFF",
    seeded: true,
  });

  batch.set(homeRef.collection("alerts").doc("seed-safety-alert"), {
    deviceId: "bedroom-iron",
    type: "SAFETY_CUTOFF",
    message: "Iron safety outlet was turned off after 2 minutes.",
    createdAt: Timestamp.fromMillis(now - 7_080_000),
    acknowledged: true,
    seeded: true,
  });

  await batch.commit();
}

async function main(): Promise<void> {
  await upsertDemoUser();
  await seedFirestore();
  console.log(`Seeded ${HOME_ID} for ${DEMO_EMAIL} in ${PROJECT_ID} emulators.`);
}

main().catch((error: unknown) => {
  console.error(error);
  process.exitCode = 1;
});
