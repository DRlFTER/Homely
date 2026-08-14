import {access, readFile, stat} from "node:fs/promises";
import {join} from "node:path";

const root = process.cwd();

const requiredFiles = [
  "firebase.json",
  "firestore.rules",
  "storage.rules",
  "functions/package.json",
  "functions/src/index.ts",
  "functions/src/seed.ts",
  "simulator-web/package.json",
  "simulator-web/src/lib/firebase/client.ts",
  "android-app/settings.gradle.kts",
  "android-app/app/build.gradle.kts",
  "android-app/app/src/main/AndroidManifest.xml",
  "android-app/gradle/wrapper/gradle-wrapper.jar",
  "PRODUCT.md",
  "DESIGN.md",
];

const jsonFiles = [
  "package.json",
  "firebase.json",
  "firestore.indexes.json",
  "functions/package.json",
  "functions/tsconfig.json",
  "simulator-web/package.json",
  "simulator-web/tsconfig.json",
];

const failures = [];

for (const relativePath of requiredFiles) {
  try {
    await access(join(root, relativePath));
  } catch {
    failures.push(`Missing required file: ${relativePath}`);
  }
}

for (const relativePath of jsonFiles) {
  try {
    JSON.parse(await readFile(join(root, relativePath), "utf8"));
  } catch (error) {
    failures.push(`Invalid JSON in ${relativePath}: ${error.message}`);
  }
}

try {
  const wrapper = await stat(join(root, "android-app/gradle/wrapper/gradle-wrapper.jar"));
  if (wrapper.size < 10_000) {
    failures.push("Gradle wrapper JAR is unexpectedly small.");
  }
} catch {
  // Missing wrapper is already reported above.
}

if (failures.length > 0) {
  console.error("Setup check failed:\n" + failures.map((failure) => `- ${failure}`).join("\n"));
  process.exitCode = 1;
} else {
  console.log("Setup check passed: the Firebase, web, Functions, Android, product, and design files are present.");
}
