# Kotlin Multiplatform / Compose Multiplatform Migration Checklist

This document lists the non-DI work required to migrate the current native
Android app to Kotlin Multiplatform (KMP) with Compose Multiplatform (CMP).
It is based on the current single-module Android project under `:app`.

DI has already been handled separately in `docs/di-migration.md`; this checklist
focuses on every other dependency, Android API, and architecture boundary that
must be changed before code can move safely into shared source sets.

## Current Project Shape

- The project is currently a single Android application module: `:app`.
- The app source lives under `app/src/main/java/com/dino/message`.
- Gradle uses Android-only plugins: `com.android.application`,
  `org.jetbrains.kotlin.android`, and `kotlin-parcelize`.
- UI is Jetpack Compose for Android, not Compose Multiplatform.
- Local persistence uses Room, AndroidX DataStore, and
  `EncryptedSharedPreferences`.
- Networking uses Ktor, but with Android/JVM-specific engine and upload
  stream APIs.
- Realtime messaging uses the Java Socket.IO client.
- Media recording/playback/upload is deeply tied to Android platform APIs.

## Dependencies That Need Replacement Or Source-Set Separation

### Build / Project Structure

- Replace the single Android-only module structure with a KMP/CMP layout.
  Recommended first structure:
  - `:app` or `:androidApp`: Android app shell.
  - `:shared` or `:composeApp`: KMP shared module.
  - Optional later feature modules if the shared module becomes too large.
- Replace `org.jetbrains.kotlin.android` in shared code with
  `org.jetbrains.kotlin.multiplatform`.
- Add Compose Multiplatform Gradle plugin/dependencies for shared UI.
- Keep Android application ID, manifest, services, permissions, ProGuard, and
  signing only in the Android app target.
- Keep `BuildConfig` access Android-specific, or replace with a shared config
  abstraction supplied by each platform.

### Compose / UI ✅ Done for bridge milestone

Current dependencies:

- `androidx.compose:*`
- `androidx.activity:activity-compose`
- `androidx.lifecycle:lifecycle-runtime-ktx`
- `androidx.compose.material:material`
- `androidx.compose.material3:material3`
- `androidx.compose.foundation:foundation`
- `androidx.compose.ui:ui-tooling-preview`
- `androidx.compose.ui:ui-tooling`
- `androidx.compose.ui:ui-test-junit4`
- `androidx.compose.ui:ui-test-manifest`

Migration actions:

- [x] Replace AndroidX Compose BOM usage with Compose Multiplatform dependencies.
- [x] Move `ComponentActivity`, `setContent`, `enableEdgeToEdge`, permission
  launchers, and Android lifecycle code into `androidMain`.
- [x] Keep pure composables only if they do not depend on Android-only resources,
  `LocalContext`, activity result APIs, Android views, Android drawables, or
  platform media classes.
- [x] Replace Android-only previews/tooling dependencies with target-appropriate
  CMP preview/tooling setup.

Bridge completion notes:

- `:app` now applies the Compose Multiplatform Gradle plugin and uses CMP
  runtime/foundation/material/resource/preview dependencies for Compose UI,
  while Android activity, lifecycle, Material3 Expressive, tooling, and UI-test
  artifacts remain Android-only.
- Theme code is split so pure color/theme composition lives behind
  `SecMessageThemeContent`; Android dynamic colors and system bar side effects
  stay in the Android shell.
- Platform UI effects and image loading are behind presentation wrappers so
  shared composables have a clear migration seam instead of directly depending
  on Android browser intents, clipboard/toast APIs, `LocalContext`, or
  `ImageRequest.Builder(context)`.
- Full common source-set movement is intentionally deferred until Navigation,
  Resources, Images, and Android UI Utility Libraries are migrated.

### Navigation ✅ Done for bridge milestone

Current dependencies:

- Compose Multiplatform Navigation via
  `org.jetbrains.androidx.navigation:navigation-compose`.

Migration actions:

- [x] Replace Compose Destinations generated routes and host with Compose
  Multiplatform Navigation typed routes.
- [x] Remove Compose Destinations annotations, generated destination imports,
  `DestinationsNavHost`, `Direction`, and generated destination objects from app
  source.
- [x] Replace ViewModel navigation events with platform-independent `AppRoute`
  commands.
