# Local development

## Demo identity

- Email: `demo@homely.local`
- Password: `homely-demo-123`
- UID: `demo-user`
- Home ID: `demo-home`

These credentials are created only in the local Auth emulator by `npm run seed`.

## Recommended startup order

1. `npm.cmd --prefix functions ci`
2. `npm.cmd --prefix simulator-web ci`
3. `npm.cmd run emulators:fresh`
4. `npm.cmd run seed`
5. `npm.cmd run dev:simulator`

Android emulator networking uses `10.0.2.2` to reach services on the development computer. The web simulator uses its current browser hostname for Firebase emulators, so opening it at `http://192.168.1.8:3000` also works from a phone on the same LAN. The Auth, Firestore, and Storage emulators are bound to `0.0.0.0` for this device-testing workflow. Set `NEXT_PUBLIC_FIREBASE_EMULATOR_HOST` in `.env.local` when the emulator host needs to be different from the page hostname.

On slower Windows machines, set `FUNCTIONS_DISCOVERY_TIMEOUT=60` before starting the suite. Firebase documents this environment variable as the supported fallback when CLI function discovery exceeds its default timeout. The local Pub/Sub emulator is included so scheduled Functions are registered during integration runs.

## Checks

- `npm.cmd run check` checks TypeScript.
- `npm.cmd test` runs the Cloud Functions domain tests.
- `npm.cmd run build` builds Functions and the statically exportable simulator.
- `npm.cmd run android:check` runs Android unit tests and lint when `JAVA_HOME` and the Android SDK are configured.
- Open `android-app` in Android Studio to sync, build, and run the Android app.

## First Android sync

Android uses its own Gradle/Maven dependency graph. Installing `firebase-tools` globally only provides the Firebase command-line program; it does not install the Firebase Android SDK, Compose, the Kotlin compiler, or the Android Gradle Plugin. The first Gradle sync therefore performs a separate one-time download. Keep Android Studio's bundled JDK selected and let the sync finish; later builds reuse `%USERPROFILE%\.gradle`.
