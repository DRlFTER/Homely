export const DEVICE_TYPES = [
  "OUTLET",
  "MULTI_SWITCH",
  "LIGHT",
  "SAFETY_OUTLET",
  "CAMERA",
] as const;

export const DEVICE_STATUSES = [
  "ON",
  "OFF",
  "ERROR",
  "DISCONNECTED",
] as const;

export const USAGE_END_REASONS = [
  "USER",
  "SCHEDULE",
  "SAFETY_CUTOFF",
] as const;

export const ALERT_TYPES = ["SAFETY_CUTOFF", "DEVICE_ERROR"] as const;

export type DeviceType = (typeof DEVICE_TYPES)[number];
export type DeviceStatus = (typeof DEVICE_STATUSES)[number];
export type UsageEndReason = (typeof USAGE_END_REASONS)[number];
export type AlertType = (typeof ALERT_TYPES)[number];

export interface Home {
  name: string;
  ownerId: string;
  memberIds: string[];
  timeZone: string;
  createdAt: FirebaseFirestore.Timestamp | FirebaseFirestore.FieldValue;
}

export interface Floor {
  name: string;
  imageUrl: string;
  gridRows: number;
  gridColumns: number;
  sortOrder: number;
}

export interface Device {
  floorId: string;
  name: string;
  type: DeviceType;
  status: DeviceStatus;
  gridX: number;
  gridY: number;
  capabilities: Record<string, unknown>;
  updatedAt: FirebaseFirestore.Timestamp | FirebaseFirestore.FieldValue;
  updatedBy: string;
}

export interface Schedule {
  deviceId: string;
  enabled: boolean;
  startTime: string;
  endTime: string;
  daysOfWeek: number[];
  maxOnDurationMinutes: number | null;
  lastExecutionKey?: string;
}

export interface UsageRecord {
  deviceId: string;
  startedAt: FirebaseFirestore.Timestamp | FirebaseFirestore.FieldValue;
  endedAt: FirebaseFirestore.Timestamp | FirebaseFirestore.FieldValue | null;
  durationSeconds: number | null;
  endedReason: UsageEndReason | null;
}
