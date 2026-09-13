# KMP / CMP Migration Checklist

This is the main migration tracker for moving the current Android app to
Kotlin Multiplatform (KMP) with Compose Multiplatform (CMP).

Target structure:

- `:androidApp`: Android application shell.
- `:composeApp`: KMP/CMP shared module.

Target platforms:

- Android.
- iOS.
- Desktop.
- Web/Wasm.

## Current Verified State

- [x] The Android shell builds as `:androidApp`.
  - Verified command: `./gradlew :androidApp:compileDebugKotlin`.
  - Result: successful.
- [x] The repository is currently clean before this checklist rewrite.
- [x] The project now uses the target `:androidApp` + `:composeApp` structure.
  - Android app source lives under `androidApp/src/main/java`.
  - `:composeApp` is an initial Kotlin Multiplatform / Compose Multiplatform
    module with common, Android, iOS, desktop, and web/Wasm targets.
- [x] Compose Destinations generated navigation appears to have been removed from
  active source.
  - Current app navigation uses app-owned route/event types such as `AppRoute`,
    `AppModalRoute`, `AppNavGraph`, and `UIEvent`.
- [x] Several migration seams already exist and should be preserved.
  - Networking: `AppHttpClientFactory`, `NetworkExceptionClassifier`,
    `AppLogger`.
  - Realtime socket: `RealtimeSocketGateway`, `RealtimeSocketTransport`,
    `SocketOutboundQueueStore`.
  - Persistence: local Room entities are separated for private and group
    messages, and migration SQL is centralized in `MessageDatabaseMigrationSql`.
  - Preferences/security: `SecureKeyValueStore` and platform DataStore factory.
  - Platform UI/media: `PlatformImage`, `PlatformVideoPlayer`,
    `PlatformUiEffect`, platform media references, upload content abstractions,
    audio recorder abstractions, and playback controller abstractions.
  - Background work: `BackgroundTaskScheduler` and task registry abstractions.
  - Crypto: `MessageCrypto`, `RsaCrypto`, `AesGcmCrypto`, `Base64Codec`,
    `HashCrypto`.
- [x] Useful JVM/Android tests already exist for DTO mappers, routes, crypto,
  socket retry policy, upload policy, notification logic, startup, and several
  reducers.

## Architecture Rules

All migration work must preserve MVVM with Clean Architecture.

- [ ] Domain code must not import data-layer packages.
- [ ] Domain code must not import presentation-layer packages.
- [ ] Domain models and repository interfaces must expose domain-safe models,
  not DTOs, Room entities, Android `Uri`, Android `Context`, or platform file
  types.
- [ ] ViewModels/state holders must depend on use cases and platform-neutral
  interfaces, not repositories directly when a use case exists.
- [ ] Shared ViewModels/state holders must not extend `AndroidViewModel` and must
  not depend on `Application`, Android `Context`, Android resources, Android
  lifecycle APIs, or Android permission APIs.
- [ ] Platform behavior must be represented by focused interfaces and supplied
  through platform implementations.
- [ ] Data implementations may depend on Ktor, Room, platform storage, platform
  media, and DTOs, but those details must not leak into domain contracts.
- [ ] Presentation composables that move to common must use CMP-compatible APIs
  and app-owned platform wrappers for side effects.
- [ ] After each completed change, run the relevant verification command and
  commit the change with an appropriate scoped commit message.

## Migration Checklist

### 1. Establish And Commit The Baseline

- [x] Commit this checklist rewrite.
  - Use a documentation commit message, for example:
    `docs(kmp): refresh migration checklist`.
- [x] Record the current baseline build status in the commit or PR notes.
  - Required command: `./gradlew :app:compileDebugKotlin`.
  - Current verified result: successful on 2026-08-12.
  - Baseline checklist commit: `486e648 docs(kmp): refresh migration checklist`.
- [x] Before every later migration step, confirm whether the working tree already
  contains unrelated changes and avoid reverting user work.
  - Confirmed clean working tree before this baseline update.

