# Homely Android app

This is the native Kotlin and Jetpack Compose client. It includes Material 3, adaptive Navigation Compose, MVVM with StateFlow, an explicit repository factory, and Firebase Auth, Firestore, Storage, Messaging, and Functions SDKs.

## Open and sync

1. Install Android Studio with JDK 17 and Android SDK 36.
2. Open this `android-app` directory as the project.
3. Let Android Studio sync Gradle 9.4.1 and AGP 9.2.1.
4. Run the debug app on an Android emulator while the Firebase emulators are running on the host computer.

Debug builds connect to `10.0.2.2` for Firebase emulator networking.

## Implemented flows

- Realtime two-floor dashboard with positioned device markers and live room glow
- Outlet, light, independent multi-switch, safety outlet, and mock camera controls
- Server-authoritative safety duration with an explanatory local countdown
- Schedules, usage summaries and history, alert acknowledgement, pending writes, and errors
- Compact bottom navigation and expanded navigation rail
- TalkBack state descriptions, 48 dp device targets, dark appearance, system Back, and edge-to-edge insets

## Connect a real Firebase app

1. Register package `com.homely.smarthome` in Firebase.
2. Copy the downloaded `google-services.json` into `app/`.
3. Apply `alias(libs.plugins.google.services)` in `app/build.gradle.kts`.
4. Keep `google-services.json` out of Git.

Release builds require a real Firebase app configuration. Debug builds can run entirely against the local emulator suite with the demo constants already defined in Gradle.