- [x] Preserve bottom-sheet and dialog destinations with CMP navigation wrapper
  routes.

Bridge completion notes:

- `AppRoute` now defines serializable, typed navigation commands for existing
  screens and arguments.
- `AppNavGraph` owns CMP `NavHost` registration and manually maps route args to
  composables.
- Notification deep links now navigate directly to typed chat, group chat, and
  activity notification routes without Compose Destinations generated classes.

### Koin

Current dependencies:

- `koin-android`
- `koin-androidx-compose`
- `koin-compose`
- `koin-compose-viewmodel`
- `koin-compose-viewmodel-navigation`
- `koin-test`

Migration actions:

- Keep Koin, but split artifacts by source set.
- Use `koin-core` in `commonMain`.
- Use KMP-compatible `koin-compose` / `koin-compose-viewmodel` where shared UI
  or shared ViewModels are used.
- Keep `koin-android`, `androidContext()`, `androidApplication()`, Android
  Workers, and Android ViewModel integration only in `androidMain`.
- Avoid Koin APIs that depend on Android framework types inside common modules.

### Networking / HTTP ✅ Done for bridge milestone

Current dependencies:

- `ktor-client-core`
- `ktor-client-okhttp`
- `ktor-client-content-negotiation`
- `ktor-serialization-kotlinx-json`
- `ktor-client-serialization`
- `ktor-client-logging`

Migration actions:

- [x] Keep Ktor client core, content negotiation, logging, and
  kotlinx.serialization in shared code.
- [x] Move `ktor-client-okhttp` to `androidMain`.
- [x] Add platform-specific engines:
  - Android: OkHttp or Android engine.
  - iOS/macOS: Darwin engine.
  - Desktop/JVM: CIO or Java engine.
  - Web/Wasm: supported Ktor web engine choice.
- [x] Replace JVM stream upload usage such as `io.ktor.utils.io.streams.asInput()`
  with a multiplatform upload abstraction.
- [x] Move `java.net.SocketTimeoutException`,
  `java.nio.channels.UnresolvedAddressException`, and other JVM exceptions out
  of common error handling.
- [x] Replace `logback-classic` with a multiplatform logger such as Kermit or Napier,
  or wrap logging behind a common interface.

Bridge completion notes:

- HTTP client setup now lives in a reusable Ktor factory that accepts the base
  URL, debug flag, engine factory, and logger; Android DI supplies the OkHttp
  engine for the current app target.
- Multipart uploads now go through upload part helpers, while Android URI and
  stream opening details are contained in an Android upload content provider.
- Retry/error handling now delegates timeout and network exception detection to
  a classifier; JVM-only exceptions are isolated in the Android/JVM classifier.
- Ktor logging now uses an app logger interface backed by Android `Log`, so the
  JVM-only `logback-classic` dependency was removed.

### Realtime Socket ✅ Done for bridge milestone

Current dependency:

- `io.socket:socket.io-client`

Migration actions:

- [x] Replace Java Socket.IO client with a KMP-compatible transport seam.
- [x] Recommended path: abstract current socket manager behind a shared realtime
  gateway, then implement Android with the existing client initially.
- Long-term options:
  - Ktor WebSockets if the server can support a compatible websocket protocol.
  - Platform-specific Socket.IO actuals if Socket.IO protocol compatibility must
    remain exact.
- [x] Socket queue persistence must be decoupled from Room before it can be shared.

Bridge completion notes:

- Realtime socket callers now depend on app-owned gateway/event abstractions
  instead of direct Socket.IO event constants.
- `AppSocketManager` owns connection, listener, emit, and retry behavior through
  a transport interface; Android supplies a Socket.IO transport that preserves
  current `/socket.io/` auth, websocket/polling, and reconnection behavior.
- Outbound socket queue persistence is behind a store interface with the current
  Room table kept as the Android-backed implementation.
- Full common source-set movement remains deferred until Room/domain JSON payload
  boundaries and the final cross-platform Socket.IO/WebSocket strategy are chosen.

### Local Database ✅ Done for bridge milestone

Current dependencies:

- `androidx.room:room-runtime`
- `androidx.room:room-ktx`
- `androidx.room:room-compiler`
- `androidx.room:room-sqlite-wrapper`
- `androidx.sqlite:sqlite-bundled`
- Android SQLite APIs through Room migrations.

