# Media uploads in shared code

Feature use cases accept `ReadableMediaFile` (directly or through
`CreatePostMedia` or `ProfileUpdateImage`). A handle carries the display name,
MIME type, size when known, and an opaque identifier. Treat the identifier as
descriptive. Use `openInput` to read the file. Each call opens a fresh input for
request retries. Keep the handle while a draft or upload owns it and call
`release` when it is discarded or the upload and its retries finish.

Invoke the platform's `UploadMediaPicker` on the UI thread. The browser picker
must run directly from a user gesture. A successful null result means the user
cancelled. The category filter guides selection. Shared upload policies still
validate files before sending a request.

| Target | Picker and metadata |
| --- | --- |
| Android | Existing screens use their Activity Result contracts and URI grants. `rememberAndroidUploadMediaPicker` uses the document picker and retains read access when the provider supports persistable grants. Content providers supply MIME and size. MediaMetadataRetriever supplies duration and embedded title/artist, and provider thumbnails are available where supported. |
| iOS | `IosUploadMediaPicker` presents a document picker from the host view controller and streams the imported copy. AVFoundation reads available duration and common title/artist tags and generates video frames. UIKit generates image thumbnails. |
| Desktop | `DesktopUploadMediaPicker` uses Swing and local file streams. ImageIO produces thumbnails for installed image readers. Java Sound supplies audio duration and tags when an installed reader exposes them. Desktop has no bundled video metadata decoder. |
| Web/Wasm | `BrowserUploadMediaPicker` reads the selected Blob into memory and reopens it for retries. Browser media elements report duration and canvas renders supported images and video frames. Browser APIs do not expose embedded title/artist tags. Files larger than the addressable byte array are rejected. |

Missing metadata is represented by null. Duration, tags and thumbnails depend
on the provider and the target's codec support. MIME resolution falls back to
the filename where appropriate. Legacy Android thumbnail rendering continues
to use the existing screen-specific loaders.

Multipart names stay the same. Chat and group use `file`, profile uses
`profile_image` and `profile_video`, post and music post use `media[]`, and
story uses `file` and repeated `mentions[]`. Music posts retain the
`track_title` and `track_artist` fields.
