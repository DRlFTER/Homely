# Homely — Next Steps Handoff

Last updated: 2026-08-15  
Workspace: `C:\Users\wwwka\OneDrive\Documents\repositories\Homely`

This document records the state at the point implementation was stopped. The core Firebase backend, browser simulator, and native Android application are implemented and compile successfully. The remaining work is primarily end-to-end runtime verification, connecting a real Firebase project, release signing, and recording the demonstration.

## 1. Current status

| Area | Status | Notes |
| --- | --- | --- |
| Firebase Functions | Implemented and tested | Health endpoint, state-change trigger, usage sessions, safety cutoffs, schedules, alerts, and FCM notifications |
| Firestore | Implemented | Rules, indexes, domain model, and emulator seed data |
| Browser simulator | Implemented and built | Next.js/TypeScript dashboard with real-time controls and monitoring |
| Android app | Implemented and built | Native Kotlin/Jetpack Compose application using Firebase SDKs |
| Android design | Implemented | “Lived-in Light” direction; Apple design principles adapted to native Material 3 behavior |
| Automated checks | Passing | Functions tests: 5/5; TypeScript checks/builds pass; Android compile/tests/lint/APK pass |
| Local emulator seed | Verified | Auth, Firestore, Storage, and Functions loaded; demo data was seeded successfully |
| Pub/Sub emulator integration | **Not yet verified** | The final Pub/Sub-enabled run was interrupted before completion |
| Real Firebase project | **Not configured or deployed** | No production Firebase resources were created or modified |
| Android device/emulator UI test | **Not yet performed** | APK was built, but the app still needs a real runtime walkthrough |
| Signed release APK | **Not yet produced** | The existing APK is a debug build |

## 2. What is already implemented

### Firebase backend

Important files:

- `functions/src/index.ts` — callable/runtime entry points and triggers.
- `functions/src/domain.ts` — core smart-home business rules.
- `functions/src/domain.test.ts` — domain tests.
- `functions/src/model.ts` — shared backend models.
- `functions/src/seed.ts` — repeatable emulator demo seed.
- `firestore.rules` and `firestore.indexes.json` — database access and query configuration.

Implemented behavior:

- HTTP health endpoint.
- Firestore device-state trigger.
- Idempotent usage-session creation and closure.
- Safety-device deadline enforcement.
- Device-error and safety alerts.
- FCM multicast notification dispatch.
- Scheduled device automation.
- Two floors and six representative device types in the seed data.
- Seeded schedules, usage records, alerts, and a mock camera feed.

### Browser simulator

The simulator in `simulator-web/` includes:

- Firebase Authentication and real-time Firestore listeners.
- Floor selection and controls.
- Independent multi-switch controls.
- Safety-duration settings.
- Camera, usage, alert, and alert-acknowledgement views.
- Authored floor-plan and camera assets under `simulator-web/public/`.
- A successful production static-export build.

### Android application

The native application in `android-app/` includes:

- Kotlin, Jetpack Compose, MVVM, and `StateFlow`.
- Firebase Authentication, Firestore, Storage, Messaging, and Functions SDKs.
- Debug-mode Firebase emulator routing through `10.0.2.2`.
- Compact bottom navigation and expanded navigation rail.
- Floor grid, positioned device markers, and warm room glow for active devices.
- Device detail sheet and controls for lights, outlets, safety devices, multi-switches, and camera.
- Server-authoritative safety timeout and explanatory countdown.
- Schedules, reports, alerts, pending states, errors, and haptic feedback.
- Notification permission and messaging service.
- TalkBack semantics, 48 dp minimum markers, dark theme, system Back, and edge-to-edge layout.
- Product/design documentation in `PRODUCT.md`, `DESIGN.md`, and `.impeccable/design.json`.

Hilt is intentionally not used. A small explicit `ViewModel` factory replaced it after the Hilt/KSP artifact download repeatedly stalled. This keeps the current client simpler. Restore Hilt only if it is an explicit marking requirement.

## 3. Verification already completed

The following completed successfully:

```powershell
npm.cmd run setup:check
npm.cmd run check
npm.cmd test
npm.cmd run build
npm.cmd run android:check
```

Results:

