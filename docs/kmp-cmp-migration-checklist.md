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

### Preferences / Secure Storage ✅ Done for bridge milestone

Current dependencies:

- `androidx.datastore:datastore-preferences`
- `androidx.security:security-crypto`
- `androidx.security:security-identity-credential`
- `androidx.security:security-app-authenticator`
- `EncryptedSharedPreferences`
- `MasterKey`

Migration actions:

- [x] Refactor DataStore creation to KMP factory style with platform-specific file
  paths.
- [x] Keep Android `Context.preferencesDataStore` only in `androidMain`.
- [x] Replace `EncryptedSharedPreferences` with a secure storage abstraction:
  - Android actual: Android Keystore / EncryptedSharedPreferences or successor.
  - iOS actual: Keychain.
  - Desktop actual: OS keychain or encrypted file strategy.
- [x] Treat auth tokens, refresh tokens, private keys, pending registration keys,
  device sync state, theme, language, and onboarding flags as migration-sensitive
  stored data.

Bridge completion notes:

- Settings DataStore construction now goes through a platform factory that supplies
  the Android file path while `DataStoreUtil` depends only on `DataStore<Preferences>`.
- Secure session/key material access now goes through an app-owned
  `SecureKeyValueStore` abstraction, with Android backed by the existing
  `EncryptedSharedPreferences` and `MasterKey` strategy.
- Existing storage filenames, preference keys, defaults, and public manager APIs
  are preserved so current Android callers continue to work during incremental
  source-set migration.

### JSON Serialization ✅ Done for bridge milestone

Current dependencies:

- `kotlinx-serialization-json`
- `gson`

Migration actions:

- [x] Keep `kotlinx.serialization`.
- [x] Replace Gson usage with kotlinx.serialization or a small common serializer
  abstraction.
- [x] Audit all DTOs for `@Serializable`; add annotations where shared decoding is
  required.

Bridge completion notes:

- Gson is no longer registered in DI or included as an app dependency; Ktor
  content negotiation continues to use kotlinx serialization for HTTP JSON.
- Chat DTOs that still participate in JSON payload decoding now have
  `@Serializable` annotations and explicit serial names for snake_case fields.
- Existing Socket.IO `JSONObject` and `JSONArray` usage is intentionally left in
  place until the realtime payload boundary moves further toward common code.

### Images ✅ Done for bridge milestone

Current dependencies:

- `io.coil-kt:coil-compose`
- `io.coil-kt:coil-video`

Migration actions:

- [x] Upgrade to Coil 3 for CMP image loading.
- [x] Replace Android-only `ImageRequest.Builder(context)`,
  `LocalContext`, and video frame decoding with CMP-compatible APIs or
  platform-specific thumbnail providers.
- [x] Keep image loading wrappers in one place so UI does not directly depend on
  Android Coil APIs.

Bridge completion notes:

- Coil is upgraded to Coil 3 using `io.coil-kt.coil3` artifacts, with OkHttp
  networking kept for the current Android target.
- Direct Coil Compose, request, cache, image loader, and video-frame APIs are
  isolated behind `PlatformImage` wrappers so feature UI calls app-owned image
  APIs instead of Android/Coil request builders.
- Existing video thumbnail first-frame behavior is preserved through wrapper
  options, while full media playback/editing remains Android-specific for the
  later media milestones.

### Video / Audio / Media Playback ✅ Done for bridge milestone

Current dependencies:

- `androidx.media3:media3-exoplayer`
- `androidx.media3:media3-ui`
- `androidx.media3:media3-common`
- `androidx.media3:media3-transformer`
- `androidx.media3:media3-effect`

Migration actions:

- [x] Move Media3/ExoPlayer code to Android platform seams.
- [x] Create shared-facing interfaces for:
  - Audio playback controls.
  - Queue state.
  - Now-playing metadata.
  - Seek/play/pause/repeat/shuffle events.
  - Media duration/metadata extraction.
  - Media transcoding/compression, if still required.
- [ ] Implement platform actuals for iOS/desktop/web using native media APIs or a
  KMP media library.
- [x] Keep Android `Service`, foreground notification, media session, and audio
  focus code Android-only.

Bridge completion notes:

- Audio playback UI now calls a platform-neutral `MediaPlaybackController`
  surface; Android `Context`, foreground-service intents, Media3 player setup,
  media session, notifications, caching, and audio-focus/noisy handling remain
  inside Android-backed playback code.
- Playback state now includes now-playing metadata, queue contents, queue index,
  progress, duration, buffering, error, repeat, and shuffle state for shared UI
  migration.
