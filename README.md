# WorkMail Planner 💼🛡️

### Offline-First Android Work Timeline, Knowledge Graph, PDF Export, and GitHub Actions APK Build System

WorkMail Planner is a production-grade, enterprise-safe native Android application designed to help professionals extract, structure, and organize their work information (meetings, deadlines, follow-ups, and project tasks) entirely on-device. 

The app adheres to **strict absolute privacy rules** to conform with corporate policies: it requests **no internet permissions**, does **not connect to Outlook APIs or Microsoft Graph**, and processes shared data using a completely local heuristic parsing engine.

---

## 🌟 Core Architecture & Features

### 1. Zero-Trust Safe Email Intake 📨
- **Share Sheet Driven**: Does not scan background folders, notification streams, or read inbox folders.
- **Rule-Based Parser**: Uses regex and keyword proximity matching to structure meetings, tasks, deadlines, priorities, and project names from plain text.
- **Wiped Memory Buffer**: Once approved or rejected, the raw shared text is fully cleared from device memory.

### 2. High-Fidelity Day Vaults 🗄️
- **Manual Checklist & Notes**: Edit, update, and manage hand-written tasks per day.
- **Storage Access Framework (SAF)**: Safely links documents, spreadsheets, presentations, audios, and archives directly to any date without requesting intrusive `MANAGE_EXTERNAL_STORAGE` permissions.
- **Outlook Reference Verification**: Displays metadata snippets (Subject, sender, date) to assist users in manually locating records inside Outlook securely.

### 3. Local Work Knowledge Graph 🕸️
- **Hub-and-Spoke Radial Canvas**: An interactive, mathematically laid-out vector relationship graph drawn directly on standard Compose Canvas.
- **Dynamic Relations**: Automatically registers edges (`assigned_to`, `due_on`, `related_to`) as items are mutated.
- **Focused Sub-Graphs**: Click any project, date, or person node to zoom in on connected nodes (Project Graph, Person Graph, Date Graph).
- **Keyword Graph Search**: Type labels like "HR" or "onboarding" to instantly isolate matching elements.

### 4. Offline Canvas PDF Exporter 📄
- **On-Device Compilation**: Employs Android's built-in `PdfDocument` and Canvas layout engines to build report sheets with zero network dependencies.
- **Corporate Notice**: Enforces explicit compliance agreements before starting the document compilation.

### 5. Sandboxed Security Shield 🛡️
- **Hardware Android KeyStore**: Sensitive database text strings (titles, descriptions, note contents) are encrypted using AES/GCM/NoPadding before storage.
- **FLAG_SECURE Privacy Toggle**: Prevents the operating system from capturing screenshots, blocking screen recordings, and hiding the app's contents in the OS Recents window.
- **Diagnostic Signals**: Performs root compromise detection and warns if running a debug compilation.
- **Audit Trails**: Keeps a transparent offline ledger of all state alterations.
- **Secure Purge**: Provides a one-tap zero-fill overwrite mechanism to wipe trashed items and older revision histories completely.

---

## 🛠️ Build System & Instructions

The project is built using **Gradle Kotlin DSL (.gradle.kts)** and **Kotlin 2.2.10**, targetting **SDK 36**.

### 1. Build via GitHub Actions (Automated)
This repository includes a continuous integration workflow located in `.github/workflows/android-apk-build.yml`.
1. Push your changes to the `main` or `master` branch.
2. The GitHub runner will check out the codebase, cache dependencies, run unit tests, and execute `./gradlew assembleDebug`.
3. Locate the completed compilation in the **Actions** tab of your repository and download the ZIP file under **Artifacts** containing the `WorkMail-Planner-debug-apk`.

### 2. Build Locally (Command Line)
To compile the project on your development machine, ensure you have Java 17/21 installed.
Execute the standard compiler command:
```bash
./gradlew assembleDebug
```
The compiled APK will be output to:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 APK Installation Guide

Follow these steps to deploy the built APK package directly onto your Android device:

### Step 1: Enable Unknown Sources
Since the APK is compiled privately outside the Google Play Store, your device requires installation authorization:
1. Open **Settings** on your Android device.
2. Go to **Apps** -> **Special app access** -> **Install unknown apps** (or search for *Unknown Sources*).
3. Select your File Manager or Browser and toggle **Allow from this source** to ON.

### Step 2: Transfer and Launch the Installer
1. Transfer the compiled `app-debug.apk` onto your device via USB, email, or a local network drive.
2. Open your device's **Files** app and navigate to the folder where you saved the APK.
3. Tap on the file. A secure prompt will ask if you want to install **WorkMail Planner**.
4. Tap **Install**.

### Step 3: First-Run Security Check
1. Launch the application from your home screen.
2. Go to the **Security** tab:
   - Ensure the **Hardware KeyStore** diagnostic indicates active encryption.
   - Verify that your **Root Integrity** is secure.
   - If desired, toggle the **FLAG_SECURE Privacy Mode** switch to lock down screenshot and recording permissions for absolute organizational confidentiality.
