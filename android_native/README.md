# Android Native (Kotlin + Jetpack Compose)

This folder contains a **new Android-native app** built in Kotlin + Jetpack Compose.

**Important:** The existing Flutter codebase is not modified. This native app lives in its own folder: `android_native/`.

## Open in Android Studio

- Open Android Studio → **Open** → select the `android_native/` folder.
- Let Gradle sync and download dependencies.

## Run

- Select the `app` configuration and run on an emulator/device.

### API base URL

This project uses a build-time base URL:
- **Debug**: `http://10.0.2.2/MockTest/api/` (Android emulator → your local XAMPP)
- **Release**: `https://sudartnpscapp.in/api/`

If you want debug to hit production, change `BuildConfig.BASE_URL` in `android_native/app/build.gradle.kts`.

## Feature parity tracking

See `PARITY_SPEC.md`.