- Cloud Functions domain tests: **5/5 passed**.
- Functions TypeScript check/build: **passed**.
- Simulator TypeScript check and production build: **passed**.
- Android `compileDebugKotlin`, unit tests, lint, and `assembleDebug`: **passed**.
- Android lint: **0 errors, 15 warnings**. The warnings are primarily dependency/update and Android API deprecation notices, not build blockers.
- Debug APK created at:
  `android-app/app/build/outputs/apk/debug/app-debug.apk`

The generated APK is roughly 18 MB and uses the debug keystore. Build outputs are normally ignored by Git, so it must be rebuilt on another machine after cloning.

## 4. Do these next, in order

### Step 1 — Complete the interrupted Pub/Sub emulator check

Firebase scheduled functions are backed by Pub/Sub. Pub/Sub was added to `firebase.json`, but the first run including it was stopped before the emulator download/startup and scheduled-function registration could be confirmed.

From PowerShell in the repository root:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = 'C:\Program Files\Android\Android Studio\jbr\bin;' + $env:Path
$env:FUNCTIONS_DISCOVERY_TIMEOUT = '60'
firebase.cmd emulators:exec --project demo-smart-home --only auth,firestore,functions,storage,pubsub "npm.cmd --prefix functions run seed"
```

Confirm all of the following in the output:

- `health`, `onDeviceStateChanged`, `enforceSafetyCutoffs`, and `applySchedules` load.
- The two scheduled functions initialize through the Pub/Sub emulator.
- There is no message saying scheduled functions were ignored because the Pub/Sub emulator does not exist.
- The seed finishes with a success message for `demo-home` and `demo@homely.local`.

The root emulator scripts now set `FUNCTIONS_DISCOVERY_TIMEOUT=60` automatically through `scripts/start-emulators.mjs`. The environment variable is only shown above because that command calls Firebase directly.

### Step 2 — Run the complete local system

Use separate terminals.

Terminal 1:

```powershell
npm.cmd run emulators:fresh
```

Terminal 2, after the emulators are ready:

```powershell
npm.cmd run seed
npm.cmd run dev:simulator
```

Then open the Android project in Android Studio, start an Android emulator, and run the `app` configuration. The Android emulator reaches the host Firebase emulators through `10.0.2.2`.

Use the demo account created by the seed:

```text
Email: demo@homely.local
```

If a password is needed, check the current value in `functions/src/seed.ts` rather than copying credentials into this document.

### Step 3 — Perform a two-client walkthrough

Keep the browser simulator and Android app open together. Verify:

- A device change on Android appears in the browser in real time.
- A browser change appears on Android in real time.
- Each channel of a multi-switch changes independently.
- Both floors load and retain their correct devices and positions.
- Camera, schedules, usage reports, and alerts render correctly.
- An alert can be acknowledged from the UI.
- Pending and error feedback appears during a failed or delayed write.
- Stopping and restarting an emulator/client reconnects without duplicate usage sessions.
- A safety device with a short timeout turns off, closes its usage session, creates an alert, and produces notification behavior.

Scheduled-function timing can differ in the emulator. If the scheduler does not automatically publish at the expected cadence, inspect the Pub/Sub emulator topics/logs and publish to the generated Firebase schedule topic manually. The unit-tested deadline logic already passes, but the complete trigger-to-write path still needs this runtime verification.

### Step 4 — Test Android UX on actual runtime targets

No Android emulator/device visual walkthrough has been done yet. Check at minimum:

- Phone portrait layout.
- A tablet or resizable emulator where the navigation rail appears.
- Light and dark themes.
- System font size at approximately 200%.
- TalkBack labels and focus order.
- System Back behavior from sheets/details.
- Notification permission and received notifications.
- Offline/reconnect behavior.
- Safety countdown wording and server-authoritative cutoff behavior.

Fix runtime issues before doing dependency upgrades or optional feature work.

### Step 5 — Connect a real Firebase project

This requires the project owner’s Firebase account, billing decision, and permission to deploy. Nothing has been deployed yet.

In Firebase Console:

1. Create or select the Firebase project.
2. Enable Email/Password Authentication.
3. Create Firestore.
4. Enable Storage.
5. Register a Web app and put its values in `simulator-web/.env.local`.
6. Register Android package `com.homely.smarthome`.
7. Download `google-services.json` into `android-app/app/`.
8. Enable the Google Services Gradle plugin where indicated in the Android Gradle files.
9. Run `firebase use --add` and select the project.
10. Review the Firestore rules against the final owner/member model.
11. Confirm whether the project may use the Blaze plan; scheduled Functions and outbound services may require billing.
12. Deploy rules, indexes, functions, Storage rules, and hosting only after receiving approval.

Never commit the following:

- `.env.local`
- `google-services.json`
- Firebase service-account keys
- Android signing keystores or signing passwords

See `docs/firebase-setup.md` and `docs/local-development.md` for the repository-specific configuration notes.

### Step 6 — Produce a signed release APK

The existing APK is debug-only. For the deliverable:

1. Create a release keystore and store it outside the repository.
2. Supply signing values through local Gradle properties or environment variables.
3. Add/enable a release signing configuration without hard-coding secrets.
4. Build the release variant.
5. Install the release APK on a clean device/emulator.
6. Verify Firebase connectivity and the entire demo once more.
7. Keep a secure backup of the keystore; losing it prevents signing future updates with the same identity.

### Step 7 — Record and package the demonstration

Follow `docs/demo-script.md`. Record only after the two-client walkthrough and release build are stable. The recording should show:

- Authentication.
- Both floors.
- Browser-to-Android and Android-to-browser updates.
- Independent multi-switch behavior.
- Safety cutoff, usage closure, alert creation, and acknowledgement.
- Schedules, reports, camera, and notification behavior.

### Step 8 — Commit and hand off the repository safely

No commit or push was performed during this implementation session. Before creating the first handoff commit:

```powershell
git status --short
git diff --check
```

Review every untracked file, confirm ignored secrets and build outputs are absent, then create a normal feature branch/commit and push it to the team repository.

## 5. Package-manager decision

The current repository uses npm with separate dependency roots:

- `functions/package-lock.json`
- `simulator-web/package-lock.json`
- Root commands orchestrate them with `npm --prefix`.

This is valid even though both applications live in one repository. Do not mix pnpm into the current checkout merely to finish the MVP. If the team deliberately chooses pnpm later, treat it as one controlled migration: add `pnpm-workspace.yaml`, replace both npm lockfiles in one commit, reinstall cleanly, and update every script and document. Do not keep npm and pnpm lockfiles side by side.

## 6. Environment notes

- Firebase CLI: globally installed and working as version `15.27.0`.
- Firebase executable on this machine: `C:\Users\wwwka\AppData\Roaming\npm\firebase.cmd`.
- Local Node.js: `v24.15.0`.
- Cloud Functions deployment runtime: Node.js 22.
- The local Functions emulator warns about the Node 24/22 mismatch but successfully discovered all functions after the discovery timeout was increased.
- Android Studio JBR: `C:\Program Files\Android\Android Studio\jbr`.
- Android SDK: `%LOCALAPPDATA%\Android\Sdk`.
- Installed compile/target SDK used by the project: API 36.
- Gradle/Android dependency caches are now populated on this machine.
- OneDrive was not established as the cause of the earlier install delays. The delays were first-time npm/Gradle downloads and dependency resolution.

For Android command-line work in a new PowerShell session:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
npm.cmd run android:check
```

