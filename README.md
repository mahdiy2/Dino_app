# SecMessage Android

Native Android client for SecMessage, built with Kotlin, Jetpack Compose, and a feature-based clean architecture.

## Tech Stack
- Kotlin 2.1.20 + Android Gradle Plugin 8.9.2
- Jetpack Compose (BOM 2025.06.01) + Material 3
- Hilt for dependency injection
- Ktor + OkHttp + kotlinx.serialization for API calls
- Room for local persistence
- WorkManager for background work
- Media3 ExoPlayer for audio/video playback
- Socket.IO client for realtime updates

## Project Layout
- `app/src/main/java/com/dino/message/`
  - `corefeature/` shared app core (network, utilities, base models)
  - `authfeature/` authentication flow
  - `threadfeature/` thread/post creation and timeline features
  - `chatfeature/` chat and messaging
  - `groupfeature/` group-related workflows
  - `profilefeature/` profile and account screens
  - `notificationfeature/` and `activitynotificationfeature/` notifications
  - Other feature folders follow the same `data/domain/presentation` pattern
- `app/src/main/res/` compose/theme/resources
- `tools/` local utility scripts (screenshots/tests/helpers)

## Requirements
- Android Studio (recent stable version)
- JDK 17
- Android SDK 36 (compile/target)
- Min SDK 28 device/emulator

## Configuration
The app reads key values from `gradle.properties`:

- `APP_BASE_URL` (default backend base URL)
- `AUTH_PASSWORD_REQUIRE_SYMBOL` (`true`/`false`)

Example:

```properties
APP_BASE_URL=https://api.dinosocial.ir/
AUTH_PASSWORD_REQUIRE_SYMBOL=false
```

For local backend from an emulator you can use:

```properties
APP_BASE_URL=http://10.0.2.2:5000/
```

## Build and Run
From this directory:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

## Tests

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

## Release
- Release artifact path: `app/release/app-release.apk`
- Build release APK:

```bash
./gradlew :app:assembleRelease
```