### 2. Create The Real KMP/CMP Module Structure

- [x] Rename or replace the current Android-only `:app` shell with
  `:androidApp`.
  - Preserve `applicationId = "com.sec.message"`.
  - Preserve Android namespace, manifest declarations, permissions, services,
    providers, signing, ProGuard, version code/name, and Android resources.
  - Keep Android app startup, `DinoOrderApplication`, `MainActivity`,
    `CrashReportActivity`, notification channels, foreground services,
    permission prompts, edge-to-edge setup, and intent handling in the Android
    shell.
- [x] Add `:composeApp` as a Kotlin Multiplatform module.
  - Configure `commonMain`, `commonTest`, `androidMain`, `iosMain`,
    `desktopMain`, and web/wasm source sets.
  - Apply `org.jetbrains.kotlin.multiplatform`, `org.jetbrains.compose`,
    Compose compiler, and serialization plugins as needed.
  - Keep Android-only Gradle plugins out of shared/common code.
- [x] Wire `:androidApp` to depend on `:composeApp`.
  - The Android app must continue to compile and launch after the split.
  - Do not move behavior in this step except the minimum needed for module
    wiring.
- [x] Add baseline KMP verification tasks.
  - Compile Android target.
  - Compile common metadata.
  - Run `commonTest`.
  - Add iOS, desktop, and web/wasm compile checks as soon as source sets are
    configured.
- [x] Commit after verification.
  - Suggested message: `build(kmp): add androidApp and composeApp modules`.
  - Verified commands:
    - `./gradlew :androidApp:compileDebugKotlin`
    - `./gradlew :composeApp:compileKotlinMetadata`
    - `./gradlew :composeApp:compileDebugKotlinAndroid`
    - `./gradlew :composeApp:compileKotlinDesktop`
    - `./gradlew :composeApp:compileKotlinWasmJs`
    - `./gradlew verifyKmpBaseline`
  - Current verified result: successful on 2026-08-12.
  - Follow-up: run the configured iOS compile/test tasks on macOS; Kotlin/Native
    iOS targets are configured but disabled on this Linux host.

### 3. Fix Clean Architecture Boundary Violations

- [x] Move `Resource` out of `corefeature.presentation.util`.
  - Added `corefeature.domain.result.Resource` and updated domain, data,
    presentation, and test imports.
  - Ensure presentation-only UI event state stays in presentation.
- [x] Remove domain imports of data DTOs.
  - Current examples included chat/group domain contracts importing
    `MessageAttachment`, `MessageSent`, `NewMessage`, `PendingMessage`, and
    `UnreadResponse` from data remote DTO packages.
  - Added domain-safe models for currently domain-facing DTO concepts and mapped
    remote DTOs in the data layer.
  - Kept server wire names and serialization annotations in DTOs.
- [x] Remove domain imports of data upload classes.
  - Moved neutral readable upload contracts to `corefeature.domain.upload`.
  - Kept Android `Uri`, `ContentResolver`, stream opening, MIME probing,
    metadata extraction, and Ktor multipart helpers in platform/data code.
- [x] Replace domain dependencies on concrete persistence/session classes.
  - Added domain interfaces for local session access/clearing, local message
    store clearing, and active conversation state clearing.
  - Bound current `SPrefManager`, Room database, and active tracker
    implementations through DI.
- [x] Add boundary checks.
  - Add a lightweight script or Gradle check that fails when domain code imports
    `.data.`, `.presentation.`, `android.`, or `androidx.` packages, except for
    explicitly approved multiplatform-safe APIs.
  - Added `checkDomainBoundaries` and wired it into `verifyKmpBaseline`.
  - Verified commands:
    - `./gradlew checkDomainBoundaries`
    - `./gradlew :androidApp:compileDebugKotlin`
    - `./gradlew :androidApp:testDebugUnitTest`
    - `./gradlew verifyKmpBaseline`
  - Result: successful on 2026-08-12.
