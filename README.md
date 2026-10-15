# SafePath 🛡️

**A personal safety and emergency assistance Android app.**

SafePath brings together emergency tools, location sharing, safety maps, and trusted-contact features to help users respond more quickly when they feel unsafe.

> **Project status:** This repository contains the Android application source code. Review and test emergency workflows on your own device before relying on them in a real emergency.

## Features

- **SOS assistance** — trigger configurable emergency actions, such as sending an SOS message or placing a call to a saved emergency contact.
- **Location sharing** — share a map link based on the app's current saved location.
- **Safety map** — view map information, search for places, and explore nearby safety-zone information.
- **Emergency contacts** — manage contact details for emergency workflows.
- **“I am safe” update** — send a follow-up message to a saved emergency contact.
- **Quick safety tools** — access a flashlight, first-aid reference, and emergency calling shortcut.
- **Configurable SOS actions** — choose supported actions such as SMS, calling, audio recording, a siren, and flashlight signaling.
- **User profile and notifications** — includes profile, settings, and Firebase messaging components.

*Feature availability depends on device hardware, granted Android permissions, network connectivity, and the app's configuration.*

## Tech stack

- **Language:** Java
- **Platform:** Android
- **Build system:** Gradle (Kotlin DSL)
- **Maps and location:** Google Maps SDK, Google Play services Location, and OSMDroid dependencies
- **Backend services:** Firebase Authentication, Realtime Database, Cloud Messaging, and Storage dependencies
- **Local persistence:** Room
- **Networking and JSON:** Retrofit, Volley, and Gson
- **UI:** Android XML layouts, Material Components, View Binding

## Getting started

### Prerequisites

- Android Studio with Android SDK support for **API 35**
- JDK 17 (required by Android Gradle Plugin 8.10.1; Java source compatibility is set to 11)
- An Android device or emulator running **Android 7.0 (API 24)** or later
- Valid configuration for the Google Maps and Firebase services used by the app

### Setup

1. Clone this repository:

   ```bash
   git clone https://github.com/VedantAdsul/SafePath.git
   ```

2. Open the `SafePath` directory inside the cloned repository in Android Studio. The Android Gradle project is nested in this directory.
3. Configure your own Google Maps API key and Firebase Android configuration. Do not commit private credentials or unrestricted API keys.
4. Sync the project with Gradle.
5. Build and run the `app` configuration on a compatible device or emulator.

You can also build from the `SafePath` directory using the Gradle wrapper:

**Windows**
```powershell
.\gradlew.bat assembleDebug
```

**macOS / Linux**
```bash
./gradlew assembleDebug
```

## Permissions

Depending on which features you use, Android may request location, camera, microphone, phone-call, SMS, notification, and foreground-service permissions. Grant only the permissions you understand and need. Some features will not work if their required permissions are denied.

## Safety notes

- SafePath is a tool, **not a replacement for local emergency services**. In an immediate emergency, contact the appropriate local emergency number.
- Check that emergency contacts and permissions are configured before testing SOS features.
- Test SMS, calling, location sharing, recording, siren, and flashlight behavior in a safe environment before depending on them.
- Location accuracy and availability depend on the device, permission settings, and connectivity.
- Review the app's permission requests and data-handling behavior before installing it on a personal device.

## Configuration and credentials

This project integrates with Google Maps and Firebase. Supply your own project configuration during setup. Restrict API keys to the required Android app and APIs, and keep signing keys, secrets, and private configuration out of version control.

## Contributing

Suggestions, bug reports, and improvements are welcome. When reporting an issue, include the device or emulator version, Android version, steps to reproduce, and relevant logs—without including personal data or credentials.

## License

No license has been specified yet. Unless a license is added to this repository, the source code is not explicitly offered for reuse, modification, or redistribution.
