import {FirebaseApp, getApp, getApps, initializeApp} from "firebase/app";
import {Auth, connectAuthEmulator, getAuth} from "firebase/auth";
import {Firestore, connectFirestoreEmulator, getFirestore} from "firebase/firestore";
import {FirebaseStorage, connectStorageEmulator, getStorage} from "firebase/storage";

export interface FirebaseClient {
  app: FirebaseApp;
  auth: Auth;
  db: Firestore;
  storage: FirebaseStorage;
}

let client: FirebaseClient | undefined;

function requiredEnvironmentValue(name: string, value: string | undefined): string {
  if (!value) {
    throw new Error(`Missing required Firebase environment value: ${name}`);
  }
  return value;
}

export function getFirebaseClient(): FirebaseClient {
  if (client) {
    return client;
  }

  const projectId = requiredEnvironmentValue(
    "NEXT_PUBLIC_FIREBASE_PROJECT_ID",
    process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  );

  const app = getApps().length > 0
    ? getApp()
    : initializeApp({
        apiKey: requiredEnvironmentValue(
          "NEXT_PUBLIC_FIREBASE_API_KEY",
          process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
        ),
        authDomain: requiredEnvironmentValue(
          "NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN",
          process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
        ),
        projectId,
        storageBucket: requiredEnvironmentValue(
          "NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET",
          process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
        ),
        messagingSenderId: requiredEnvironmentValue(
          "NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID",
          process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
        ),
        appId: requiredEnvironmentValue(
          "NEXT_PUBLIC_FIREBASE_APP_ID",
          process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
        ),
      });

  const auth = getAuth(app);
  const db = getFirestore(app);
  const storage = getStorage(app);

  if (process.env.NEXT_PUBLIC_USE_FIREBASE_EMULATORS === "true") {
    connectAuthEmulator(auth, "http://127.0.0.1:9099", {disableWarnings: true});
    connectFirestoreEmulator(db, "127.0.0.1", 8080);
    connectStorageEmulator(storage, "127.0.0.1", 9199);
  }

  client = {app, auth, db, storage};
  return client;
}