## 7. Optional work after the MVP is stable

Do not put these ahead of the runtime, deployment, and release tasks:

- Restore Hilt if it is an explicit assessment requirement.
- Upload a real demonstration camera snapshot to Firebase Storage instead of using only the authored mock asset.
- Add richer charts to reports.
- Add drag-and-drop device positioning.
- Resolve non-blocking Android lint deprecations and update dependencies in a separate change.
- Remove any obsolete, unused setup-screen source after confirming no navigation references remain.

## 8. Useful documentation map

- `README.md` — project overview and root commands.
- `docs/architecture.md` — system architecture.
- `docs/local-development.md` — emulator and local-development workflow.
- `docs/firebase-setup.md` — real Firebase setup/deployment notes.
- `docs/demo-script.md` — demonstration recording sequence.
- `PRODUCT.md` — product intent and experience principles.
- `DESIGN.md` — Android visual and interaction system.
- `android-app/README.md` — Android-specific setup.

## 9. Definition of done

The project should be considered complete only when:

- The Pub/Sub-enabled emulator integration passes.
- The browser and Android clients pass the two-way real-time walkthrough.
- The automatic safety cutoff is verified end to end.
- Android has been inspected on phone, expanded/tablet, dark mode, large text, and TalkBack.
- A real Firebase project is configured and deployed with reviewed security rules.
- A signed release APK installs and works on a clean target.
- Secrets and build artifacts are excluded from Git.
- The demo recording is completed using the stable release configuration.