- Video playback composables use the app-owned `PlatformVideoPlayer` wrapper
  instead of embedding ExoPlayer/PlayerView directly in feature screens.
- Duration/title/artist/MIME probing now goes through a shared-facing media
  metadata extractor contract with Android backed by `MediaMetadataRetriever`.
- Story video export/transformation still uses Android Media3 Transformer behind
  the current Android story-editor implementation; non-Android actuals remain a
  future target-specific milestone.

### Audio Recording ✅ Done for bridge milestone

Current APIs:

- `MediaRecorder`
- `RECORD_AUDIO` permission
- `FileProvider`
- Android `Uri`
- Android cache directory

Migration actions:

- [x] Move current voice recorder implementation to Android platform seams.
- [x] Define a shared recorder contract that returns a platform-neutral media handle
  instead of `android.net.Uri`.
- [ ] Add platform actuals for iOS/desktop only after target requirements are clear.

Bridge completion notes:

- Voice recording now uses an app-owned `PlatformAudioRecorder` contract with
  neutral `RecordedAudio` handles for recording source, name, MIME type, size,
  and duration.
- Android recording remains backed by `MediaRecorder`, AAC-in-MP4 `.m4a` cache
  files, Android cache cleanup, and Android URI conversion only at the current
  upload boundary.
- Private and group chat ViewModels depend on the recorder contract instead of
  the concrete Android recorder type, while runtime microphone permission
  launchers remain in Android UI screens.

### Background Work ✅ Done for bridge milestone

Current dependency:

- `androidx.work:work-runtime-ktx`

Migration actions:

- [x] Keep WorkManager only in Android source set.
- [x] Abstract background tasks:
  - Pending private chat upload retry.
  - Pending group upload retry.
  - Socket outbound retry.
  - Crash log retry.
- [x] For non-Android targets, choose platform-specific scheduling or run retries
  in app lifecycle scope.

Bridge completion notes:

- Background retry requests now go through an app-owned `BackgroundTaskScheduler`
  and task registry instead of feature code directly owning scheduling details.
- Socket outbound retry and crash log retry are registered task handlers that
  preserve the current in-process coroutine behavior for the Android app.
- Private and group upload retry task IDs exist as no-op bridge hooks; persistent
  attachment upload retry is deferred until cross-platform file-handle retention
  and retry UX are specified.
- WorkManager remains an Android dependency and can be introduced behind the
  Android scheduler implementation without changing shared-facing task callers.

### Android UI Utility Libraries ✅ Done for bridge milestone

Current dependencies:

- `androidx.swiperefreshlayout:swiperefreshlayout`
- `com.google.accompanist:accompanist-systemuicontroller`
- `androidx.constraintlayout:constraintlayout-compose`
- `androidx.graphics:graphics-shapes`
- `androidx.compose.material:material-icons-extended`
- `com.airbnb.android:lottie-compose`

Migration actions:

- [x] Replace `SwipeRefreshLayout` and custom View subclassing with CMP pull refresh
  or pure Compose gesture logic.
- [x] Replace Accompanist system UI controller with platform-specific system bar
  handling.
- [x] Verify `constraintlayout-compose` availability for CMP targets; otherwise
  rewrite layouts with `Box`, `Row`, `Column`, `Lazy*`, and custom layouts.
- [x] Verify `graphics-shapes` target support before moving shape utilities to
  common code.
- [x] Keep material icons if the selected CMP dependency provides them for all
  targets; otherwise replace with shared vector resources.
- [x] Replace Android Lottie with a CMP-compatible alternative such as Compottie, or
  keep animations platform-specific.

Bridge completion notes:

- `SwipeRefreshLayout`, Accompanist system UI controller, ConstraintLayout
  Compose, AndroidX graphics-shapes, Material Icons Extended, and Lottie Compose
  dependencies were removed from the app module.
- The remaining active playlist ConstraintLayout usage was rewritten with pure
  Compose layout primitives, and the profile decorative polygon shape now uses
  an app-owned Compose `Shape` implementation.
- Direct Material icon imports were replaced by an app-owned `AppIcons` vector
  boundary, so future CMP resource migration can move icons without touching
  feature screens.
- The create-post options animation uses the existing custom Canvas animation;
  stale Lottie imports and dependency wiring were removed.

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

### Application / Activity / Manifest ✅ Done for bridge milestone

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

- [x] Keep app lifecycle shells in `androidMain`.
- [x] Expose only platform-neutral app lifecycle events to shared code.

