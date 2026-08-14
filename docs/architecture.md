# Architecture

```text
Android app <---- Firestore ----> Next.js simulator
                     |
                     +---- Cloud Functions
                     |       safety cutoff
                     |       usage records
                     |       alerts / notifications
                     |
                     +---- Firebase Auth, Storage, FCM
```

Firestore is the source of truth. Both clients subscribe through snapshot listeners and issue targeted writes. Trusted automation runs through Cloud Functions with the Admin SDK.

## Boundaries

- `android-app` owns the household user experience.
- `simulator-web` represents the hardware-side state simulator.
- `functions` owns safety enforcement, history, aggregation, and notifications.
- `firestore.rules` and `storage.rules` define client authorization.

## Environments

- Local development uses the Firebase Emulator Suite and the project ID `demo-smart-home`.
- Shared development and production use explicit Firebase project aliases configured with `firebase use --add`.
- Real secrets and downloaded configuration files remain outside Git.

