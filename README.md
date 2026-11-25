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
- `androidApp/src/main/java/com/dino/message/`
  - `corefeature/` shared app core (network, utilities, base models)
  - `authfeature/` authentication flow
  - `threadfeature/` thread/post creation and timeline features
  - `chatfeature/` chat and messaging
  - `groupfeature/` group-related workflows
  - `profilefeature/` profile and account screens
  - `notificationfeature/` and `activitynotificationfeature/` notifications
  - Other feature folders follow the same `data/domain/presentation` pattern
- `androidApp/src/main/res/` compose/theme/resources
- `tools/` local utility scripts (screenshots/tests/helpers)

## Requirements
- Android Studio (recent stable version)
- JDK 17
- Android SDK 36 (compile/target)
- Min SDK 28 device/emulator

## Configuration
Keep machine-specific values in `local.properties` (gitignored), not in committed files.

1. Copy the template:

```bash
cp local.properties.example local.properties
```

2. Set your values in `local.properties`:

```properties
APP_BASE_URL=http://10.0.2.2:5000/
AUTH_PASSWORD_REQUIRE_SYMBOL=false
```

The build resolves values in this order:
1. Gradle property (`-PAPP_BASE_URL=...`)
2. Environment variable (`APP_BASE_URL`)
3. `local.properties`
4. Fallback default (`http://10.0.2.2:5000/`)

## Build and Run
From this directory:

```bash
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
```

## Run Linux and web in Android Studio

Sync Gradle, select **Run Linux Desktop** or **Run Web** from the
run configuration dropdown, and press **Run**. The web configuration starts
a local development server. Chrome can open the URL printed in the Run panel.

Both launchers open SecMessage's shared sign-in and account shell. Use an
existing backend account or register and save the generated private-key
backup. The Android app still provides the full feature set.

In the account screen, import a matching Android-format key backup or confirm
a replacement if the old key is lost. Replacing the key can make older
messages unreadable. Chrome keeps encrypted account keys in an IndexedDB
vault. Save the vault passphrase separately from the account password and
enter it after reload or switching accounts. Signing out locks the vault and
leaves its encrypted backup on the device. To resume pending registration
after restart, enter the same username, password, and vault passphrase.

Linux stores its session and account keys in an encrypted file with an
owner-only local master key. The development host exposes Secret Service
over D-Bus. This implementation still uses the encrypted-file adapter.

Linux uses `APP_BASE_URL` from the Run configuration environment, or
`http://127.0.0.1:5000/` by default. The web development server proxies the session, key, and registration
endpoints to port 5000 on loopback. For local Chrome sign-in,
start the backend with `WEB_SESSION_ORIGIN=http://127.0.0.1:8080` and
`WEB_SESSION_LOCAL_HTTP=1`, then open that exact URL. Use the printed port
if 8080 is occupied and set the backend origin to match.

For production, serve the Wasm distribution and `/api/auth/web-session`
through one HTTPS origin. Set `WEB_SESSION_ORIGIN` to that exact origin
and leave `WEB_SESSION_LOCAL_HTTP` unset. The backend creates the
`web_sessions` table via its normal database startup. Browser session
cookies expire after 24 hours. Signing out revokes the server session.

For web, install Node.js and Yarn Classic and ensure Android Studio can find
them on its PATH. If Android Studio was launched without your shell PATH,
set `web.node` and `web.yarn` in `local.properties` to their absolute
executable paths. Select **Test Web in Chrome** to run the browser suite,
or **Test Linux Desktop** to run the JVM suite.

The equivalent launch commands from the project root are:

```bash
./gradlew :composeApp:run
./gradlew :composeApp:wasmJsBrowserDevelopmentRun -Pkotlin.daemon.jvmargs=-Xmx4500m
```

## Tests

```bash
./gradlew :androidApp:testDebugUnitTest
./gradlew :androidApp:connectedDebugAndroidTest
```

## Release
- Gradle writes release APKs to `androidApp/build/outputs/apk/release/`.
- You can get releases from: https://api.dinosocial.ir/
- Build release APK:

```bash
./gradlew :androidApp:assembleRelease
```

## App Screenshots
![Feed Screen](screen/processed/36f47b2d-bedc-42bd-b43f-31ff358eb04c.png)
![Create Post Screen](screen/processed/b61e1201-3538-41c7-9cab-58af7e33ee7f.png)
![Profile Screen](screen/processed/b9060059-1eb5-4eba-b2cd-0393c261b0d7.png)

## License
This project is licensed under the GNU Affero General Public License v3.0 (AGPLv3).
See the LICENSE file for details.
