# SWIFT COCKPIT — Android source project

A native Kotlin + Jetpack Compose landscape dashboard MVP for a 10-inch Maruti Suzuki Swift head unit.

## Current MVP features
- Full-screen 10-inch landscape cockpit UI.
- Dark graphite / carbon-inspired background, neon orange highlights and custom speed gauge.
- GPS speed in km/h using Android location services (no OBD2 adapter required).
- GPS permission and signal state.
- Clock and date.
- Navigation handoff to Google Maps (or installed map app).
- Music app handoff.
- Android home/app access and system settings shortcut.
- No fabricated RPM, fuel, coolant or engine values.

## Requirements
- Android Studio Ladybug or newer recommended.
- JDK 17.
- Android SDK Platform 35.
- Gradle 8.9+ (Android Studio can download/configure Gradle).

## Build APK
1. Extract the ZIP.
2. Open the `SWIFT_COCKPIT` folder in Android Studio.
3. Allow Gradle sync and SDK installation.
4. Connect the head unit by USB or copy the APK to it.
5. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. Install the generated APK from `app/build/outputs/apk/debug/app-debug.apk`.

Command-line build after Gradle is installed:
`gradle assembleDebug`

## First-run test
1. Install and launch in landscape mode.
2. Grant precise location permission.
3. Enable device location and wait for a GPS fix outdoors.
4. Compare the displayed speed with a trusted GPS speed app. GPS speed can lag briefly and may be unavailable indoors.
5. Tap NAVIGATE, MEDIA, QUICK ACCESS and SYSTEM SETTINGS to verify app handoffs.

## Known scope / next iteration
- This is source code for the first testable MVP, not a precompiled APK.
- The central map area is a designed preview panel; the NAVIGATE action hands off to Google Maps. Embedded live maps can be added in a later iteration with a Google Maps API key or another map SDK.
- Bluetooth ELM327 OBD2 support is intentionally not included in this first MVP. It should be implemented as an optional module and only show values actually returned by the ECU.
- No vehicle data is fabricated.
- Test GPS speed accuracy and layout on the actual head unit before road use. Do not interact with the screen while driving.