- [x] Commit after verification.
  - Suggested message: `refactor(architecture): enforce domain boundaries`.

### 4. Make ViewModels And State Holders Multiplatform-Ready

- [x] Replace the shared state-holder/event base with CMP-compatible types.
  - Moved `StateHolder`, `ScopedStateHolder`, `HaveUIEvent`, `HaveUIEventImpl`,
    `UIEvent`, `UiEventBus`, `PopBackStackLevel`, `AppRoute`, `AppConfig`,
    `StringResolver`, and `Resource` into `:composeApp/commonMain`.
  - Added shared `commonTest` coverage for `HaveUIEventImpl`.
- [x] Remove `Application` and `Context` constructor dependencies from the
  migrated auth/core ViewModels.
  - Updated login, registration, private-key alert, main-activity, custom
    message dialog, and private-key reset holders to use `ScopedStateHolder`,
    `UiEventBus`, `StringResolver`, `AppConfig`, and focused platform
    interfaces.
  - Preserved navigation commands, storage keys, and session behavior.
- [x] Keep lifecycle, permissions, activity result launchers, saved-state
  adapters, and Android navigation execution outside shared ViewModels.
  - Replaced remaining Android/data constructor dependencies in state holders
    with focused domain/platform interfaces for session, settings, socket,
    logging, media resolution, active conversation state, splash version checks,
    and clocks.
  - Kept Android permissions, launchers, lifecycle hooks, URI conversion, and
    navigation execution in Android screens/platform wiring.
- [ ] Ensure ViewModels expose immutable state and one-shot events.
  - State should be serializable or platform-neutral where practical.
  - Navigation should continue through app route commands, not platform
    navigation types.
  - Follow-up: replace remaining public mutable Compose state properties with
    read-only state plus update/event methods in auth, profile, group, search,
    report, thread, and create-post holders.
  - Follow-up: replace JVM-only helpers such as `java.time`, `java.net.URI`, and
    Java collections in chat/group/thread holders before moving those holders
    into `commonMain`.
- [x] Add unit tests for migrated state holders.
  - Added shared event-base coverage plus Android-side route-selection and
    dialog-holder tests.
- [x] Commit after verification.
  - Commit message: `refactor(viewmodel): make state holders multiplatform-ready`.
  - Verified commands:
    - `./gradlew checkDomainBoundaries`
    - `./gradlew :androidApp:compileDebugKotlin`
    - `./gradlew :androidApp:testDebugUnitTest`
    - `./gradlew verifyKmpBaseline`
  - Result: successful on 2026-09-12.

### 5. Move Pure Common Logic To `:composeApp/commonMain`

- [x] Move platform-free domain models and use cases after the architecture
  cleanup is complete.
  - Moved platform-free domain models, repository contracts, and use cases for
    auth, activity notifications, main page, report, search, splash, crash,
    core contracts, and pure chat/group/profile/thread models.
  - Kept upload-bound repositories/use cases and models in Android until
    `ReadableMediaFile` no longer exposes Ktor/JVM upload input details.
- [x] Move pure reducers, policies, route models, queue controllers, and utility
  logic.
  - Moved upload size/MIME policies, local paging, message deduplication,
    route modal helper, socket outbound retry queue, contact delta reducer, and
    playback queue controller/state/contracts.
- [x] Move existing JVM tests for moved logic to `commonTest`.
  - Converted moved tests from JUnit assertions to `kotlin.test`.
  - Kept Android-specific tests in Android source sets.
- [x] Keep any code with Android, JVM-only, Room, Java security, Java IO, or
  resource imports out of `commonMain` until platform abstractions exist.
  - Follow-up: move `ReadableMediaFile` and upload-bound repository contracts
    after replacing the Ktor/JVM input type with a shared upload stream
    abstraction.
  - Follow-up: move `SocketRetryPolicy` and `SocketOutboundPayloadCodec` after
    replacing direct `org.json` usage with a common payload representation and
    Android socket edge conversion.
  - Follow-up: move comment ordering after replacing the `java.time` server
    timestamp parser with a common-compatible parser or clock abstraction.
  - Follow-up: move `SyncDeviceIdUseCase` after replacing JVM
    `AtomicBoolean` with a common synchronization strategy.