Migration actions:

- [x] Choose Room KMP as the database strategy.
- [x] Split database entities from domain models. Current Room annotations leak
  into domain packages for messages and group messages.
- [x] Refactor `SupportSQLiteDatabase` migrations because that API is Android-only.
- [x] Preserve existing schema version and migration SQL when changing
  persistence libraries.
- [x] Keep DAOs/database builders platform-specific until the KMP database
  strategy is selected and validated.

Bridge completion notes:

- Room is upgraded to a KMP-capable version and paired with the bundled SQLite
  and Room SQLite wrapper artifacts recommended for incremental KMP migration.
- Private and group message Room annotations now live in data-layer local
  entities, with repositories mapping to the existing domain-facing models.
- Migration SQL is centralized outside Android `Migration` subclasses so it can
  be reused when migrations move to SQLite driver APIs in shared code.
- Android still owns the Room builder and database path through a local factory,
  preserving the existing `chat_db` name and schema version `13`; the generated
  Room constructor seam is deferred until this database moves into a true KMP
  source set.

### Preferences / Secure Storage

Current dependencies:

- `androidx.datastore:datastore-preferences`
- `androidx.security:security-crypto`
- `androidx.security:security-identity-credential`
- `androidx.security:security-app-authenticator`
- `EncryptedSharedPreferences`
- `MasterKey`

Migration actions:

- Refactor DataStore creation to KMP factory style with platform-specific file
  paths.
- Keep Android `Context.preferencesDataStore` only in `androidMain`.
- Replace `EncryptedSharedPreferences` with a secure storage abstraction:
  - Android actual: Android Keystore / EncryptedSharedPreferences or successor.
  - iOS actual: Keychain.
  - Desktop actual: OS keychain or encrypted file strategy.
- Treat auth tokens, refresh tokens, private keys, pending registration keys,
  device sync state, theme, language, and onboarding flags as migration-sensitive
  stored data.

### JSON Serialization

Current dependencies:

- `kotlinx-serialization-json`
- `gson`

Migration actions:

- Keep `kotlinx.serialization`.
- Replace Gson usage with kotlinx.serialization or a small common serializer
  abstraction.
- Audit all DTOs for `@Serializable`; add annotations where shared decoding is
  required.

### Images

Current dependencies:

- `io.coil-kt:coil-compose`
- `io.coil-kt:coil-video`

Migration actions:

- Upgrade to Coil 3 for CMP image loading.
- Replace Android-only `ImageRequest.Builder(context)`,
  `LocalContext`, and video frame decoding with CMP-compatible APIs or
  platform-specific thumbnail providers.
- Keep image loading wrappers in one place so UI does not directly depend on
  Android Coil APIs.

### Video / Audio / Media Playback

Current dependencies:

- `androidx.media3:media3-exoplayer`
- `androidx.media3:media3-ui`
- `androidx.media3:media3-common`
- `androidx.media3:media3-transformer`
- `androidx.media3:media3-effect`

Migration actions:

- Move Media3/ExoPlayer code to `androidMain`.
- Create shared interfaces for:
  - Audio playback controls.
  - Queue state.
  - Now-playing metadata.
  - Seek/play/pause/repeat/shuffle events.
  - Media duration/metadata extraction.
  - Media transcoding/compression, if still required.
- Implement platform actuals for iOS/desktop/web using native media APIs or a
  KMP media library.
- Keep Android `Service`, foreground notification, media session, and audio
  focus code Android-only.

### Audio Recording

Current APIs:

- `MediaRecorder`
- `RECORD_AUDIO` permission
- `FileProvider`
- Android `Uri`
- Android cache directory

Migration actions:

- Move current voice recorder implementation to `androidMain`.
- Define a shared recorder contract that returns a platform-neutral media handle
  instead of `android.net.Uri`.
- Add platform actuals for iOS/desktop only after target requirements are clear.

### Background Work

Current dependency:

- `androidx.work:work-runtime-ktx`

Migration actions:

- Keep WorkManager only in Android source set.
- Abstract background tasks:
  - Pending private chat upload retry.
  - Pending group upload retry.
  - Socket outbound retry.
  - Crash log retry.
- For non-Android targets, choose platform-specific scheduling or run retries
  in app lifecycle scope.

### Android UI Utility Libraries

Current dependencies:

