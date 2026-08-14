# Firestore data model

The MVP uses one demo home while retaining a multi-home document structure.

```text
homes/{homeId}
homes/{homeId}/floors/{floorId}
homes/{homeId}/devices/{deviceId}
homes/{homeId}/schedules/{scheduleId}
homes/{homeId}/usageRecords/{recordId}
homes/{homeId}/alerts/{alertId}
homes/{homeId}/fcmTokens/{tokenId}
```

## Shared enums

- Device type: `OUTLET`, `MULTI_SWITCH`, `LIGHT`, `SAFETY_OUTLET`, `CAMERA`
- Device status: `ON`, `OFF`, `ERROR`, `DISCONNECTED`
- Usage end reason: `USER`, `SCHEDULE`, `SAFETY_CUTOFF`
- Alert type: `SAFETY_CUTOFF`, `DEVICE_ERROR`

## Write ownership

- Clients may update only device intent fields: `status`, `capabilities`, `updatedAt`, and `updatedBy`.
- Cloud Functions exclusively write usage records and create alerts.
- Home owners manage floors and device definitions.
- Home members can manage schedules and acknowledge alerts.

