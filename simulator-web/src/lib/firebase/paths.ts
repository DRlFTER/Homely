import {collection, doc, type Firestore} from "firebase/firestore";

export const homeDocument = (db: Firestore, homeId: string) =>
  doc(db, "homes", homeId);

export const floorsCollection = (db: Firestore, homeId: string) =>
  collection(db, "homes", homeId, "floors");

export const devicesCollection = (db: Firestore, homeId: string) =>
  collection(db, "homes", homeId, "devices");

export const schedulesCollection = (db: Firestore, homeId: string) =>
  collection(db, "homes", homeId, "schedules");

export const usageRecordsCollection = (db: Firestore, homeId: string) =>
  collection(db, "homes", homeId, "usageRecords");

export const alertsCollection = (db: Firestore, homeId: string) =>
  collection(db, "homes", homeId, "alerts");

