"use client";

import {onAuthStateChanged, signInWithEmailAndPassword, type User} from "firebase/auth";
import {
  doc,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  updateDoc,
  type DocumentData,
  type QueryDocumentSnapshot,
  type Unsubscribe,
} from "firebase/firestore";
import {useCallback, useEffect, useMemo, useState} from "react";
import {getFirebaseClient} from "@/lib/firebase/client";
import {
  alertsCollection,
  devicesCollection,
  floorsCollection,
  schedulesCollection,
  usageRecordsCollection,
} from "@/lib/firebase/paths";
import type {
  Alert,
  Device,
  DeviceSwitch,
  DeviceType,
  Floor,
  Schedule,
  UsageRecord,
} from "@/model/firestore";

const HOME_ID = process.env.NEXT_PUBLIC_DEMO_HOME_ID ?? "demo-home";
const DEMO_EMAIL = "demo@homely.local";
const DEMO_PASSWORD = "homely-demo-123";

type View = "floor" | "activity";

function dataWithId<T>(snapshot: QueryDocumentSnapshot<DocumentData>): T {
  return {id: snapshot.id, ...snapshot.data()} as T;
}

function switchesFor(device: Device): DeviceSwitch[] {
  const value = device.capabilities.switches;
  if (!Array.isArray(value)) return [];
  return value.filter((item): item is DeviceSwitch => (
    typeof item === "object"
      && item !== null
      && typeof (item as DeviceSwitch).id === "string"
      && typeof (item as DeviceSwitch).label === "string"
      && typeof (item as DeviceSwitch).isOn === "boolean"
  ));
}

function booleanCapability(device: Device, key: string): boolean {
  return device.capabilities[key] === true;
}

function numberCapability(device: Device, key: string, fallback = 0): number {
  const value = device.capabilities[key];
  return typeof value === "number" ? value : fallback;
}

function timestampLabel(value: unknown): string {
  if (value && typeof value === "object" && "toDate" in value) {
    const toDate = (value as {toDate: () => Date}).toDate;
    return toDate.call(value).toLocaleString([], {dateStyle: "medium", timeStyle: "short"});
  }
  return "Waiting for first update";
}

function durationLabel(seconds: number | null): string {
  if (seconds === null) return "Active now";
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return remainder ? `${minutes}m ${remainder}s` : `${minutes}m`;
}

function DeviceGlyph({type}: {type: DeviceType}) {
  const path = {
    OUTLET: "M7 3v5m10-5v5M5 8h14v3a7 7 0 0 1-7 7 7 7 0 0 1-7-7V8Zm7 10v3",
    LIGHT: "M9 18h6m-5 3h4M8.5 14.5a6 6 0 1 1 7 0c-1 .8-1.5 1.8-1.5 3.5h-4c0-1.7-.5-2.7-1.5-3.5Z",
    MULTI_SWITCH: "M6 5h12M6 12h12M6 19h12M9 3v4m6 3v4m-5 3v4",
    SAFETY_OUTLET: "M12 3 4 6v5c0 5 3.4 8.3 8 10 4.6-1.7 8-5 8-10V6l-8-3Zm0 5v5m0 3v.1",
    CAMERA: "M4 7h4l2-2h4l2 2h4v12H4V7Zm8 3a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z",
  }[type];
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24">
      <path d={path} fill="none" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
    </svg>
  );
}

