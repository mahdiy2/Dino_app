# Android Integration Guide — Notifications & Contact Message Status

No Firebase required. This uses **Socket.IO** for real-time delivery and
**WorkManager polling** for background notifications.

---

## 1. Overview

| Feature | How it works |
|---|---|
| **Real-time messages** | Socket.IO `new_message` event (already existed) |
| **Background notifications** | Android `WorkManager` polls `GET /api/notifications/unread` periodically and shows a local notification |
| **Contact message status** | `GET /api/contacts?detailed=true` returns `has_message`, `online`, `last_message` per contact |
| **Real-time contact status** | Socket.IO `get_contacts_status` → `contacts_status` event |

---

## 2. New Backend Endpoints

### 2.1 Unread Messages Summary (for polling)

```
GET /api/notifications/unread
Authorization: Bearer <jwt>
```

**Response:**
```json
{
  "total": 3,
  "senders": [
    {
      "sender": "alice",
      "count": 2,
      "last_type": "text",
      "last_timestamp": "2025-01-15T12:30:00.000000"
    },
    {
      "sender": "bob",
      "count": 1,
      "last_type": "image",
      "last_timestamp": "2025-01-15T12:25:00.000000"
    }
  ]
}
```

This endpoint does **not** consume messages — it only peeks at the inbox.
Messages are consumed when the user connects via Socket.IO (existing
`pending_messages` event).

### 2.2 Contact List with Message Status

```
GET /api/contacts?detailed=true
Authorization: Bearer <jwt>
```

**Response:**
```json
{
  "contacts": [
    {
      "username": "alice",
      "has_message": true,
      "online": false,
      "last_message": {
        "from": "alice",
        "type": "text",
        "timestamp": "2025-01-15T12:30:00.000000"
      }
    },
    {
      "username": "bob",
      "has_message": false,
      "online": true,
      "last_message": null
    }
  ]
}
```

Without `?detailed=true` the endpoint returns the original plain list of
usernames (backward-compatible).

### 2.3 Socket.IO Events (new)

| Emit | Receive | Description |
|---|---|---|
| `get_contacts_status` | `contacts_status` | Returns full contact list with `has_message` / `online` / `last_message` |

---

## 3. Android — Background Notification via WorkManager

### 3.1 Add WorkManager dependency

```groovy
// app/build.gradle
dependencies {
    implementation "androidx.work:work-runtime-ktx:2.10.1"
}
```

### 3.2 Create the polling Worker

```kotlin
class UnreadMessagesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = getJwtToken() ?: return Result.success()

        try {
            val response = apiService.getUnread(token)  // GET /api/notifications/unread
            if (response.total > 0) {
                showNotification(response)
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    private fun showNotification(response: UnreadResponse) {
        val channelId = "messages"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // Create channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // One notification per sender
        for (sender in response.senders) {
            val body = when (sender.lastType) {
                "image" -> "Sent you an image"
                "video" -> "Sent you a video"
                "voice" -> "Sent you a voice message"
                "file"  -> "Sent you a file"
                "mixed" -> "Sent you a message with attachment"
                else    -> if (sender.count > 1) "${sender.count} new messages"
                           else "Sent you a message"
            }

            val intent = Intent(applicationContext, ChatActivity::class.java).apply {
                putExtra("username", sender.sender)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                sender.sender.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle(sender.sender)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(sender.sender.hashCode(), notification)
        }
    }

    private fun getJwtToken(): String? {
        // Read from SharedPreferences or your auth store
        val prefs = applicationContext.getSharedPreferences("auth", Context.MODE_PRIVATE)
        return prefs.getString("jwt_token", null)
    }
}
```

### 3.3 Data classes for the response

```kotlin
data class UnreadResponse(
    val total: Int,
    val senders: List<SenderSummary>
)

data class SenderSummary(
    val sender: String,
    val count: Int,
    @SerializedName("last_type")
    val lastType: String,
    @SerializedName("last_timestamp")
    val lastTimestamp: String
)
```

### 3.4 Retrofit interface

```kotlin
interface ApiService {
    @GET("api/notifications/unread")
    suspend fun getUnread(
        @Header("Authorization") token: String
    ): UnreadResponse

    @GET("api/contacts")
    suspend fun getContacts(
        @Header("Authorization") token: String,
        @Query("detailed") detailed: Boolean = false
    ): ContactsResponse
}
```