Bridge completion notes:

- Android application and activity classes remain the app-shell owners for Koin
  startup, manifest entrypoints, runtime permissions, locale resource mutation,
  crash UI restart intents, foreground services, and provider declarations.
- App foreground/background and visible-screen callbacks now flow through
  platform-neutral lifecycle event and observer abstractions instead of placing
  all background cleanup directly in `DinoOrderApplication`.
- Navigation events now use an injectable UI event bus, so shared-facing callers
  no longer need to depend on app-shell global flows.
- Notification channel setup and notification clearing now go through an
  app-owned notification controller backed by the current Android notification
  helper.
- Notification deep links are parsed into typed app routes behind an Android
  intent handler, keeping `Intent` and notification extras at the Android shell
  boundary.

### Context And Resources ✅ Done for bridge milestone

Current Android-only patterns:

- `Context` injected into repositories, ViewModels, API utilities, storage
  managers, media managers, and UI helpers.
- `context.getString(...)`.
- `R.string`, `R.drawable`, `R.mipmap`, XML drawables, Android resource IDs.
- `@StringRes`, `@DrawableRes`.
- `LocalContext` inside composables.

Migration action:

- [x] Replace direct `Context` dependencies with focused interfaces:
  - `StringResolver`.
  - `AppConfig`.
  - `DeviceInfoProvider`.
  - `NetworkStatusProvider`.
  - `ClipboardManager`.
  - `Toast/Snackbar/EventNotifier`.
  - `FileSystemProvider`.
- [ ] Move resources to Compose Multiplatform resources where shared UI needs them.
- [x] Keep Android resource IDs only in Android-specific code for this bridge
  milestone.

Bridge completion notes:

- Non-composable string resolution now has a `StringResolver` seam, with Android
  backed by existing string resources.
- Runtime app constants now flow through `AppConfig`, keeping `BuildConfig`
  reads at the Android DI boundary.
- Device ID, network status, and app cache cleanup are behind app-owned provider
  interfaces with Android implementations.
- Chat, group chat, and main-screen user feedback now use the existing
  platform UI effect boundary for toast and clipboard actions instead of direct
  `Toast`/clipboard calls in shared-facing UI paths.
- Full resource migration remains deferred: composable `stringResource`,
  `painterResource`, Android drawable IDs, media `Uri`/`ContentResolver`, and
  platform media save flows stay Android-specific until the resource and file
  milestones.

### Files, URIs, And Uploads ✅ Done for bridge milestone

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

- [x] Introduce a platform-neutral media/file handle:
  - ID or URI string.
  - Display name.
  - MIME type.
  - Size.
  - Open-read-byte-channel callback.
- [x] Keep Android URI resolution in `androidMain`.
- [x] Keep common validation in shared code.

Bridge completion notes:

- Upload-facing domain contracts now use `ReadableMediaFile`/`MediaFileHandle`
  instead of exposing `android.net.Uri`.ik
- Android picker, recorder, and story editor boundaries convert `Uri` values into
  readable media files before invoking domain use cases.
- Private chat, group chat, post/story, and profile multipart uploads now reuse
  neutral file metadata/input providers; `ContentResolver`, `OpenableColumns`,
  `MimeTypeMap`, `File`, and stream conversion remain Android implementation
  details.

### Media And Graphics ✅ Done for bridge milestone

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

- [x] Move platform media operations to platform implementations.
- [x] Keep common UI state and intent models shared.
- [x] Avoid exposing Android media types from shared ViewModels or repositories.

Bridge completion notes:

- Media selection/upload boundaries now use platform-neutral
  `PlatformMediaReference`, `PlatformPickedMedia`, `PlatformMediaResolver`, and
  `PlatformMediaMetadataExtractor` contracts.
- Android `Uri`, `ContentResolver`, and `MediaMetadataRetriever` usage is
  isolated in Android media/upload implementations and Compose picker edges.
- Chat, group chat, create/edit post, music post, and edit profile ViewModels no
  longer expose Android media types for picked/uploaded media.
- Voice recordings are sent through neutral media references instead of passing
  Android `Uri` values through ViewModels.
- Media3/ExoPlayer playback, Coil image loading, and story bitmap/export code
  remain Android platform implementations behind existing UI/platform wrappers.

### Notifications ✅ Done for bridge milestone

Current Android-only patterns:

- `Notification`, `NotificationChannel`, `NotificationManager`.
- `PendingIntent`.
- `NotificationManagerCompat`.
- `POST_NOTIFICATIONS`.
- Foreground service notifications.

