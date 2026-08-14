import type {FieldValue, Timestamp} from "firebase/firestore";

export const DEVICE_TYPES = [
  "OUTLET",
  "MULTI_SWITCH",
  "LIGHT",
  "SAFETY_OUTLET",
  "CAMERA",
] as const;

export const DEVICE_STATUSES = ["ON", "OFF", "ERROR", "DISCONNECTED"] as const;

export type DeviceType = (typeof DEVICE_TYPES)[number];
export type DeviceStatus = (typeof DEVICE_STATUSES)[number];

export interface Home {
  name: string;
  ownerId: string;
  memberIds: string[];
  timeZone: string;
  createdAt: Timestamp;
}

export interface Floor {
  id: string;
  name: string;
  imageUrl: string;
  gridRows: number;
  gridColumns: number;
  sortOrder: number;
}

export interface Device {
  id: string;
  floorId: string;
  name: string;
  type: DeviceType;
  status: DeviceStatus;
  gridX: number;
  gridY: number;
  capabilities: Record<string, unknown>;
  updatedAt: Timestamp | FieldValue;
  updatedBy: string;
}

export interface DeviceSwitch {
  id: string;
  label: string;
  isOn: boolean;
}

export interface Schedule {
  id: string;
  deviceId: string;
  enabled: boolean;
  startTime: string;
  endTime: string;
  daysOfWeek: number[];
  maxOnDurationMinutes: number | null;
}

export type UsageEndReason = "USER" | "SCHEDULE" | "SAFETY_CUTOFF";

export interface UsageRecord {
  id: string;
  deviceId: string;
  startedAt: Timestamp;
  endedAt: Timestamp | null;
  durationSeconds: number | null;
  endedReason: UsageEndReason | null;
}

export type AlertType = "SAFETY_CUTOFF" | "DEVICE_ERROR";

export interface Alert {
  id: string;
  deviceId: string;
  type: AlertType;
  message: string;
  createdAt: Timestamp;
  acknowledged: boolean;
}
