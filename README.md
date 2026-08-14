# Homely Smart Home

Homely is a university smart-home monitoring and control system consisting of a native Kotlin Android app, a Next.js hardware simulator, and Firebase Cloud Functions.

## Repository

```text
android-app/      Native Kotlin and Jetpack Compose app
simulator-web/    Next.js TypeScript hardware simulator
functions/        Firebase Cloud Functions and emulator seed script
docs/             Architecture, setup, and demo documentation
```

## Prerequisites

- Node.js 22 recommended; Firebase deploys Functions on the explicitly configured Node 22 runtime
- Android Studio with its bundled JDK (17 or newer) and Android SDK 36
- Firebase CLI
- A Firebase project for shared/cloud testing

## Local setup

1. Run `npm.cmd --prefix functions ci` from the repository root. This installs only the Functions workspace.
2. Run `npm.cmd --prefix simulator-web ci` from the repository root. This installs only the simulator workspace.
3. Copy `simulator-web/.env.example` to `simulator-web/.env.local`.
4. Run `npm.cmd run emulators:fresh`.
5. In a second terminal, run `npm.cmd run seed`.
6. In a third terminal, run `npm.cmd run dev:simulator`.
7. Open the Firebase Emulator UI at `http://127.0.0.1:4000` and the simulator at `http://127.0.0.1:3000`.

The first Android Gradle sync downloads the Kotlin compiler, Android Gradle Plugin, Compose, and Firebase Android libraries. This is independent of the Firebase CLI and can take several minutes on the first run; later builds use the Gradle cache.

Run `npm.cmd run check`, `npm.cmd test`, and `npm.cmd run build` for the TypeScript workspaces. Run `npm.cmd run android:check` from a terminal where `JAVA_HOME` and the Android SDK are configured, or use the equivalent Gradle tasks from Android Studio.

Demo credentials are documented in `docs/local-development.md` and are safe only for the local emulator.

## Real Firebase project

External Firebase resources are intentionally not created automatically. Follow `docs/firebase-setup.md` when the team is ready to connect a real project. Do not commit `.env.local`, `google-services.json`, service-account files, or signing keys.
