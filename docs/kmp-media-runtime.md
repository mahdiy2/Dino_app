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
- Media uploads currently run from the submitting screen. Android does not
  yet resume media submissions after process death.
- iOS can suspend app code in the background. Desktop retry code can run
  while its process is open. Browsers can throttle or suspend background tabs
  and stop app code at tab close. Persisted socket queue entries can resume
  after the host recreates and connects its gateway. This module does not
  install OS jobs for media, crash reports, or notifications on these targets.
