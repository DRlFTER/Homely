# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Native Kotlin and Jetpack Compose with Material 3, Navigation Compose, Hilt, Coroutines and Firebase. A companion Next.js TypeScript application simulates home hardware, while Firebase Authentication, Firestore, Storage, Cloud Messaging and Cloud Functions provide the shared realtime and trusted backend layers.

## Users

The primary user is a resident operating and monitoring a small smart home from an Android phone. During the university demonstration, evaluators also need to understand the realtime relationship between the Android app, hardware simulator and server-enforced safety automation without specialist setup knowledge.

## Product Purpose

Homely lets a resident see devices in their physical floor-plan context, control different device types, schedule selected devices, inspect a mock camera, review usage, and understand safety interventions. Success means state changes remain synchronized between Android and the simulator, while an unattended high-risk appliance is shut down by the backend and leaves a visible, auditable alert and usage record.

## Positioning

Homely makes the home's spatial layout and its trusted safety state one coherent control surface: devices live where they physically belong, while the client visibly defers safety enforcement and history to Firebase rather than pretending a phone timer is authoritative.

## Operating Context

The resident uses the app for quick checks and controls around the home, often one-handed. The demonstration uses two sample floors, a demo account, Firebase emulators, a browser-based hardware simulator, and a shortened one-to-two-minute iron cutoff. Android must remain useful across loading, offline/error, pending command, disconnected device, empty history and alert states.

## Capabilities and Constraints

- Two floor plans with positioned devices and visible grid context.
- Outlet, light, independent multi-switch, safety outlet and mock camera device types.
- Realtime Firestore snapshots are the source of truth; client writes are pending until Firestore confirms them.
- Backend-owned usage sessions, schedules, idempotent safety cutoffs, alerts and optional FCM delivery.
- Safety duration is configurable, with a local countdown that is explanatory rather than authoritative.
- Reports summarize completed usage sessions; alerts remain available until acknowledged.
- Android follows Material 3 structure, system Back, edge-to-edge insets, 48 dp touch targets, semantic colors, large text and dark appearance.
- No real customer claims, production telemetry, proprietary floor imagery or camera footage may be invented.

## Brand Commitments

The product name is Homely. Language is calm, direct and safety-conscious: controls name concrete actions; failures state what did not happen and how the user can recover. Apple design principles may inform fluidity, clarity and restraint, but the Android app must remain recognizably native to Android and must not copy Apple assets or platform-specific controls.

## Evidence on Hand

- The complete product and data requirements are in `SMART_HOME_DEVELOPMENT_HANDOFF.md` outside the repository.
- Architecture, Firebase setup, data model and demo instructions live under `docs/`.
- Deterministic sample floors, devices, schedules, usage and alert records are defined in `functions/src/seed.ts`.
- The web simulator includes authored geometric floor-plan and mock-camera assets under `simulator-web/public/`.
- There are no testimonials, deployment claims, production screenshots or approved brand assets; future work must not fabricate them.

## Product Principles

1. Show the home's state in its physical context.
2. Keep the user in control while making pending, failed and automated actions explicit.
3. Treat safety as a trusted backend responsibility with visible history.
4. Make the common control path immediate; keep configuration one level deeper.
5. Preserve realtime consistency across every client rather than maintaining parallel local truth.

## Accessibility & Inclusion

All essential meaning must survive without color, motion, haptics or sound. The Android interface must support TalkBack labels, system font scaling, dark appearance, reduced animation, minimum 48 dp targets, predictable focus/order and clear error recovery. English is the current demo language, but layouts and labels should allow reflow and later localization.