Migration action:

- [x] Keep Android notification implementation in `androidMain`.
- [x] Share only notification intent/payload models if needed.

Bridge completion notes:

- Notification display is now behind an app-owned presenter interface, while
  Android channel creation, `PendingIntent` construction, notification IDs,
  resources, and `MainActivity` intents stay in the Android implementation.
- Socket-driven in-app notification orchestration no longer depends directly on
  Android `Context` or `SharedPreferences`; private-message dedupe persistence
  is supplied through a notification snapshot store interface.
- Notification channel setup, clearing, dedupe reset, and notification socket
  disconnects continue through `AppNotificationController`, so shared-facing
  callers do not need direct Android notification APIs.
- `POST_NOTIFICATIONS` runtime permission handling and foreground service
  playback notifications remain Android shell/service responsibilities.

### Connectivity / Device / System ✅ Done for bridge milestone

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

- [x] Replace with common abstractions:
  - `NetworkMonitor`.
  - `DeviceIdProvider`.
  - `PlatformInfo`.
  - `Clock`.
  - `Logger`.
  - `Clipboard`.
  - `UserMessageDispatcher`.
- [x] Use kotlinx datetime/time or Kotlin time APIs where possible.

Bridge completion notes:

- Connectivity and device ID are behind app-owned providers backed by Android
  `ConnectivityManager`, `NetworkCapabilities`, and `Settings.Secure`.
- Platform device metadata, process termination, wall-clock time, and uptime are
  behind system contracts used by crash reporting, socket queue persistence, and
  debug watchdog code.
- Shared-adjacent socket, notification, crash, and upload orchestration now use
  `AppLogger`/`AppClock` instead of direct Android logging or time APIs.
- Clipboard, toast, and browser side effects remain behind `PlatformUiEffect`
  with Android `Toast`, `ClipData`, and intents isolated in the Android
  implementation.
- Android-only UI, media, music service, theme, permission, keyboard/window, and
  story export code still owns its platform system APIs until those areas move
  behind their own platform shells.

### Crypto ✅ Done for bridge milestone

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

- [x] Define shared crypto interfaces for:
  - [x] Base64 encode/decode.
  - [x] RSA key generation/import/export.
  - [x] RSA OAEP encryption/decryption.
  - [x] AES-GCM encryption/decryption.
  - [x] SHA/hash operations.
- [x] Implement actuals per platform or adopt a KMP crypto library after verifying
  RSA/OAEP and AES/GCM compatibility.
- [x] Add cross-platform golden-vector tests before shipping.

Bridge completion notes:

- Message encryption/decryption callers now depend on app-owned crypto
  contracts and a `MessageCrypto` facade instead of direct presentation utility
  functions.
- Android/JVM crypto remains the current platform implementation, preserving RSA
  2048 key generation, RSA OAEP SHA-256/MGF1 SHA-256 v2 payloads, AES-GCM
  `iv + ciphertext/tag` payloads, and legacy RSA PKCS#1/AES fallback
  decryption.
- Public/private key import/export and public-key debug hashing are behind the
  same crypto boundary, including the existing X.509, PEM, certificate,
  PKCS#1 RSA public key, nested PEM, and OpenSSH `ssh-rsa` public-key formats.
- JVM unit tests cover v2 private-message round trips, group-message recipient
  key round trips, existing group AES-key reply-preview encryption, legacy
  payload decryption, public-key decoding formats, and stable debug
  fingerprints.
- Full common source-set movement still waits on the actual KMP module split and
  non-Android implementations or a vetted KMP crypto library for each target.

### Parcelable / Android Route Arguments ✅ Done

Current Android-only patterns:

- `Parcelable`.
- `@Parcelize`.
- Android route argument passing through generated destinations.

Migration action:

- Replace with `@Serializable` route/data models or plain common data classes.
- Keep Android parcelables only for Android platform interop, not shared models.

## Libraries That Can Remain With Minimal Or Moderate Changes ✅ Done for bridge milestone

- [x] **Kotlin coroutines / Flow**: Keep, but avoid Android/JVM-only dispatchers in
  common code unless wrapped.
- [x] **kotlinx.serialization**: Keep as primary common serialization stack.
- [x] **Ktor core and plugins**: Keep in shared networking; move engines per target.
- [x] **Koin**: Keep as DI framework; split Android and common artifacts.
- [x] **Compose runtime/foundation/material3 concepts**: Keep UI architecture, but
  switch to CMP dependencies and remove Android-only APIs.