- [x] Commit in small groups.
  - Commit message: `refactor(kmp): move pure common logic`.
  - Verified commands:
    - `./gradlew checkDomainBoundaries`
    - `./gradlew :composeApp:compileKotlinMetadata`
    - `./gradlew :composeApp:desktopTest`
    - `./gradlew :composeApp:compileDebugKotlinAndroid`
    - `./gradlew :androidApp:compileDebugKotlin`
    - `./gradlew :androidApp:testDebugUnitTest`
    - `./gradlew verifyKmpBaseline`
  - Result: successful on 2026-09-12.

### 6. Move Networking To Common

- [x] Move Ktor API interfaces, request construction, response mapping, DTOs, and
  serialization setup that have no platform dependency to `commonMain`.
- [x] Configure platform HTTP engines.
  - Android: OkHttp.
  - iOS: Darwin.
  - Desktop: CIO or Java engine.
  - Web/Wasm: supported Ktor web engine selected for the final web target.
- [x] Remove Android `Context` from API service implementations.
  - API service implementations now depend on shared session/upload/error
    abstractions instead of Android `Context`.
  - Use structured API errors or a domain-safe `StringResolver` boundary instead
    of resolving Android strings in data/network code.
- [x] Keep auth token injection and refresh behavior platform-neutral.
  - Secure token lookup must go through shared session/secure-storage
    interfaces.
  - Android encrypted storage remains the Android implementation.
- [x] Preserve all server wire shapes.
  - DTO serial names must match existing API payloads.
  - Multipart field names and response parsing must remain compatible.
- [x] Add or move tests.
  - DTO mapper tests.
  - Serialization round-trip tests for request/response DTOs.
  - Error mapping tests.
  - Token refresh/session-expiry tests.
- [x] Commit after verification.
  - Suggested message: `refactor(network): move ktor services to common`.
  - Completed with `verifyKmpBaseline` passing after moving HTTP services and
    Ktor setup to `composeApp/commonMain`.

Follow-up discovered:
- Move common fallback network error strings into CMP resources during resource
  and localization migration; Android still uses existing `R.string` values
  through its network error message provider.

### 7. Complete Resource And Localization Migration

- [x] Move shared UI strings to Compose Multiplatform resources.
  - Replace common/shared usages of Android `R.string` and
    `androidx.compose.ui.res.stringResource`.
  - Keep Android resource access only in Android shell/platform code.
  - Added CMP resources for the shared string-key subset and Persian
    translations.
  - Replaced shared/default network and upload validation messages with stable
    shared string keys.
  - Follow-up: Android-only composables still use `R.string` and
    `androidx.compose.ui.res.stringResource`; migrate each screen's resources
    with the screen during step 8.
- [x] Move shared drawable/vector assets to CMP resources.
  - Replace common/shared usages of Android `R.drawable`, `R.mipmap`, and
    `androidx.compose.ui.res.painterResource`.
  - Keep launcher icons, notification icons, XML provider metadata, and
    Android-only service assets in Android resources.
  - Current audit found no shared/common drawable resources to move; Android
    drawables remain Android-only until their screens move.
- [x] Replace domain models that expose Android resource IDs.
  - Current examples include `StringResourceContent`, `AppTheme`, and
    `AppLanguage`.
  - Use stable keys/enums/domain text descriptors instead of Android resource
    IDs.
  - `StringResourceContent` now stores stable `AppStringKey` values instead of
    Android integer resource IDs.
  - `AppTheme` and `AppLanguage` remain stable enums so existing preference
    values are preserved.