### 3.5 Schedule the Worker

Call this once after login:

```kotlin
fun scheduleUnreadPolling(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val request = PeriodicWorkRequestBuilder<UnreadMessagesWorker>(
        15, TimeUnit.MINUTES   // minimum interval for periodic work
    )
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
        .build()

    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork(
            "unread_messages_poll",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
}
```

Cancel on logout:

```kotlin
fun cancelUnreadPolling(context: Context) {
    WorkManager.getInstance(context)
        .cancelUniqueWork("unread_messages_poll")
}
```

### 3.6 Create notification channel on app startup

In your `Application` class:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "messages", "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
```

---

## 4. Android — Contact List with Badges

### 4.1 ViewModel

```kotlin
data class ContactItem(
    val username: String,
    val hasMessage: Boolean,
    val online: Boolean,
    val lastMessageType: String?,
    val lastMessageTime: String?
)

class ContactsViewModel : ViewModel() {
    private val _contacts = MutableLiveData<List<ContactItem>>()
    val contacts: LiveData<List<ContactItem>> = _contacts

    fun loadContacts(apiService: ApiService, token: String) {
        viewModelScope.launch {
            val response = apiService.getContacts(token, detailed = true)
            _contacts.value = response.contacts.map {
                ContactItem(
                    username = it.username,
                    hasMessage = it.hasMessage,
                    online = it.online,
                    lastMessageType = it.lastMessage?.type,
                    lastMessageTime = it.lastMessage?.timestamp
                )
            }
        }
    }

    fun refreshViaSocket(socket: Socket) {
        socket.emit("get_contacts_status")
    }
}
```

### 4.2 Socket.IO listener

```kotlin
socket.on("contacts_status") { args ->
    val data = args[0] as JSONObject
    val contacts = data.getJSONArray("contacts")
    // Parse and update your LiveData / StateFlow
}
```

### 4.3 When to refresh

| Trigger | Action |
|---|---|
| App opened / resumed | `GET /api/contacts?detailed=true` |
| Socket `new_message` received | `socket.emit("get_contacts_status")` |
| Navigate to contacts screen | `GET /api/contacts?detailed=true` |
| Socket reconnected | `socket.emit("get_contacts_status")` |

---

## 5. Full Flow Diagram

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│  Android A   │         │   Backend    │         │  Android B   │
│  (sender)    │         │  (Flask+WS)  │         │  (receiver)  │
└──────┬──────┘         └──────┬──────┘         └──────┬──────┘
       │                       │                       │
       │  socket:              │                       │
       │  send_message ───────►│                       │
       │                       │                       │
       │                       │── Is B online? ──►    │
       │                       │                       │
       │                       │  YES: emit            │
       │                       │  "new_message" ──────►│ (instant)
       │                       │                       │
       │                       │  NO: persist to       │
       │                       │  Redis inbox          │
       │                       │                       │
       │◄── message_sent ─────│                       │
       │                       │                       │
       │                       │  B's WorkManager      │
       │                       │  polls every 15min:   │
       │                       │◄─ GET /unread ───────│
       │                       │── {total:1,..} ─────►│ → local notification
       │                       │                       │
       │                       │  B opens app:         │
       │                       │◄── socket connect ───│
       │                       │── pending_messages──►│ (messages consumed)
       │                       │                       │
       │                       │  B checks contacts:   │
       │                       │◄── GET /contacts ────│
       │                       │    ?detailed=true     │
       │                       │── contacts with ────►│
       │                       │   has_message flags   │
```

---

## 6. Quick Checklist

### Backend
- [ ] No extra dependencies or config needed (already deployed)
- [ ] Restart backend to pick up new endpoints

### Android
- [ ] Add WorkManager dependency
- [ ] Create `UnreadMessagesWorker` (see section 3.2)
- [ ] Schedule polling after login (see section 3.5)
- [ ] Cancel polling on logout
- [ ] Create notification channel `"messages"` in `Application.onCreate`
- [ ] Use `GET /api/contacts?detailed=true` for contact badges
- [ ] Listen to socket `contacts_status` for real-time updates
- [ ] Request `POST_NOTIFICATIONS` permission (Android 13+)

### Android 13+ Notification Permission

```kotlin
// In your Activity
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
}
```