- `androidx.swiperefreshlayout:swiperefreshlayout`
- `com.google.accompanist:accompanist-systemuicontroller`
- `androidx.constraintlayout:constraintlayout-compose`
- `androidx.graphics:graphics-shapes`
- `androidx.compose.material:material-icons-extended`
- `com.airbnb.android:lottie-compose`

Migration actions:

- Replace `SwipeRefreshLayout` and custom View subclassing with CMP pull refresh
  or pure Compose gesture logic.
- Replace Accompanist system UI controller with platform-specific system bar
  handling.
- Verify `constraintlayout-compose` availability for CMP targets; otherwise
  rewrite layouts with `Box`, `Row`, `Column`, `Lazy*`, and custom layouts.
- Verify `graphics-shapes` target support before moving shape utilities to
  common code.
- Keep material icons if the selected CMP dependency provides them for all
  targets; otherwise replace with shared vector resources.
- Replace Android Lottie with a CMP-compatible alternative such as Compottie, or
  keep animations platform-specific.

### Testing

Current dependencies:

- JUnit 4.
- AndroidX test JUnit.
- Espresso.
- Android Compose UI test artifacts.

Migration actions:

- Keep Android instrumentation tests in Android source sets.
- Add common tests with `kotlin.test`.
- Add target-specific UI tests only where tooling supports the chosen targets.
- Start with tests around migrated pure logic: upload policy, reducers, route
  serialization, DTO mapping, token/session rules, and crypto compatibility.

## Android-Specific APIs To Abstract Or Move

### Application / Activity / Manifest

Current Android-only areas:

- `DinoOrderApplication`.
- `MainActivity`.
- `CrashReportActivity`.
- Manifest permissions, activities, services, and provider declarations.
- Notification channels and foreground services.
- Runtime permissions.
- WebView setup.
- Locale configuration through Android resources/configuration.

Migration action:

- Keep app lifecycle shells in `androidMain`.
- Expose only platform-neutral app lifecycle events to shared code.

### Context And Resources

Current Android-only patterns:

- `Context` injected into repositories, ViewModels, API utilities, storage
  managers, media managers, and UI helpers.
- `context.getString(...)`.
- `R.string`, `R.drawable`, `R.mipmap`, XML drawables, Android resource IDs.
- `@StringRes`, `@DrawableRes`.
- `LocalContext` inside composables.

Migration action:

- Replace direct `Context` dependencies with focused interfaces:
  - `StringResolver`.
  - `AppConfig`.
  - `DeviceInfoProvider`.
  - `NetworkStatusProvider`.
  - `ClipboardManager`.
  - `Toast/Snackbar/EventNotifier`.
  - `FileSystemProvider`.
- Move resources to Compose Multiplatform resources where shared UI needs them.
- Keep Android resource IDs only in Android-specific code.

### Files, URIs, And Uploads

Current Android-only patterns:

- `android.net.Uri`.
- `ContentResolver`.
- `OpenableColumns`.
- `MimeTypeMap`.
- `MediaStore`.
- `FileProvider`.
- `Environment`.
- `java.io.File` passed through domain/repository APIs.
- Multipart uploads built from JVM input streams.

Migration action:

- Introduce a platform-neutral media/file handle, for example:
  - ID or URI string.
  - Display name.
  - MIME type.
  - Size.
  - Open-read-byte-channel callback.
- Keep Android URI resolution in `androidMain`.
- Keep common validation in shared code.

### Media And Graphics

Current Android-only patterns:

- `Bitmap`, `BitmapFactory`, `ImageDecoder`.
- `MediaMetadataRetriever`.
- `MediaPlayer`.
- `MediaRecorder`.
- `AudioManager`.
- `MediaSession`.
- `PlaybackState`.
- ExoPlayer/Media3 UI.

Migration action:

- Move platform media operations to platform implementations.
- Keep common UI state and intent models shared.
- Avoid exposing Android media types from shared ViewModels or repositories.

### Notifications

Current Android-only patterns:

- `Notification`, `NotificationChannel`, `NotificationManager`.
- `PendingIntent`.
- `NotificationManagerCompat`.
- `POST_NOTIFICATIONS`.
- Foreground service notifications.

Migration action:

- Keep Android notification implementation in `androidMain`.
- Share only notification intent/payload models if needed.