- [x] Preserve current localization behavior.
  - Keep Persian/RTL behavior working.
  - Keep Android locale application in the Android shell until shared resource
    locale handling is fully validated.
  - Android string resolution maps keys back to existing Android resources.
  - Android locale application remains in the Android shell.
- [x] Add UI/resource compile checks for all configured targets.
- [x] Commit after verification.
  - Commit message: `refactor(resources): migrate shared strings to cmp`.
  - Verified commands:
    - `./gradlew checkDomainBoundaries`
    - `./gradlew :composeApp:compileKotlinMetadata`
    - `./gradlew :composeApp:desktopTest`
    - `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinDesktop :composeApp:compileKotlinWasmJs`
    - `./gradlew :androidApp:compileDebugKotlin`
    - `./gradlew :androidApp:testDebugUnitTest`
    - `./gradlew verifyKmpBaseline`
  - Result: successful on 2026-09-13.

### 8. Move Compose UI Feature By Feature

- [x] Start with platform-light screens.
  - Recommended first candidates: error/no-internet/update/basic notification
    screens, then auth/profile/search screens.
  - Completed first slice: platform-light shared content remains in
    `:composeApp`, and auth/update/core dialog state holders plus their
    common-safe tests moved to `:composeApp`.
  - Follow-up: move Android route wrappers only after shared state-holder
    creation/navigation adapters are available for non-Android targets.
  - Follow-up: move chat, group chat, story editor/viewer, music, media picker,
    profile edit, media viewer, and playback screens later; they still depend on
    Android permissions, activity results, `Uri`, lifecycle, keyboard/window,
    Coil, Media3, filesystem, or platform media adapters.
- [ ] For each moved screen, remove shared composable dependencies on:
  - `LocalContext`.
  - Android `Toast`.
  - Android activity result APIs.
  - Android permission APIs.
  - Android lifecycle observers.
  - Android resource IDs.
  - Android `Uri`.
  - Media3/ExoPlayer views.
  - Android keyboard/window APIs.
- [ ] Keep Android edges as adapters.
  - Permission launchers, pickers, file providers, notification permission,
    system bars, keyboard/window control, and activity lifecycle handling remain
    in `:androidApp` or platform source sets.
- [ ] Keep shared UI dependent on state, callbacks, route commands, CMP
  resources, and platform wrapper interfaces only.
- [ ] Verify every moved feature on Android and through all target compile tasks.
- [ ] Commit each feature migration separately.
  - Suggested message format:
    `refactor(cmp): move <feature> ui to composeApp`.
  - Current feature commit target:
    `refactor(cmp): move platform-light ui state to composeApp`.

### 9. Finalize Persistence

- [ ] Keep Room KMP as the selected database strategy.
- [ ] Move database declarations, entities, DAOs, and migration SQL only after
  the module split and source sets are stable.
- [ ] Preserve the current Android database contract.
  - Database name: `chat_db`.
  - Current schema version: `13`.
  - Existing migration SQL must remain compatible.
- [ ] Keep domain models separate from Room entities.
  - Private message and group message local entities must remain data-layer
    persistence models.
  - Repositories map local entities to domain models.
- [ ] Add platform database builders.
  - Android builder keeps the existing database location.
  - iOS, desktop, and web/wasm storage strategy must be explicit before enabling
    persistence on those targets.
- [ ] Add migration and repository tests.
  - Private messages.
  - Group messages.
  - Socket outbound queue.
  - Crash log queue.
  - Notification/message dedupe stores.
  - Schema migration from existing Android installs.
- [ ] Commit after verification.
  - Suggested message: `refactor(database): move room schema to composeApp`.

### 10. Finalize Preferences, Secure Storage, And Crypto

- [ ] Keep existing Android preference filenames, keys, defaults, and encrypted
  storage behavior.
- [ ] Add platform secure storage implementations.
  - Android: current encrypted storage or vetted successor.
  - iOS: Keychain.
  - Desktop: OS keychain or encrypted file strategy.
  - Web/Wasm: explicit browser storage strategy with documented security limits.
