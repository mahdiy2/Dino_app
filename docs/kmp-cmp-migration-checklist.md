# KMP / CMP Migration Checklist

SecMessage’s KMP/CMP migration is in progress. The goal is to run the real app
against the existing backend through shared Kotlin and Compose code, with each
platform shell supplying its own system integrations.

| Target | Current state |
| --- | --- |
| Android | `:androidApp` launches the full app. The signed-in navigation graph and several feature screens and state holders still live in Android code. |
| Linux desktop | The native Compose launcher opens the shared sign-in and account shell. The desktop session adapter passed live backend verification. |
| Chrome / Wasm | The Compose browser launcher opens the shared sign-in and account shell using a same-origin server session. |
| iOS | Kotlin/Native targets and platform adapters exist. A full app shell and macOS verification are pending. |
| macOS and Windows desktop | Runtime verification is deferred. Linux results apply to Linux. |

The Linux and Chrome launchers support account sign-in and session restoration. Full app acceptance also requires the feed, account, messaging, and media flows below.

## Remaining migration tasks

### 1. Shared app shell and navigation

- [x] Move the app root, signed-in home, startup routing, screen and modal
  navigation, and state-holder creation into shared code. Keep platform startup,
  lifecycle, permissions, and OS actions behind adapters.
- [x] Wire Android, native Linux, and Chrome to the shared root. Preserve
  Android route arguments, deep links, and notification destinations. Verify
  browser back, reload, and startup routing with and without a session.
- [x] Make Android Studio’s Linux and web Run configurations launch the shared
  app. Document local backend configuration for both targets and keep a
  repeatable test configuration for each.

The shared route graph and state-holder wiring are in place. The Android
compile and unit suite passed. The Chrome launcher passed live backend
session restoration, protected reload, browser history, and logout checks.
The Linux window opened, and the desktop adapter passed live sign-in,
encrypted session restoration, and logout tests. Backend session tests
covered cookie attributes, expiry, origin rejection, and revocation.

Android queues notification destinations until the shared graph is ready and a
session is active. The debug and test APKs built, Android unit tests passed, and
all nine instrumentation tests passed on API 29. The runtime smoke covered a
cold private-chat notification deferred through sign-in, a warm activity
notification, and a signed-in cold group URI. Back returned to the previous
screen and dismissed the Create Post sheet without leaving the feed.
Production cookie attributes and session invalidation were verified against a
disposable HTTPS backend. Backend session implementation and tests are in
`deab63a` in the backend repository.

### 2. Authentication and session lifecycle

- [ ] Connect shared splash, login, registration, private-key confirmation and
  recovery, and logout to real HTTP, settings, and secure-storage adapters.
  Wire session restoration and token refresh into each launcher.
- [ ] Verify valid and invalid sign-in, restart with a saved session, token
  refresh, account switching, and logout cleanup. Preserve existing Android
  storage keys, session behavior, and wire contracts.
- [ ] Define and implement browser key and session storage suitable for
  production use. Validate the policy for reload, logout, and key recovery.
  Evaluate native keychain integration for the Linux encryption master key.

### 3. Feed, search, and account flows

- [ ] Port the signed-in feed, thread and shared-post details, comments,
  reporting, and post deletion. Connect live user and post search to the
  backend and route real results to profiles and posts.
- [ ] Port profiles and editing, followers and following, block list, settings,
  referrals, and About. Connect shared content to reusable state holders and
  preserve loading, empty, error, and retry behavior.
- [ ] Wire the activity-notification list, unread state, replies, follow-back,
  and destination routing into the shared shell.

### 4. Private and group messaging

- [ ] Port conversation lists, private and group chats, group creation and
  member management, attachment composition, and media viewing. Move
  reusable state holders to common code with read-only state and one-shot
  events. Replace JVM-only helpers in state holders being migrated.
- [ ] Connect the existing HTTP and realtime adapters, cached history,
  delivery and read state, and durable outbound queues to the launchers.
  Use Room where supported and define browser persistence for chat history
  and queued operations.
- [ ] Verify encrypted private and group messages across Android, Linux,
  and Chrome. Cover legacy ciphertext, attachments, reconnect, auth refresh,
  offline resend, duplicate suppression, and session cleanup.

