"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const strict_1 = __importDefault(require("node:assert/strict"));
const node_test_1 = __importDefault(require("node:test"));
const domain_js_1 = require("./domain.js");
(0, node_test_1.default)("safety cutoff becomes due at the exact configured deadline", () => {
    const startedAt = Date.UTC(2026, 7, 14, 10, 0, 0);
    strict_1.default.equal((0, domain_js_1.isSafetyOverdue)(startedAt + 119_999, startedAt, 2), false);
    strict_1.default.equal((0, domain_js_1.isSafetyOverdue)(startedAt + 120_000, startedAt, 2), true);
});
(0, node_test_1.default)("invalid safety durations never trigger a cutoff", () => {
    strict_1.default.equal((0, domain_js_1.isSafetyOverdue)(100_000, 0, 0), false);
    strict_1.default.equal((0, domain_js_1.isSafetyOverdue)(100_000, 0, Number.NaN), false);
});
(0, node_test_1.default)("schedule action uses the home's local weekday and minute", () => {
    const clock = (0, domain_js_1.localClock)(new Date("2026-08-17T01:30:00.000Z"), "Asia/Colombo");
    const schedule = {
        enabled: true,
        startTime: "07:00",
        endTime: "07:30",
        daysOfWeek: [1],
    };
    strict_1.default.deepEqual(clock, { dateKey: "2026-08-17", dayOfWeek: 1, time: "07:00" });
    strict_1.default.equal((0, domain_js_1.scheduleAction)(schedule, clock), "ON");
    strict_1.default.equal((0, domain_js_1.executionKey)(clock, "ON"), "2026-08-17:07:00:ON");
});
(0, node_test_1.default)("multi-switch aggregate status is on when any switch is on", () => {
    strict_1.default.equal((0, domain_js_1.statusForSwitches)([{ isOn: false }, { isOn: true }]), "ON");
    strict_1.default.equal((0, domain_js_1.statusForSwitches)([{ isOn: false }, { isOn: false }]), "OFF");
});
(0, node_test_1.default)("usage duration is non-negative whole seconds", () => {
    strict_1.default.equal((0, domain_js_1.usageDurationSeconds)(1_000, 3_999), 2);
    strict_1.default.equal((0, domain_js_1.usageDurationSeconds)(5_000, 1_000), 0);
});
//# sourceMappingURL=domain.test.js.map