### Connectivity / Device / System

Current Android-only patterns:

- `ConnectivityManager`.
- `NetworkCapabilities`.
- `Settings.Secure`.
- `Build`, `Build.VERSION`.
- `Process`.
- `SystemClock`.
- `Handler`, `Looper`.
- `Toast`.
- `Log`.
- `ClipData`.
- `WindowManager`.

Migration action:

- Replace with common abstractions:
  - `NetworkMonitor`.
  - `DeviceIdProvider`.
  - `PlatformInfo`.
  - `Clock`.
  - `Logger`.
  - `Clipboard`.
  - `UserMessageDispatcher`.
- Use kotlinx datetime/time or Kotlin time APIs where possible.

### Crypto

Current Android/JVM-only patterns:

- `android.util.Base64`.
- `java.security.KeyPairGenerator`.
- `java.security.KeyFactory`.
- `java.security.PublicKey`.
- `java.security.PrivateKey`.
- `java.security.SecureRandom`.
- `java.security.MessageDigest`.
- `javax.crypto.Cipher`.
- `GCMParameterSpec`.
- `OAEPParameterSpec`.
- `SecretKeySpec`.
- `CertificateFactory`.

Migration action:

- Define shared crypto interfaces for:
  - Base64 encode/decode.
  - RSA key generation/import/export.
  - RSA OAEP encryption/decryption.
  - AES-GCM encryption/decryption.
  - SHA/hash operations.
- Implement actuals per platform or adopt a KMP crypto library after verifying
  RSA/OAEP and AES/GCM compatibility.
- Add cross-platform golden-vector tests before shipping.

### Parcelable / Android Route Arguments

Current Android-only patterns:

- `Parcelable`.
- `@Parcelize`.
- Android route argument passing through generated destinations.

Migration action:

- Replace with `@Serializable` route/data models or plain common data classes.
- Keep Android parcelables only for Android platform interop, not shared models.

## Libraries That Can Remain With Minimal Or Moderate Changes

- **Kotlin coroutines / Flow**: Keep, but avoid Android/JVM-only dispatchers in
  common code unless wrapped.
- **kotlinx.serialization**: Keep as primary common serialization stack.
- **Ktor core and plugins**: Keep in shared networking; move engines per target.
- **Koin**: Keep as DI framework; split Android and common artifacts.
- **Compose runtime/foundation/material3 concepts**: Keep UI architecture, but
  switch to CMP dependencies and remove Android-only APIs.
- **Pure domain models/use cases/repository interfaces**: Most can move after
  removing `Parcelable`, `Room`, `Uri`, `Context`, and resource IDs.
- **Upload MIME/size policy**: Pure policy code can move to common code once
  callers stop passing Android file types.
- **DTO mappers and reducers**: Good early candidates for `commonMain` if they
  do not import Android/JVM APIs.

## Components Likely To Require Significant Refactoring

### Navigation And UI Events

- ViewModels currently depend on generated Compose Destinations classes.
- Shared ViewModels should emit route commands or screen intents, not generated
  Android navigation objects.
- Bottom sheets/dialog destinations need an explicit common route model.

### Local Persistence

- Room annotations exist in domain packages.
- Database migrations use Android SQLite APIs.
- DAOs and entities need to be separated from domain models before sharing.
- Socket retry queue, crash queue, private messages, and group messages all need
  a common persistence strategy.

### Media Upload Pipeline

- Chat, group, profile, post, and story uploads all depend on Android `Uri`,
  `ContentResolver`, file names, MIME resolution, stream opening, and file size
  detection.
- Shared code should receive a platform-neutral upload source.
- Platform code should own URI permissions and stream opening.

### Music Playback

- Current playback uses Android service lifecycle, foreground notification,
  media session, Media3 cache, ExoPlayer, and Android intents.
- Shared code should own playlist/queue state only.
- Actual playback implementation must be platform-specific.

### Voice Recording

- Current recorder uses Android `MediaRecorder`, cache files, permissions, and
  `Uri`.
- Needs a platform recorder contract and platform implementations.

### Security And Key Management

- Tokens/private keys are stored through AndroidX Security.
- RSA/AES utilities use JVM crypto types directly.
- KMP migration must preserve existing encrypted message compatibility.
- Key migration must be designed before replacing storage.

### Crash Reporting

