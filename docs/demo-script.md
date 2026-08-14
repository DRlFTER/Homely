# Demo script

## Before recording

1. Start the Firebase emulators with `npm.cmd run emulators:fresh`.
2. Seed the deterministic demo data with `npm.cmd run seed`.
3. Start the simulator with `npm.cmd run dev:simulator` and open `http://127.0.0.1:3000`.
4. Start the Android debug app on an emulator; it signs in automatically as the local demo user.
5. Keep the Emulator UI (`http://127.0.0.1:4000`) available as optional evidence, but demonstrate normal behavior through the two clients.

## Recording sequence

1. On Android, switch between Ground floor and First floor. Point out that device markers keep their physical grid positions and that ON devices create a warm room glow.
2. Open a normal outlet or light and toggle it. Show the pending indicator, then show the matching state update in the web simulator without refreshing.
3. In the simulator, change a different device. Show the Android marker and detail sheet updating from the Firestore snapshot.
4. Open the multi-switch device and toggle each gang independently. Confirm the other switch retains its state.
5. Open the Entry camera and show the authored static snapshot, freshness value, and explicit mock stream label.
6. Open Schedules, select a device, choose days and start/end times, save, and show the confirmation. Explain that the scheduled Function evaluates the home's `Asia/Colombo` time zone.
7. Open the Iron safety outlet, set the cutoff to one or two minutes, and turn it on. Point out that the visible countdown is explanatory and the backend timer remains authoritative.
8. Wait for the scheduled worker. Show the iron turning OFF in both clients, then open Alerts and acknowledge the new safety-cutoff alert.
9. Open Reports and show the completed iron usage session with `SAFETY_CUTOFF`, plus total active time by device.

## Recovery notes

- If seeded data is changed beyond recognition, stop the suite, restart with `npm.cmd run emulators:fresh`, and run `npm.cmd run seed` again.
- If Android cannot connect, confirm the emulators are running and that the debug build is using `10.0.2.2`, not `127.0.0.1`.
- Push notifications are optional in local debug; the Firestore alert is the durable, demonstrable source of truth.
