# Playback, recording, and background behavior

The shared playback controller maintains queue position and playback state.
Playback engines feed it duration, position, buffering, errors, and end-of-track
events. Create the engine and controller on the host UI thread and dispose the
controller when its owner closes. Android continues to use its Media3 service
for music and Media3 player surfaces for video.

| Target | Playback and recording |
| --- | --- |
| Android | Media3 handles playback, audio focus, and background controls. MediaRecorder writes AAC/M4A to cache after the chat screen grants microphone permission. |
| iOS | AVPlayer handles playback. Pass an AVPlayerViewController when a video surface is needed. The host must supply a main-thread coroutine scope and an `NSMicrophoneUsageDescription` entry in its Info.plist. AVAudioRecorder returns AAC/M4A. |
| Desktop | The Desktop runtime includes the OpenJFX native artifacts for the build host. Create the player on the Swing event thread and embed its video surface when requested. Java Sound records WAV using the OS-selected microphone. |
| Web/Wasm | Use a browser audio element or attach a video element to a host ID. Call play and start recording from a user gesture. Capture requires a secure context and permission. The WAV fallback also requires AudioWorklet support and a content security policy that permits its Blob module. |

Stop hands ownership of the recorded file to the caller. Call `release` after
its final upload attempt or when discarding the recording. The Android story
editor shares its undo history, draft models, and upload validation. Only
Android currently provides story preview and export integration.

## Retry and lifecycle

- Android uses a network-constrained WorkManager job for persisted crash-log
  uploads. Socket sends retain their existing outbox and reconnect behavior.
  Notification events continue to use the existing notification socket.
- Android stages private and group attachments (including voice), profile
  media, post creation and editing, music posts, and stories in app-private
  storage before queuing network-constrained WorkManager jobs. The outbox
  stores the operation ID, account session, file metadata, and submitted form.
  On restart it reschedules pending jobs and removes abandoned staging files.
  Logout invalidates the session and cancels and removes matching jobs.
- The Flask server must advertise `replay_version: 1` from
  `GET /api/messages/media-submission-capabilities` before a worker writes.
  Deploy this server change across all instances before publishing the client.
  Each write uses the stored `Idempotency-Key` and confirms the server replay
  version. An online client that detects an older server uses its existing
  foreground submission path. A job queued offline waits for connectivity
  before it can verify server support.
- Settings → Media submissions shows pending, completed, and failed jobs and
  allows cancellation or a manual retry after transient attempts are exhausted.
  Cancellation stops client retries. An HTTP request already accepted by the
  server may still finish. Non-retryable failures, completion, and cancellation
  release staged files.
- Attachment completion means the resulting message was persisted to the
  socket outbox. The original Send action resumes without a conversation
  screen, using the same client message ID if handoff repeats. The server
  deduplicates socket messages by sender, destination, and that ID.
- iOS can suspend app code in the background. Desktop retry code can run
  while its process is open. Browsers can throttle or suspend background tabs
  and stop app code at tab close. Persisted socket queue entries can resume
  after the host recreates and connects its gateway. Attach
  `RealtimeRetryLifecycle` to the host's lifecycle dispatcher, enable background
  suspension on iOS and Wasm, and forward connectivity restoration through
  `networkAvailable()`. Desktop can keep processing while the app is running.
  The host owns adapter disposal. This module does not install OS jobs for
  media, crash reports, or notifications on these targets.

## Verification

- The Flask replay tests exercise private and group uploads, profile media,
  post creation and editing, music posts, and stories. They cover replayed
  response bodies, conflicting content, concurrent requests, and a failed
  attempt retried with the same storage key. The full API suite reported 11
  failures. The same 11 reproduced on the backend baseline with the same
  local settings.
- Android unit tests cover offline staging, rescheduling after the outbox is
  recreated, a lost response, attachment handoff retry, logout cleanup, and
  fallback to foreground uploads on an older server. The Android instrumentation
  APK compiled. The software emulator terminated during APK optimization, so
  Room database reopen, playback service progress, microphone capture, and
  Transformer export remain unverified on an Android device. Task 13 uses the
  approved Linux verification scope.
- Run the JavaFX smoke test with `DINO_MEDIA_SMOKE=1` and an available desktop
  display. The browser smoke tests cover playback metadata and PCM capture
  through an AudioWorklet. The recording test runs when Chrome's user agent
  includes `DinoMediaSmoke`. Launch test Chrome with a synthetic microphone
  and the fake permission prompt flags.
- The iOS adapters and smoke tests were type-checked with Kotlin/Native's
  `ios_simulator_arm64` platform libraries on Linux. iOS runtime testing was
  outside this verification pass. On macOS, set
  `SIMCTL_CHILD_DINO_MEDIA_SMOKE=1` for the simulator playback test. To run the
  microphone smoke test, also set `SIMCTL_CHILD_DINO_MEDIA_MIC_SMOKE=1` and use
  a test host with a microphone usage description and an available input.