- [x] **Pure domain models/use cases/repository interfaces**: Most can move after
  removing `Parcelable`, `Room`, `Uri`, `Context`, and resource IDs.
- [x] **Upload MIME/size policy**: Pure policy code can move to common code once
  callers stop passing Android file types.
- [x] **DTO mappers and reducers**: Good early candidates for `commonMain` if they
  do not import Android/JVM APIs.

Bridge completion notes:

- Coroutine dispatching that remained inside reusable domain cleanup now goes
  through an app-owned dispatcher contract; Android supplies `Dispatchers.IO`.
- Private-message deduplication no longer depends on `Context` or
  `SharedPreferences` from domain code. The current Android app keeps the same
  `msg_dedup` preference storage behind a small store interface.
- Upload MIME/category/size policy now lives in the domain upload package and is
  covered by JVM tests; Android URI, MIME resolver, file-size, and stream-opening
  code remains in the Android upload provider.
- kotlinx.serialization DTOs, route models, mapper tests, reducers, Ktor core,
  Koin common artifacts, and CMP UI dependencies are already retained behind the
  bridge boundaries established in earlier checklist sections.
- Remaining full common-source movement is deferred until Android string
  resource IDs and composable string resolution are moved out of
  `StringResourceContent`, `AppTheme`, and `AppLanguage`, and until the actual
  KMP source-set/module split is created.

## Components Likely To Require Significant Refactoring

### Navigation And UI Events ✅ Done for bridge milestone

- [x] ViewModels currently depend on generated Compose Destinations classes.
- [x] Shared ViewModels should emit route commands or screen intents, not generated
  Android navigation objects.
- [x] Bottom sheets/dialog destinations need an explicit common route model.

Bridge completion notes:

- Generated Compose Destinations imports and generated destination objects have
  been removed from app source; ViewModels now emit serializable `AppRoute`
  commands through `UIEvent`.
- Screen routes and modal routes share the same typed route model, with modal
  bottom sheets/dialogs explicitly marked by `AppRoute.Modal`.
- `HandleUIEvent` remains the platform/UI adapter that maps route commands to
  the active CMP `NavController` or modal state, keeping ViewModels independent
  of generated Android navigation objects.
- Full common-source movement still depends on moving Android `ViewModel`,
  `AndroidViewModel`, `SavedStateHandle`, and activity-owned lifecycle wiring
  into the eventual platform source sets.

### Local Persistence ✅ Done for bridge milestone

- [x] Remove Room annotations from domain packages.
- [x] Move database migration SQL out of Android-only migration classes so it can
  be reused by future KMP database setup.
- [x] Keep DAOs and database entities separated from domain models before sharing.
- [x] Put socket retry queue, crash queue, private message, group message,
  notification dedupe, and message dedupe persistence behind app-owned
  persistence boundaries or Android-backed implementations.

Bridge completion notes:

- Room-backed chat, group, socket queue, and crash-log persistence remain
  Android-owned for the bridge milestone, preserving the existing schema,
  database name, and migration path.
- Private and group message persistence maps through data-layer local entities,
  while repository and use-case callers work with plain domain models.
- Settings DataStore and secure key/value storage already use platform factory
  and secure-store abstractions, preserving existing filenames, keys, and
  Android encrypted storage behavior.
- Private notification dedupe and message dedupe persistence are supplied through
  interfaces while the Android app keeps the existing SharedPreferences files.
- Full common-source movement remains deferred until shared source sets exist and
  non-Android database/storage implementations are added.

### Media Upload Pipeline ✅ Done for bridge milestone

- Chat, group, profile, post, and story uploads all depend on Android `Uri`,
  `ContentResolver`, file names, MIME resolution, stream opening, and file size
  detection.
- Shared code should receive a platform-neutral upload source.
- Platform code should own URI permissions and stream opening.
- Bridge milestone completed for chat, group, profile, post, and music-post
  picker/upload flows through platform-neutral media references and readable
  upload files. Story editor export remains Android-specific.

### Music Playback ✅ Done for bridge milestone

- [x] Current playback uses Android service lifecycle, foreground notification,
  media session, Media3 cache, ExoPlayer, and Android intents.
- [x] Shared code should own playlist/queue state only.
- [x] Actual playback implementation must be platform-specific.

Bridge completion notes:

- Playlist queue normalization, active index tracking, playback order, repeat
  cycling, and shuffle reordering now live in an Android-free playback queue
  controller that can move with shared UI/state code.