- Current crash flow uses Android process/activity restart behavior.
- Shared crash payload construction can be common.
- Crash capture, restart UI, device metadata, and retry scheduling are
  platform-specific.

### Main Activity / Application Startup

- Startup currently owns Koin startup, notification socket setup, notification
  permission request, theme/locale resolution, WebView, and root navigation.
- Shared app startup should be reduced to common state initialization.
- Android shell should remain thin but platform-specific.

## Migration Blockers And Compatibility Risks

- **Single-module architecture**: Code cannot move incrementally until shared
  source sets or a `:shared` module exist.
- **Generated navigation coupling**: Compose Destinations types appear across UI
  and ViewModels, making presentation sharing difficult.
- **Android `Context` spread**: Many data, domain, and presentation classes
  currently receive `Context`, preventing commonization.
- **Android resource IDs in models**: `StringResourceContent`, `AppLanguage`,
  `AppTheme`, and dialog models expose Android resource/Parcelable concepts.
- **Room annotations in domain models**: Domain/database boundaries must be
  separated before sharing.
- **Android `Uri` in repository interfaces**: Upload APIs leak Android platform
  types into domain contracts.
- **Socket.IO protocol choice**: If the server requires Socket.IO rather than
  plain WebSockets, KMP client support may require platform actuals or a server
  protocol change.
- **Crypto byte compatibility**: Message encryption must remain compatible with
  existing users and server expectations.
- **Media feature parity**: ExoPlayer/Media3 behavior may not map cleanly to iOS,
  desktop, or web.
- **Secure storage migration**: Existing tokens/private keys stored in
  `EncryptedSharedPreferences` need a safe Android migration path and equivalent
  secure storage on each new platform.
- **Background execution parity**: WorkManager and Android foreground services
  have no direct cross-platform equivalent.
- **Resource/localization strategy**: XML strings/drawables and `R` references
  must be migrated or wrapped before shared UI can compile.

## Suggested Step-By-Step Migration Order

1. **Create KMP structure without moving behavior**
   - Add shared source sets/module.
   - Keep Android app building exactly as before.
   - Add common test setup.

2. **Move pure common code**
   - Move DTOs, mappers, validation policies, reducers, simple domain models,
     and repository interfaces that do not expose Android types.
   - Replace Gson with kotlinx.serialization where needed.

3. **Define platform contracts**
   - Add interfaces for config, strings, logging, secure storage, device info,
     network status, files/uploads, crypto, background work, notifications, and
     media.
   - Keep Android implementations backed by current code.

4. **Untangle navigation**
   - Replace generated destination objects in ViewModels with common route
     commands.
   - Keep actual navigation rendering in UI/platform layer.

5. **Refactor persistence**
   - Split domain models from database entities.
   - Choose Room KMP or SQLDelight.
   - Port schema and migrations with tests.

6. **Refactor networking and uploads**
   - Move Ktor API services to common where possible.
   - Split HTTP engines by target.
   - Replace Android `Uri` upload inputs with common upload sources.

7. **Refactor crypto and secure session storage**
   - Add compatibility tests.
   - Move shared crypto intent/API to common.
   - Implement platform actuals.

8. **Migrate UI to CMP**
   - Replace Android resources with CMP resources.
   - Replace navigation, image loading, pull refresh, system UI, and platform
     media components.
   - Move composables feature by feature.

9. **Move platform-heavy features last**
   - Music playback.
   - Voice recording.
   - Video/story editing.
   - Notifications.
   - Crash reporter UI.
   - Background upload/retry workers.

## Useful Official References

- Kotlin Multiplatform overview:
  <https://kotlinlang.org/docs/multiplatform-get-started.html>
- Compose Multiplatform:
  <https://kotlinlang.org/docs/multiplatform/compose-multiplatform.html>
- Compose Multiplatform resources:
  <https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources.html>
- Ktor client engines:
  <https://ktor.io/docs/client-engines.html>
- Kotlin serialization:
  <https://kotlinlang.org/docs/serialization.html>
- Room KMP:
  <https://developer.android.com/kotlin/multiplatform/room>
- DataStore KMP:
  <https://developer.android.com/kotlin/multiplatform/datastore>
- Koin setup:
  <https://insert-koin.io/docs/setup/>
- Coil:
  <https://coil-kt.github.io/coil/>