- [ ] Keep sensitive data behind secure/session interfaces.
  - Auth token.
  - Refresh token.
  - Private keys.
  - Pending registration keys.
  - Device sync state.
  - Theme, language, onboarding, and session flags.
- [ ] Add non-Android crypto implementations or adopt a vetted KMP crypto
  library.
  - Preserve RSA 2048 key generation/import/export.
  - Preserve RSA OAEP SHA-256/MGF1 SHA-256 v2 payload compatibility.
  - Preserve AES-GCM `iv + ciphertext/tag` compatibility.
  - Preserve legacy RSA PKCS#1/AES fallback decryption.
  - Preserve public-key parsing formats currently supported by Android/JVM.
- [ ] Keep golden-vector tests in common.
  - Private-message round trips.
  - Group-message recipient key round trips.
  - Legacy payload decrypt.
  - Public-key import/export formats.
  - Stable debug fingerprints.
- [ ] Commit after verification.
  - Suggested message: `feat(kmp): add multiplatform secure storage`.

### 11. Finalize Realtime Socket

- [ ] Keep current Android Socket.IO behavior behind `RealtimeSocketTransport`.
  - Preserve `/socket.io/` auth, websocket/polling behavior, reconnection policy,
    event names, and payload shapes unless the server protocol changes.
- [ ] Choose and implement non-Android realtime transports.
  - Option A: platform-specific Socket.IO actuals.
  - Option B: Ktor WebSockets if the server supports a compatible protocol.
  - Do not silently switch protocols without server compatibility validation.
- [ ] Keep outbound queue persistence behind shared contracts.
  - Shared code owns queue policy and payload encoding.
  - Platform code owns durable storage.
- [ ] Add tests.
  - Event mapping.
  - Payload serialization/deserialization.
  - Retry/backoff policy.
  - Queue persistence behavior.
  - Reconnect and auth-refresh behavior.
- [ ] Commit after verification.
  - Suggested message: `feat(kmp): add multiplatform realtime transport`.

### 12. Finalize Media, Uploads, And Files

- [ ] Keep platform-neutral upload/file handles in shared APIs.
  - Shared contracts may expose ID/URI string, display name, MIME type, size,
    byte reading, and lifecycle/cleanup hooks.
  - Shared contracts must not expose Android `Uri`, `ContentResolver`,
    `OpenableColumns`, `MimeTypeMap`, `MediaStore`, `FileProvider`, or
    `Environment`.
- [ ] Implement media picking per platform.
  - Android keeps Activity Result APIs and URI permission handling.
  - iOS uses native picker/file APIs.
  - Desktop uses desktop file chooser/file APIs.
  - Web/Wasm uses browser file input/blob APIs.
- [ ] Implement media metadata extraction per platform.
  - Preserve current duration, size, MIME, title/artist, and thumbnail behavior
    where supported.
  - Document target-specific unsupported metadata behavior.
- [ ] Keep upload validation common.
  - MIME policy.
  - Size policy.
  - Media category policy.
  - User-visible validation errors through domain/presentation-safe text models.
- [ ] Preserve multipart upload behavior for chat, group, profile, post, music
  post, and story flows.
- [ ] Commit feature areas separately.
  - Suggested message format:
    `refactor(upload): move <flow> upload source to common`.

### 13. Finalize Playback, Recording, Story Editing, And Background Work

- [ ] Keep playback state and queue logic shared.
  - Queue normalization.
  - Active index.
  - Progress/duration state.
  - Repeat/shuffle behavior.
  - Now-playing metadata.
- [ ] Implement actual playback per platform.
  - Android keeps Media3/ExoPlayer, service, audio focus, media session, cache,
    and foreground notification behavior.
  - iOS, desktop, and web/wasm use native or vetted target-appropriate playback
    APIs.
