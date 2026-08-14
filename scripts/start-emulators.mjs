import {spawn} from "node:child_process";

const firebaseCommand = process.platform === "win32" ? "firebase.cmd" : "firebase";
const child = spawn(
  firebaseCommand,
  ["emulators:start", ...process.argv.slice(2)],
  {
    env: {
      ...process.env,
      FUNCTIONS_DISCOVERY_TIMEOUT: process.env.FUNCTIONS_DISCOVERY_TIMEOUT ?? "60",
    },
    shell: process.platform === "win32",
    stdio: "inherit",
  },
);

child.on("error", (error) => {
  console.error(`Unable to start Firebase emulators: ${error.message}`);
  process.exitCode = 1;
});

child.on("exit", (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
    return;
  }
  process.exitCode = code ?? 1;
});