export function SimulatorDashboard() {
  const [user, setUser] = useState<User | null>(null);
  const [floors, setFloors] = useState<Floor[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [usage, setUsage] = useState<UsageRecord[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [activeFloorId, setActiveFloorId] = useState("");
  const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(null);
  const [view, setView] = useState<View>("floor");
  const [pendingIds, setPendingIds] = useState<Set<string>>(new Set());
  const [message, setMessage] = useState("Connecting to Firebase…");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const {auth} = getFirebaseClient();
    const unsubscribe = onAuthStateChanged(auth, async (nextUser) => {
      if (cancelled) return;
      if (nextUser) {
        setUser(nextUser);
        setMessage("Live");
        return;
      }
      try {
        setMessage("Signing in to the demo home…");
        const credential = await signInWithEmailAndPassword(auth, DEMO_EMAIL, DEMO_PASSWORD);
        if (!cancelled) setUser(credential.user);
      } catch (signInError) {
        if (!cancelled) {
          setError(signInError instanceof Error ? signInError.message : "Unable to sign in.");
          setMessage("Offline");
        }
      }
    });
    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, []);

  useEffect(() => {
    if (!user) return;
    const {db} = getFirebaseClient();
    const subscriptions: Unsubscribe[] = [];
    const handleError = (snapshotError: Error) => {
      setError(snapshotError.message);
      setMessage("Sync interrupted");
    };

    subscriptions.push(onSnapshot(
      query(floorsCollection(db, HOME_ID), orderBy("sortOrder", "asc")),
      (snapshot) => {
        const next = snapshot.docs.map((item) => dataWithId<Floor>(item));
        setFloors(next);
        setActiveFloorId((current) => current || next[0]?.id || "");
        setMessage("Live");
      },
      handleError,
    ));
    subscriptions.push(onSnapshot(
      devicesCollection(db, HOME_ID),
      (snapshot) => setDevices(snapshot.docs.map((item) => dataWithId<Device>(item))),
      handleError,
    ));
    subscriptions.push(onSnapshot(
      schedulesCollection(db, HOME_ID),
      (snapshot) => setSchedules(snapshot.docs.map((item) => dataWithId<Schedule>(item))),
      handleError,
    ));
    subscriptions.push(onSnapshot(
      query(usageRecordsCollection(db, HOME_ID), orderBy("startedAt", "desc")),
      (snapshot) => setUsage(snapshot.docs.map((item) => dataWithId<UsageRecord>(item))),
      handleError,
    ));
    subscriptions.push(onSnapshot(
      query(alertsCollection(db, HOME_ID), orderBy("createdAt", "desc")),
      (snapshot) => setAlerts(snapshot.docs.map((item) => dataWithId<Alert>(item))),
      handleError,
    ));
    return () => subscriptions.forEach((unsubscribe) => unsubscribe());
  }, [user]);

  const selectedDevice = devices.find((item) => item.id === selectedDeviceId) ?? null;
  const activeFloor = floors.find((item) => item.id === activeFloorId) ?? floors[0] ?? null;
  const floorDevices = useMemo(
    () => devices.filter((item) => item.floorId === activeFloor?.id),
    [activeFloor?.id, devices],
  );
  const unacknowledged = alerts.filter((item) => !item.acknowledged).length;

  const runWrite = useCallback(async (id: string, operation: () => Promise<void>) => {
    setPendingIds((current) => new Set(current).add(id));
    setError(null);
    try {
      await operation();
    } catch (writeError) {
      setError(writeError instanceof Error ? writeError.message : "The command was not accepted.");
    } finally {
      setPendingIds((current) => {
        const next = new Set(current);
        next.delete(id);
        return next;
      });
    }
  }, []);

  const updateDevice = useCallback((device: Device, fields: Record<string, unknown>) => {
    if (!user) return Promise.resolve();
    const {db} = getFirebaseClient();
    return runWrite(device.id, () => updateDoc(
      doc(devicesCollection(db, HOME_ID), device.id),
      {...fields, updatedAt: serverTimestamp(), updatedBy: user.uid},
    ));
  }, [runWrite, user]);

  const toggleDevice = (device: Device) => {
    if (device.type === "CAMERA" || device.status === "DISCONNECTED") return;
    const nextIsOn = device.status !== "ON";
    void updateDevice(device, {
      status: nextIsOn ? "ON" : "OFF",
      capabilities: {...device.capabilities, isOn: nextIsOn},
    });
  };

  const toggleSwitch = (device: Device, switchId: string) => {
    const switches = switchesFor(device).map((item) => (
      item.id === switchId ? {...item, isOn: !item.isOn} : item
    ));
    void updateDevice(device, {
      status: switches.some((item) => item.isOn) ? "ON" : "OFF",
      capabilities: {...device.capabilities, switches},
    });
  };

  const setSafetyDuration = (device: Device, minutes: number) => {
    void updateDevice(device, {
      capabilities: {...device.capabilities, maxOnDurationMinutes: minutes},
    });
  };

  const acknowledgeAlert = (alert: Alert) => {
    const {db} = getFirebaseClient();
    void runWrite(alert.id, () => updateDoc(
      doc(alertsCollection(db, HOME_ID), alert.id),
      {acknowledged: true},
    ));
  };

  return (
    <main className="simulator-shell">
      <header className="topbar">
        <div className="brand-lockup">
          <span className="brand-mark" aria-hidden="true">H</span>
          <div>
            <p className="eyebrow">Homely lab</p>
            <h1>Hardware simulator</h1>
          </div>
        </div>
        <div className="connection" data-state={error ? "error" : "live"}>
          <span aria-hidden="true" />
          {error ? "Needs attention" : message}
        </div>
      </header>

      <nav className="view-tabs" aria-label="Simulator views">
        <button aria-current={view === "floor" ? "page" : undefined} onClick={() => setView("floor")}>Floor controls</button>
        <button aria-current={view === "activity" ? "page" : undefined} onClick={() => setView("activity")}>
          Activity
          {unacknowledged > 0 && <span className="badge">{unacknowledged}</span>}
        </button>
      </nav>

      {error && (
        <div className="error-banner" role="alert">
          <strong>Firebase could not complete the last operation.</strong>
          <span>{error}</span>
          <button onClick={() => setError(null)}>Dismiss</button>
        </div>
      )}

      {view === "floor" ? (
        <div className="workspace">
          <section className="floor-workspace" aria-labelledby="floor-heading">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Realtime topology</p>
                <h2 id="floor-heading">{activeFloor?.name ?? "Loading home"}</h2>
              </div>
              <div className="floor-picker" role="group" aria-label="Choose floor">
                {floors.map((floor) => (
                  <button
                    aria-pressed={floor.id === activeFloor?.id}
                    key={floor.id}
                    onClick={() => {
                      setActiveFloorId(floor.id);
                      setSelectedDeviceId(null);
                    }}
                  >
                    {floor.name}
                  </button>
                ))}
              </div>
            </div>

            {activeFloor && (
              <div
                className="floor-plan"
                style={{
                  "--columns": activeFloor.gridColumns,
                  "--rows": activeFloor.gridRows,
                  backgroundImage: `linear-gradient(rgba(35,48,55,.08) 1px, transparent 1px), linear-gradient(90deg, rgba(35,48,55,.08) 1px, transparent 1px), url(${activeFloor.imageUrl})`,
                } as React.CSSProperties}
              >
                {floorDevices.map((device) => (
                  <button
                    className="device-node"
                    data-selected={selectedDeviceId === device.id}
                    data-status={device.status.toLowerCase()}
                    key={device.id}
                    onClick={() => setSelectedDeviceId(device.id)}
                    style={{
                      left: `${((device.gridX - 0.5) / activeFloor.gridColumns) * 100}%`,
                      top: `${((device.gridY - 0.5) / activeFloor.gridRows) * 100}%`,
                    }}
                    title={`${device.name}: ${device.status}`}
                  >
                    <DeviceGlyph type={device.type} />
                    <span>{device.name}</span>
                  </button>
                ))}
              </div>
            )}
          </section>

          <aside className="device-panel" aria-live="polite">
            {selectedDevice ? (
              <DevicePanel
                device={selectedDevice}
                pending={pendingIds.has(selectedDevice.id)}
                schedules={schedules.filter((item) => item.deviceId === selectedDevice.id)}
                onToggle={() => toggleDevice(selectedDevice)}
                onSwitch={(switchId) => toggleSwitch(selectedDevice, switchId)}
                onSafetyDuration={(minutes) => setSafetyDuration(selectedDevice, minutes)}
              />
            ) : (
              <div className="panel-empty">
                <span className="empty-orbit" aria-hidden="true"><i /></span>
                <h2>Select a device</h2>
                <p>Choose a marker on the floor plan to inspect telemetry and send a command.</p>
              </div>
            )}
          </aside>
        </div>
      ) : (
        <ActivityView
          alerts={alerts}
          devices={devices}
          pendingIds={pendingIds}
          usage={usage}
          onAcknowledge={acknowledgeAlert}
        />
      )}
    </main>
  );
}

function DevicePanel({
  device,
  pending,
  schedules,
  onToggle,
  onSwitch,
  onSafetyDuration,
}: {
  device: Device;
  pending: boolean;
  schedules: Schedule[];
  onToggle: () => void;
  onSwitch: (id: string) => void;
  onSafetyDuration: (minutes: number) => void;
}) {
  const switches = switchesFor(device);
  const snapshotUrl = typeof device.capabilities.snapshotUrl === "string"
    ? device.capabilities.snapshotUrl
    : "/camera/entry-camera.svg";
  const maxMinutes = numberCapability(device, "maxOnDurationMinutes", 2);

  return (
    <div className="panel-content">
      <div className="device-hero">
        <span className="large-device-glyph"><DeviceGlyph type={device.type} /></span>
        <div>
          <p className="eyebrow">{device.type.replaceAll("_", " ")}</p>
          <h2>{device.name}</h2>
        </div>
        <span className="status-pill" data-status={device.status.toLowerCase()}>{device.status}</span>
      </div>

      {device.type === "CAMERA" ? (
        <figure className="camera-frame">
          {/* The simulator deliberately displays the Firestore-provided mock snapshot. */}
          <img alt="Mock view from the entry camera" src={snapshotUrl} />
          <figcaption>Snapshot refreshed {timestampLabel(device.capabilities.lastSnapshotAt)}</figcaption>
        </figure>
      ) : device.type === "MULTI_SWITCH" ? (
        <div className="switch-list">
          {switches.map((item) => (
            <label key={item.id}>
              <span>{item.label}</span>
              <input
                checked={item.isOn}
                disabled={pending}
                onChange={() => onSwitch(item.id)}
                type="checkbox"
              />
            </label>
          ))}
        </div>
      ) : (
        <button
          className="power-control"
          data-on={booleanCapability(device, "isOn") || device.status === "ON"}
          disabled={pending || device.status === "DISCONNECTED"}
          onClick={onToggle}
        >
          <span>{pending ? "Sending command…" : device.status === "ON" ? "Turn off" : "Turn on"}</span>
          <i aria-hidden="true" />
        </button>
      )}

      {device.type === "SAFETY_OUTLET" && (
        <div className="safety-card">
          <div>
            <strong>Automatic cutoff</strong>
            <span>Server-enforced while the outlet is on</span>
          </div>
          <label>
            Maximum duration
            <select value={maxMinutes} onChange={(event) => onSafetyDuration(Number(event.target.value))}>
              {[1, 2, 5, 10, 15, 30].map((value) => <option key={value} value={value}>{value} min</option>)}
            </select>
          </label>
        </div>
      )}

      <dl className="telemetry">
        <div><dt>Last update</dt><dd>{timestampLabel(device.updatedAt)}</dd></div>
        <div><dt>Updated by</dt><dd>{device.updatedBy || "Unknown"}</dd></div>
        <div><dt>Automation</dt><dd>{schedules.some((item) => item.enabled) ? "Enabled" : "No active schedule"}</dd></div>
      </dl>
    </div>
  );
}

function ActivityView({
  alerts,
  devices,
  pendingIds,
  usage,
  onAcknowledge,
}: {
  alerts: Alert[];
  devices: Device[];
  pendingIds: Set<string>;
  usage: UsageRecord[];
  onAcknowledge: (alert: Alert) => void;
}) {
  const deviceName = (id: string) => devices.find((item) => item.id === id)?.name ?? id;
  const totals = usage.reduce<Record<string, number>>((result, item) => {
    result[item.deviceId] = (result[item.deviceId] ?? 0) + (item.durationSeconds ?? 0);
    return result;
  }, {});
  const maximum = Math.max(1, ...Object.values(totals));

  return (
    <div className="activity-grid">
      <section className="activity-card" aria-labelledby="usage-heading">
        <div className="section-heading">
          <div><p className="eyebrow">Usage telemetry</p><h2 id="usage-heading">Active time</h2></div>
        </div>
        <div className="usage-bars">
          {Object.entries(totals).map(([deviceId, seconds]) => (
            <div className="usage-row" key={deviceId}>
              <div><strong>{deviceName(deviceId)}</strong><span>{durationLabel(seconds)}</span></div>
              <span className="bar"><i style={{width: `${Math.max(6, (seconds / maximum) * 100)}%`}} /></span>
            </div>
          ))}
          {Object.keys(totals).length === 0 && <p className="muted">Usage records will appear after a device completes a session.</p>}
        </div>
        <div className="recent-list">
          {usage.slice(0, 6).map((record) => (
            <article key={record.id}>
              <span className="event-dot" data-reason={record.endedReason?.toLowerCase()} />
              <div><strong>{deviceName(record.deviceId)}</strong><span>{record.endedReason?.replaceAll("_", " ") ?? "IN PROGRESS"}</span></div>
              <time>{durationLabel(record.durationSeconds)}</time>
            </article>
          ))}
        </div>
      </section>

      <section className="activity-card" aria-labelledby="alerts-heading">
        <div className="section-heading">
          <div><p className="eyebrow">Safety stream</p><h2 id="alerts-heading">Alerts</h2></div>
        </div>
        <div className="alert-list">
          {alerts.map((alert) => (
            <article data-acknowledged={alert.acknowledged} key={alert.id}>
              <span className="alert-symbol" aria-hidden="true">!</span>
              <div>
                <strong>{alert.type === "SAFETY_CUTOFF" ? "Safety cutoff" : "Device error"}</strong>
                <p>{alert.message}</p>
                <time>{timestampLabel(alert.createdAt)}</time>
              </div>
              {!alert.acknowledged && (
                <button disabled={pendingIds.has(alert.id)} onClick={() => onAcknowledge(alert)}>
                  {pendingIds.has(alert.id) ? "Saving…" : "Acknowledge"}
                </button>
              )}
            </article>
          ))}
          {alerts.length === 0 && <p className="muted">No alerts. Safety events will appear here in realtime.</p>}
        </div>
      </section>
    </div>
  );
}