- Android still supplies the actual playback controller through the existing
  service-backed manager, preserving foreground notification, media session,
  Media3 cache, ExoPlayer, service intents, and current controls.
- `MediaPlaybackController`, `MediaPlaybackState`, and `MediaQueueItem` remain
  platform-neutral contracts for shared-facing callers, with Android binding the
  controller implementation through DI.

### Voice Recording ✅ Done for bridge milestone

- [x] Current recorder uses Android `MediaRecorder`, cache files, permissions,
  and Android upload-boundary URI conversion.
- [x] Shared-facing code depends on `PlatformAudioRecorderFactory` and
  `PlatformAudioRecorder` instead of constructing the Android recorder directly.
- [x] Recorded audio is returned as neutral `RecordedAudio` metadata and handed
  to chat/group upload flows as `PlatformMediaReference`.
- [x] Runtime microphone permission handling remains in Android UI screens.
- [ ] Add platform recorder implementations for iOS/desktop/web after target
  requirements are clear.

### Security And Key Management ✅ Done for bridge milestone

- [x] Tokens/private keys are stored through a secure key-value abstraction backed
  by AndroidX Security on Android.
- [x] Message encryption/decryption uses app-owned crypto contracts instead of
  direct presentation crypto utilities.
- [x] JWT token handling no longer depends on Android Base64 APIs.
- [x] Existing encrypted message compatibility is covered by JVM crypto tests.
- [x] Existing Android preference filenames, keys, defaults, and storage APIs are
  preserved for incremental migration.
- [ ] Add non-Android secure storage and crypto implementations after target
  requirements are clear.

Bridge completion notes:

- `SecureKeyValueStore` isolates secure session/key material storage while
  Android remains backed by the existing `EncryptedSharedPreferences` and
  `MasterKey` setup.
- `MessageCrypto`, `RsaCrypto`, `AesGcmCrypto`, `Base64Codec`, and `HashCrypto`
  isolate RSA/AES/Base64/hash operations from shared-facing callers.
- Current Android/JVM crypto preserves RSA 2048 key generation, RSA OAEP
  SHA-256/MGF1 SHA-256 v2 payloads, AES-GCM `iv + ciphertext/tag` payloads,
  and legacy RSA PKCS#1/AES fallback decryption.
- Stored auth tokens, refresh tokens, private keys, pending registration keys,
  user/session flags, and device sync state keep their existing storage keys.

### Crash Reporting ✅ Done for bridge milestone

- [x] Current crash flow uses platform contracts for app config, device metadata,
  logging, retry scheduling, and process termination.
- [x] Shared-facing crash payload construction is isolated in `CrashLogBuilder`.
- [x] Android crash report UI launch is isolated behind `CrashUiLauncher`.
- [x] Crash upload payload mapping preserves the existing server wire shape.
- [ ] Add non-Android crash capture, persistence, retry, and restart UI
  implementations after target requirements are clear.

Bridge completion notes:

- `CrashReporter` now orchestrates capture, persistence, retry, upload, and
  termination while delegating crash log construction and upload DTO mapping to
  a pure builder.
- Android still owns `Thread.setDefaultUncaughtExceptionHandler`,
  `CrashReportActivity`, restart intents, Room queue persistence, and the
  process kill/exit implementation.
- Crash payloads keep existing app version, build type, thread, exception,
  stack trace, timestamp, device, OS, SDK, username, and event ID fields.
- Unit tests cover metadata population, breadcrumb formatting, trimming limits,
  and ISO UTC upload timestamp mapping.

### Main Activity / Application Startup ✅ Done for bridge milestone

- Startup currently owns Koin startup, notification socket setup, notification
  permission request, theme/locale resolution, WebView, and root navigation.
- [x] Shared app startup should be reduced to common state initialization.
- [x] Android shell should remain thin but platform-specific.

Bridge completion notes:

- `AppStartupCoordinator` now owns shared-facing startup initialization and
  lifecycle observer registration without depending on Android framework types.
- Notification socket startup is behind `NotificationSocketController`, with
  Android still supplying the current secure-token lookup and Socket.IO-backed
  in-app notification manager.
- `DinoOrderApplication` still owns Koin startup, crash process handling, crash
  reporter installation, notification channel setup, Android music playback
  initialization, and Android activity lifecycle callbacks.
- `MainActivity` still owns Android permission prompts, edge-to-edge setup,
  Compose content, locale application, intent deep-link consumption, and
  navigation execution.
- Unit tests cover idempotent shared startup initialization and notification
  socket connect/disconnect policy.

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
