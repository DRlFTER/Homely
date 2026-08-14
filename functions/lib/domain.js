"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.localClock = localClock;
exports.scheduleAction = scheduleAction;
exports.executionKey = executionKey;
exports.isSafetyOverdue = isSafetyOverdue;
exports.usageDurationSeconds = usageDurationSeconds;
exports.statusForSwitches = statusForSwitches;
exports.safeDocumentId = safeDocumentId;
const DAY_BY_NAME = {
    Mon: 1,
    Tue: 2,
    Wed: 3,
    Thu: 4,
    Fri: 5,
    Sat: 6,
    Sun: 7,
};
function localClock(date, timeZone) {
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
    const value = (type) => parts.find((part) => part.type === type)?.value ?? "";
    return {
        dateKey: `${value("year")}-${value("month")}-${value("day")}`,
        dayOfWeek: DAY_BY_NAME[value("weekday")] ?? 1,
        time: `${value("hour")}:${value("minute")}`,
    };
}
function scheduleAction(schedule, clock) {
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
function executionKey(clock, action) {
    return `${clock.dateKey}:${clock.time}:${action}`;
}
function isSafetyOverdue(nowMs, onSinceMs, maxOnDurationMinutes) {
    if (!Number.isFinite(maxOnDurationMinutes) || maxOnDurationMinutes <= 0) {
        return false;
    }
    return nowMs >= onSinceMs + maxOnDurationMinutes * 60_000;
}
function usageDurationSeconds(startedAtMs, endedAtMs) {
    return Math.max(0, Math.floor((endedAtMs - startedAtMs) / 1_000));
}
function statusForSwitches(switches) {
    return switches.some((item) => item.isOn) ? "ON" : "OFF";
}
function safeDocumentId(value) {
    return value.replace(/[^a-zA-Z0-9_-]/g, "-").slice(0, 120);
}
//# sourceMappingURL=domain.js.map