### 5. Posts, stories, and media

- [ ] Port post creation and editing, story viewing and editing, audio-post
  creation, music queues and playlists, and media playback screens. Connect
  them to the existing pickers, metadata readers, recording and playback
  adapters, and platform media renderers.
- [ ] Integrate upload progress, cancellation, retries, and restart recovery
  wherever supported. Verify private and group attachments, profile media,
  posts, and stories against the existing multipart and replay contracts.
- [ ] Record platform capability limits and decide the supported fallback for
  permission denial, unsupported media, and interrupted uploads before
  accepting each affected flow.

### 6. Platform behavior and release verification

- [ ] Complete Android notification tap routing across cold and warm starts,
  API 33+ permission denial and grant, and crash queue and restart checks.
- [ ] Define the OS-level notification, background work, and crash-reporting
  requirements for Linux, Chrome, and iOS. Implement and verify the required
  platform adapters.
- [ ] Run end-to-end checks against a local backend on Android, native Linux,
  and Chrome. Verify signup and sign-in, feed and search, account operations,
  private and group messages, media, and logout through each normal launcher.

### 7. Deferred platform bring-up

- [ ] Resolve the Kotlin/Native and Room ABI mismatch. Add an iOS host that
  starts the shared app and wires iOS storage, networking, and platform
  services. Verify startup, Keychain, Room, crypto, realtime, and media on
  macOS using an iOS simulator and a device where required.
- [ ] Run the desktop build and end-to-end checks on macOS and Windows.
  Track these results independently of Linux acceptance.

## Completion gates

- [ ] Verify the real app through the Android, Linux, and Chrome launchers.
  Record per-target results and any remaining platform-specific gaps.
- [ ] Verify shared compilation and required tests on every claimed target.
  Record the exact host, target, and test environment for each result.
- [ ] Confirm compatibility with existing database migrations, secure-storage
  keys, crypto payloads, route arguments, server wire shapes, and multipart
  uploads. Keep shared UI and domain boundary checks passing.
- [ ] Associate every completed task with a scoped commit and verification
  evidence.

## Completed foundations and evidence

- **Module and architecture groundwork.** The KMP project uses
  `:androidApp` and `:composeApp`, with shared contracts for navigation,
  networking, persistence, crypto, and platform effects. Android domain and
  shared UI boundary checks passed. See `f6b4c68`, `4b72c84`, and `4d18fdb`.
- **Shared UI and state.** Auth screens and several dialogs are shared.
  Search, activity notifications, group pickers and member lists, and other
  platform-light screens have shared content. The Android route graph still
  supplies the feature wrappers and orchestration. See `4bbf44c`.
- **Persistence, crypto, realtime, and media.** Shared services and platform
  adapters landed, including Room, secure storage, Socket.IO transports,
  multipart uploads, media pickers, playback, recording, and retries.
  See `784f46e`, `4ab6f56`, `a3c8a61`, and `38dd49d`.
  [Media and uploads](kmp-media-uploads.md) ·
  [Media runtime, retries, and verification](kmp-media-runtime.md)
- **Android startup and dependency cleanup.** Task 14’s implementation was
  committed in `22e3a5c`. Task 15 removed obsolete dependencies and sensitive
  logging in `2a199bc`. The instrumentation runner was restored in `4cd68e9`.
- **Recorded verification.** The Linux and Android pass in `2cc51c8` recorded
  Android debug and instrumentation APK builds, 56 Android unit tests, nine
  API 29 instrumentation tests, a passing Linux desktop suite, and 73
  Chrome/Wasm tests. The focused local Socket.IO fixture and three upload
  replay tests passed. Android cold startup and opening the crash report
  screen were observed. The checklist above tracks the remaining runtime
  scenarios.
- **Launcher verification.** Linux and Chrome now open the shared account
  shell. The original Search preview served as initial launcher verification.
  [Android Studio launch instructions](../README.md#run-linux-and-web-in-android-studio)

The original 15-task tracker and its detailed command history are available
in Git at `22e3a5c:docs/kmp-cmp-migration-checklist.md`.
