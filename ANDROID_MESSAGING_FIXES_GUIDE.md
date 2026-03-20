# Android Messaging Fixes — Developer Guide

This document explains the backend changes to the messaging system and what
needs to change on the Android side to work correctly with them.

---

## What Was Broken (Server-Side — Already Fixed)

1. Messages were sometimes not persisted when the recipient was "online"
   (the server's online check was unreliable across workers).
2. Reading the inbox via HTTP deleted all messages, so socket connect found
   nothing.
3. Messages arriving during inbox read were silently lost (race condition).
4. Messages stored on the server never expired.

All of these are now fixed on the backend. But these fixes introduce two
things the Android client **must** handle:

- **Duplicate messages** — every message is now persisted to Redis AND sent
  live via socket. A connected user will receive the message twice: once via
  the `new_message` event (instant) and once via `pending_messages` on
  reconnect. The client must deduplicate by `message_id`.
- **New `message_id` field** — every message now has a UUID `message_id`.

---

## 1. Message Payload Changes

Every message (in `new_message`, `pending_messages`, and `GET /api/messages/inbox`)
now includes a `message_id` field:

```json
{
  "message_id": "550e8400-e29b-41d4-a716-446655440000",
  "from": "alice",
  "type": "text",
  "message": "<encrypted_content>",
  "encrypted_key": "<encrypted_key>",
  "attachment": null,
  "timestamp": "2025-01-15T12:30:00.000000"
}
```

The `message_sent` confirmation also includes it now:

```json
{
  "to": "bob",
  "message_id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-01-15T12:30:00.000000",
  "type": "text"
}
```

---

## 2. Deduplication (REQUIRED)

This is the most important change. Since messages are always persisted AND
sent live, the same message can arrive through multiple paths:

```
User sends message
    ├── Server saves to Redis inbox
    └── Server emits "new_message" via socket (instant)

Later, on reconnect:
    └── Server emits "pending_messages" with same message from Redis
```

### What to do

Keep a local set of seen `message_id` values and skip any message you've
already processed.

```kotlin
class MessageDeduplicator {
    private val seenIds = Collections.synchronizedSet(
        LinkedHashSet<String>()
    )

    companion object {
        private const val MAX_CACHE_SIZE = 5000
    }

    fun isDuplicate(messageId: String): Boolean {
        if (messageId in seenIds) return true
        seenIds.add(messageId)
        if (seenIds.size > MAX_CACHE_SIZE) {
            val iterator = seenIds.iterator()
            iterator.next()
            iterator.remove()
        }
        return false
    }
}
```

Use it when processing messages:

```kotlin
private val deduplicator = MessageDeduplicator()

// For "new_message" event
socket.on("new_message") { args ->
    val data = args[0] as JSONObject
    val messageId = data.getString("message_id")

    if (deduplicator.isDuplicate(messageId)) return@on

    // Process and display the message
    handleIncomingMessage(data)
}

// For "pending_messages" event (received on connect)
socket.on("pending_messages") { args ->
    val data = args[0] as JSONObject
    val messages = data.getJSONArray("messages")

    for (i in 0 until messages.length()) {
        val msg = messages.getJSONObject(i)
        val messageId = msg.getString("message_id")

        if (deduplicator.isDuplicate(messageId)) continue

        handleIncomingMessage(msg)
    }
}
```

### Persistence across app restarts

Optionally, persist seen IDs to a local database (Room) or SharedPreferences
so deduplication survives app restarts. This prevents re-showing messages
that were already displayed before the app was killed.

```kotlin
// Simple SharedPreferences approach
fun persistSeenIds(context: Context, ids: Set<String>) {
    context.getSharedPreferences("msg_dedup", Context.MODE_PRIVATE)
        .edit()
        .putStringSet("seen_ids", ids)
        .apply()
}

fun loadSeenIds(context: Context): Set<String> {
    return context.getSharedPreferences("msg_dedup", Context.MODE_PRIVATE)
        .getStringSet("seen_ids", emptySet()) ?: emptySet()
}
```

---

## 3. HTTP Inbox Endpoint Change

`GET /api/messages/inbox` **no longer deletes messages** by default. It only
peeks (reads without consuming).

| Call | Behavior |
|---|---|
| `GET /api/messages/inbox` | Returns messages, keeps them in server |
| `GET /api/messages/inbox?consume=true` | Returns messages AND deletes them from server |

**Recommendation:** Do NOT use `?consume=true` from Android. Let the socket
`connect` event handle consumption. The HTTP endpoint should only be used
for checking if there are pending messages (e.g. for WorkManager polling
notifications).

---

## 4. Message Lifecycle (How It Works Now)

```
1. Sender emits "send_message" via socket
       │
2. Server ALWAYS saves message to Redis (inbox:{recipient})
   └── Inbox key expires after 24 hours automatically
       │
3. Server emits "new_message" to recipient's socket room
       │
4a. Recipient is CONNECTED:
    ├── Receives "new_message" instantly → display it
    └── On next reconnect, receives same message in "pending_messages"
        → deduplicate by message_id, skip it

4b. Recipient is DISCONNECTED:
    └── Message waits in Redis inbox
        └── On next socket connect, server sends "pending_messages"
            with ALL stored messages, then deletes them from Redis
```

---

## 5. Socket Event Reference

### Events the client receives

| Event | When | Payload |
|---|---|---|
| `connected` | After successful socket auth | `{"username": "..."}` |
| `pending_messages` | On connect, if there are stored messages | `{"messages": [...]}` |
| `new_message` | When someone sends you a message in real-time | Single message object (see section 1) |
| `message_sent` | Confirmation that your message was delivered | `{"to": "...", "message_id": "...", "timestamp": "...", "type": "..."}` |
| `message_error` | When a send fails | `{"error": "..."}` |
| `user_status` | When a contact comes online/offline | `{"username": "...", "online": true/false}` |
| `contacts_status` | Response to `get_contacts_status` | `{"contacts": [...]}` |

### Events the client sends

| Event | Payload |
|---|---|
| `send_message` | `{"to": "...", "message": "...", "encrypted_key": "...", "type": "text"}` |
| `get_user_status` | `{"username": "..."}` |
| `get_contacts_status` | (no payload) |

---

## 6. Recommended Socket Reconnection Strategy

Android should handle socket disconnections gracefully. On reconnect, the
server automatically sends `pending_messages`, so the client just needs to
process them (with deduplication).

```kotlin
val options = IO.Options().apply {
    auth = mapOf("token" to jwtToken)
    reconnection = true
    reconnectionAttempts = Integer.MAX_VALUE
    reconnectionDelay = 1000
    reconnectionDelayMax = 30000
    timeout = 20000
}

socket = IO.socket(serverUrl, options)

socket.on(Socket.EVENT_CONNECT) {
    Log.d("Socket", "Connected — pending_messages will arrive automatically")
}

socket.on(Socket.EVENT_DISCONNECT) {
    Log.d("Socket", "Disconnected — will auto-reconnect")
}

socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
    Log.e("Socket", "Connection error: ${args[0]}")
}
```

---

## 7. Quick Checklist

- [ ] Parse `message_id` from all incoming messages (`new_message` and `pending_messages`)
- [ ] Add `MessageDeduplicator` class (section 2)
- [ ] Use deduplicator in both `new_message` and `pending_messages` handlers
- [ ] Persist seen IDs across app restarts (SharedPreferences or Room)
- [ ] Stop using `GET /api/messages/inbox` to consume messages (or remove it)
- [ ] Store `message_id` from `message_sent` confirmation for sent message tracking
- [ ] Test: send message while recipient is online → should appear once
- [ ] Test: send message while recipient is offline → should appear once on reconnect
- [ ] Test: send multiple messages offline → all should appear on reconnect, no duplicates