- [ ] Implement audio recording per platform.
  - Android keeps `MediaRecorder`, cache files, microphone permissions, and URI
    conversion at Android edges.
  - Other targets return the same neutral recorded-audio model.
- [ ] Keep story/video editing platform-specific until each target has an
  implementation.
  - Android Media3 Transformer remains Android-only.
  - Shared code owns editor state, validation, and upload intent models.
- [ ] Define background retry behavior per platform.
  - Android may use WorkManager/foreground services behind the scheduler.
  - iOS, desktop, and web/wasm need explicit lifecycle/background constraints.
  - Cover pending uploads, socket outbound retry, crash log upload, and any
    notification sync behavior.
- [ ] Commit after verification.
  - Suggested messages:
    - `feat(kmp): add multiplatform audio recorder contract`
    - `feat(kmp): add platform playback implementations`
    - `refactor(background): isolate retry scheduling by platform`.

### 14. Notifications, Crash Reporting, And Startup

- [ ] Keep Android notification implementation platform-specific.
  - Notification channels.
  - `NotificationManager`.
  - `PendingIntent`.
  - Runtime notification permission.
  - Foreground service notifications.
  - Android notification icons/resources.
- [ ] Share only platform-neutral notification intent and payload models.
- [ ] Add target-specific notification behavior only after product requirements
  are clear for iOS, desktop, and web/wasm.
- [ ] Keep crash capture and restart UI platform-specific.
  - Android keeps uncaught exception handler, crash activity, restart intents,
    Room-backed crash queue, and process kill/exit behavior.
  - Shared code may own crash payload construction, trimming, breadcrumbs, and
    upload mapping.
- [ ] Keep startup split clean.
  - Shared startup initializes common state and observers.
  - Android shell owns Koin startup, crash reporter installation, notification
    channel setup, Android music setup, locale application, permissions, and
    intent/deep-link consumption.
- [ ] Commit after verification.
  - Suggested message: `refactor(startup): separate shared and android startup`.

### 15. Remove Obsolete Dependencies And Stale Code

- [ ] Remove dependencies only after source imports are gone.
  - AndroidX Compose artifacts replaced by CMP equivalents where code is shared.
  - Android-only Koin artifacts kept only in Android source sets.
  - OkHttp Ktor engine kept only for Android/JVM targets that use it.
  - Socket.IO Java client kept only if Android transport still requires it.
  - Media3, AndroidX Security, WorkManager, Room Android artifacts, and Android
    test artifacts kept only in Android source sets.
- [ ] Remove stale comments and old migration wording after each area is moved.
- [ ] Remove security-sensitive debug logs.
  - Current TODOs in chat/group surfaces mention raw key samples in production
    logs; redact or remove those logs before release.
- [ ] Run dependency and import audits.
  - No Android imports in `commonMain`.
  - No data/presentation imports in domain.
  - No unused old generated navigation or Android-only UI utility dependencies.
- [ ] Commit after verification.
  - Suggested message: `chore(kmp): remove obsolete android dependencies`.

## Final Acceptance Criteria

- [ ] The project uses `:androidApp + :composeApp`.
- [ ] Android builds and existing Android behavior remains compatible.
- [ ] `:composeApp` compiles for Android, iOS, desktop, and web/wasm targets.
- [ ] Common tests pass.
- [ ] Android unit tests pass.
- [ ] Android instrumentation tests pass where required.
- [ ] Domain code has no data, presentation, Android, or AndroidX imports.
- [ ] Shared UI has no Android resource, context, lifecycle, permission, or
  activity-result dependencies.
- [ ] Existing database schema, secure storage keys, crypto payloads, route
  arguments, server wire shapes, and upload multipart behavior remain compatible.
- [ ] Every completed checklist item has a corresponding commit with an
  appropriate commit message.

## Useful References

- Kotlin Multiplatform:
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
- Koin:
  <https://insert-koin.io/docs/setup/>
- Coil:
  <https://coil-kt.github.io/coil/>
