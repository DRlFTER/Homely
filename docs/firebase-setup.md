# Firebase cloud setup

The local emulator requires no cloud project. To connect a shared Firebase project:

1. Create a Firebase project in the Firebase Console.
2. Enable Email/Password authentication.
3. Create a Firestore database and a default Storage bucket.
4. Register a web app and copy its public configuration into `simulator-web/.env.local`.
5. Register Android package `com.homely.smarthome` and place the downloaded `google-services.json` in `android-app/app/`.
6. Add the real project alias with `firebase use --add`.
7. Review rules and indexes before running any deploy command.

Cloud Functions scheduling and external network access may require the Firebase Blaze plan. No deploy command should be run until the project ID and billing expectations are confirmed.
