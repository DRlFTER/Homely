import type {DeviceStatus} from "./model.js";

export interface ScheduleLike {
  enabled: boolean;
  startTime: string;
  endTime: string;
  daysOfWeek: number[];
}

export interface LocalClock {
  dateKey: string;
  dayOfWeek: number;
  time: string;
}

const DAY_BY_NAME: Record<string, number> = {
  Mon: 1,
  Tue: 2,
  Wed: 3,
  Thu: 4,
  Fri: 5,
  Sat: 6,
  Sun: 7,
};

export function localClock(date: Date, timeZone: string): LocalClock {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone,
    weekday: "short",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(date);
  const value = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((part) => part.type === type)?.value ?? "";

  return {
    dateKey: `${value("year")}-${value("month")}-${value("day")}`,
    dayOfWeek: DAY_BY_NAME[value("weekday")] ?? 1,
    time: `${value("hour")}:${value("minute")}`,
  };
}

export function scheduleAction(
  schedule: ScheduleLike,
  clock: LocalClock,
): "ON" | "OFF" | null {
  if (!schedule.enabled || !schedule.daysOfWeek.includes(clock.dayOfWeek)) {
    return null;
  }
  if (schedule.startTime === schedule.endTime) {
    return null;
  }
  if (clock.time === schedule.startTime) {
    return "ON";
  }
  if (clock.time === schedule.endTime) {
    return "OFF";
  }
  return null;
}

export function executionKey(clock: LocalClock, action: "ON" | "OFF"): string {
  return `${clock.dateKey}:${clock.time}:${action}`;
}

export function isSafetyOverdue(
  nowMs: number,
  onSinceMs: number,
  maxOnDurationMinutes: number,
): boolean {
  if (!Number.isFinite(maxOnDurationMinutes) || maxOnDurationMinutes <= 0) {
    return false;
  }
  return nowMs >= onSinceMs + maxOnDurationMinutes * 60_000;
}

export function usageDurationSeconds(startedAtMs: number, endedAtMs: number): number {
  return Math.max(0, Math.floor((endedAtMs - startedAtMs) / 1_000));
}

export function statusForSwitches(switches: Array<{isOn: boolean}>): DeviceStatus {
  return switches.some((item) => item.isOn) ? "ON" : "OFF";
}

export function safeDocumentId(value: string): string {
  return value.replace(/[^a-zA-Z0-9_-]/g, "-").slice(0, 120);
}
