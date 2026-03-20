# Android Debug Socket Fix Guide

This guide explains exactly how the Android app should connect to the backend Socket.IO service in the current VPS setup.

## Current backend status

- Socket.IO is available on:
  - `http://back.dinosocial.ir`
- Socket path:
  - `/socket.io/`
- Auth method:
  - send JWT access token in `auth`
- Server supports:
  - `websocket`
  - `polling`

Because the SSL certificate for `back.dinosocial.ir` is not valid right now, Android must use **HTTP in debug builds only**.

## Required Android changes

### 1) Use this base URL

```text
http://back.dinosocial.ir
```

Do not use `https://back.dinosocial.ir` for now.

## 2) Allow cleartext traffic in debug

In `AndroidManifest.xml`, inside the `<application>` tag:

```xml
android:usesCleartextTraffic="true"
android:networkSecurityConfig="@xml/network_security_config"
```

## 3) Add network security config

Create this file:

`app/src/main/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">back.dinosocial.ir</domain>
    </domain-config>
</network-security-config>
```

## 4) Socket.IO client config

Recommended Kotlin configuration:

```kotlin
import io.socket.client.IO
import io.socket.client.Socket

fun createMessagingSocket(accessToken: String): Socket {
    val options = IO.Options.builder()
        .setAuth(mapOf("token" to accessToken))
        .setTransports(arrayOf("websocket", "polling"))
        .setReconnection(true)
        .setReconnectionAttempts(Int.MAX_VALUE)
        .setReconnectionDelay(1000)
        .setReconnectionDelayMax(10000)
        .setTimeout(20000)
        .build()

    return IO.socket("http://back.dinosocial.ir", options)
}
```

Notes:
- Do not force only `websocket` right now.
- Keep both `websocket` and `polling` enabled.
- Always send the JWT in `auth` as `token`.

## 5) Connect and listen for events

```kotlin
val socket = createMessagingSocket(accessToken)

socket.on(Socket.EVENT_CONNECT) {
    // connected to socket server
}

socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
    // log args for debugging
}

socket.on(Socket.EVENT_DISCONNECT) { args ->
    // handle disconnect
}

socket.on("connected") { args ->
    // {"username":"alice"}
}

socket.on("pending_messages") { args ->
    // queued offline messages
}

socket.on("new_message") { args ->
    // live incoming message
}

socket.on("message_sent") { args ->
    // sender ack
}

socket.on("message_error") { args ->
    // validation/auth error
}

socket.on("user_status") { args ->
    // presence updates
}

socket.connect()
```

## 6) Send a message

```kotlin
val payload = org.json.JSONObject()
    .put("to", "bob")
    .put("type", "text")
    .put("message", encryptedMessage)
    .put("encrypted_key", encryptedKey)

socket.emit("send_message", payload)
```

## 7) Get user status

```kotlin
socket.emit(
    "get_user_status",
    org.json.JSONObject().put("username", "bob")
)
```

## 8) Authentication rules

The backend expects the access token during socket connection:

```json
{"token":"<access_token>"}
```

If token is missing or invalid, connection will be rejected.

## 9) Attachment flow

For image/file/voice/video messages:

1. Upload file first with HTTP:
   - `POST /api/messages/attachments`
2. Use returned `attachment` object inside socket payload.
3. Then emit `send_message`.

## 10) Debug troubleshooting

### If `xhr poll error` still happens

Check these items:
- base URL is exactly `http://back.dinosocial.ir`
- app allows cleartext traffic
- transports include both `websocket` and `polling`
- access token is valid
- socket path is default `/socket.io/`
- mobile network/firewall is not blocking the domain

### If connect fails with unauthorized

- token expired
- token not passed in `auth`
- wrong token type

### If sender connects but receiver gets nothing

- receiver may be offline
- check `pending_messages` after reconnect
- verify both users are connected with valid tokens

## 11) Important note for production

This HTTP setup is for **debug/testing only**.

For production, the app should go back to:
- `https://back.dinosocial.ir`
- with a valid SSL certificate that includes `back.dinosocial.ir`

