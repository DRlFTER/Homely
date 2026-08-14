import assert from "node:assert/strict";
import test from "node:test";
import {
  executionKey,
  isSafetyOverdue,
  localClock,
  scheduleAction,
  statusForSwitches,
  usageDurationSeconds,
} from "./domain.js";

test("safety cutoff becomes due at the exact configured deadline", () => {
  const startedAt = Date.UTC(2026, 7, 14, 10, 0, 0);
  assert.equal(isSafetyOverdue(startedAt + 119_999, startedAt, 2), false);
  assert.equal(isSafetyOverdue(startedAt + 120_000, startedAt, 2), true);
});

test("invalid safety durations never trigger a cutoff", () => {
  assert.equal(isSafetyOverdue(100_000, 0, 0), false);
  assert.equal(isSafetyOverdue(100_000, 0, Number.NaN), false);
});

test("schedule action uses the home's local weekday and minute", () => {
  const clock = localClock(new Date("2026-08-17T01:30:00.000Z"), "Asia/Colombo");
  const schedule = {
    enabled: true,
    startTime: "07:00",
    endTime: "07:30",
    daysOfWeek: [1],
  };
  assert.deepEqual(clock, {dateKey: "2026-08-17", dayOfWeek: 1, time: "07:00"});
  assert.equal(scheduleAction(schedule, clock), "ON");
  assert.equal(executionKey(clock, "ON"), "2026-08-17:07:00:ON");
});

test("multi-switch aggregate status is on when any switch is on", () => {
  assert.equal(statusForSwitches([{isOn: false}, {isOn: true}]), "ON");
  assert.equal(statusForSwitches([{isOn: false}, {isOn: false}]), "OFF");
});

test("usage duration is non-negative whole seconds", () => {
  assert.equal(usageDurationSeconds(1_000, 3_999), 2);
  assert.equal(usageDurationSeconds(5_000, 1_000), 0);
});
