package com.dino.message.chatfeature.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.BuildConfig
import com.dino.message.DinoOrderApplication
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.chatfeature.domain.usecase.ClearMessageFromDBUseCase
import com.dino.message.chatfeature.domain.usecase.GetMessageFromDBUseCase
import com.dino.message.chatfeature.domain.usecase.GetReceiverPublicKeyUseCase
import com.dino.message.chatfeature.domain.usecase.SaveMessageInDbUseCase
import com.dino.message.chatfeature.domain.usecase.UploadAttachmentUseCase
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.CryptoUtil
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.util.decodePrivateKey
import com.dino.message.corefeature.presentation.util.decodePublicKey
import com.dino.message.corefeature.presentation.util.decryptMessage
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.ArrayDeque
import com.dino.message.chatfeature.domain.util.MessageDeduplicator
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val spref: SPrefManager,
    private val getReceiverPublicKeyUseCase: GetReceiverPublicKeyUseCase,
    private val getMessageFromDBUseCase: GetMessageFromDBUseCase,
    private val saveMessageInDbUseCase: SaveMessageInDbUseCase,
    private val clearMessageFromDBUseCase: ClearMessageFromDBUseCase,
    private val uploadAttachmentUseCase: UploadAttachmentUseCase,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val receiverPublicKey = mutableStateOf<String?>(null)

    private val _messages = mutableStateListOf<MessageEntity>()
    val messages: List<MessageEntity> = _messages

    private val _socketDebugEvents = MutableStateFlow<List<String>>(emptyList())
    val socketDebugEvents: StateFlow<List<String>> = _socketDebugEvents.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isPeerOnline = MutableStateFlow(false)
    val isPeerOnline: StateFlow<Boolean> = _isPeerOnline.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private var voiceRecorder: com.dino.message.chatfeature.presentation.component.VoiceRecorderManager? = null
    private var recordingTimerJob: kotlinx.coroutines.Job? = null

    private val _isPublicKeyLoading = MutableStateFlow(true)
    val isPublicKeyLoading: StateFlow<Boolean> = _isPublicKeyLoading.asStateFlow()

    private val _publicKeyError = MutableStateFlow<String?>(null)
    val publicKeyError: StateFlow<String?> = _publicKeyError.asStateFlow()

    private val _isPublicKeyReady = MutableStateFlow(false)
    val isPublicKeyReady: StateFlow<Boolean> = _isPublicKeyReady.asStateFlow()

    private var getPublicKey = false
    private var isSocketConnecting = false
    private var hasSocketConnectionStarted = false
    private var socket: Socket? = null
    private val pendingOutgoingMessages = ArrayDeque<String>()

    private val deduplicator = MessageDeduplicator(app)
    private val messagesMutex = Mutex()

    init {
        loadMessages()
        prepareEncryptionSession()
    }

    private fun pushSocketDebugLog(message: String) {
        val now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) java.time.LocalTime.now().toString() else "time"
        val updated = (_socketDebugEvents.value + "[$now] $message").takeLast(40)
        _socketDebugEvents.value = updated
    }

    private fun prepareEncryptionSession() = viewModelScope.launch {
        pushSocketDebugLog("HTTP get public key for $chatId")
        val publicKeyLoaded = getReceiverPublicKey()
        if (publicKeyLoaded) {
            connectSocketIfNeeded()
        }
    }

    private suspend fun getReceiverPublicKey(): Boolean {
        var isPublicKeyLoaded = false

        getReceiverPublicKeyUseCase.execute(chatId).collect { res ->
            when (res) {
                is Resource.Success -> {
                    val publicKey = res.data.public_key
                    if (publicKey.isNullOrBlank()) {
                        Log.e("ChatViewModel", "getReceiverPublicKey: empty public key for chatId=$chatId")
                        _publicKeyError.value = "Received empty public key"
                        _isPublicKeyLoading.value = false
                        getPublicKey = false
                        showRefreshing()
                        return@collect
                    }

                    receiverPublicKey.value = publicKey
                    _isPublicKeyReady.value = true
                    _publicKeyError.value = null
                    _isPublicKeyLoading.value = false
                    pushSocketDebugLog("HTTP public key loaded for $chatId")
                    Log.d("ChatViewModel", "getReceiverPublicKey: public key loaded for chatId=$chatId")
                    getPublicKey = false
                    showRefreshing()
                    isPublicKeyLoaded = true
                }

                is Resource.Loading -> {
                    _isPublicKeyLoading.value = true
                    _publicKeyError.value = null
                    getPublicKey = true
                    showRefreshing()
                }

                is Resource.Error -> {
                    _publicKeyError.value = res.message.asString(getApplication())
                    _isPublicKeyLoading.value = false
                    pushSocketDebugLog("HTTP public key failed for $chatId")
                    Log.e("ChatViewModel", "getReceiverPublicKey: failed for chatId=$chatId")
                    getPublicKey = false
                    showRefreshing()
                }
            }
        }

        return isPublicKeyLoaded
    }

    fun loadMessages() = viewModelScope.launch {
        messagesMutex.withLock {
            getMessageFromDBUseCase.execute(chatId).collect { dbMessages ->
                _messages.clear()
                _messages.addAll(dbMessages)
            }
        }
    }

    private suspend fun saveMessageToDb(message: MessageEntity) {
        Log.d("ChatViewModel", "saveMessageToDb: senderOrReceiver=${message.senderOrReceiver}, timestamp=${message.timestamp}")
        saveMessageInDbUseCase.execute(message).collect { }
    }

    fun saveMessage(message: MessageEntity) = viewModelScope.launch {
        saveMessageToDb(message)
        messagesMutex.withLock {
            getMessageFromDBUseCase.execute(chatId).collect { dbMessages ->
                _messages.clear()
                _messages.addAll(dbMessages)
            }
        }
    }

    fun clearMessages() = viewModelScope.launch {
        clearMessageFromDBUseCase.execute(chatId).collect {
            _messages.clear()
        }
    }

    private fun getUsernameFromPrefs(): String? = spref.getUsername()

    private fun getCurrentTimestamp(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.Instant.now().toString()
        } else {
            TODO("VERSION.SDK_INT < O")
        }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }

    fun retryLoadPublicKey() {
        _publicKeyError.value = null
        _isPublicKeyLoading.value = true
        prepareEncryptionSession()
    }

    fun startVoiceRecording(context: Context) {
        if (_isRecording.value) return
        val recorder = com.dino.message.chatfeature.presentation.component.VoiceRecorderManager(context)
        if (recorder.startRecording()) {
            voiceRecorder = recorder
            _isRecording.value = true
            _recordingDurationMs.value = 0L
            recordingTimerJob = viewModelScope.launch {
                while (_isRecording.value) {
                    _recordingDurationMs.value = recorder.currentDurationMs
                    kotlinx.coroutines.delay(200)
                }
            }
        }
    }

    fun stopVoiceRecording() {
        val recorder = voiceRecorder ?: return
        recordingTimerJob?.cancel()
        _isRecording.value = false
        val uri = recorder.stopRecording()
        voiceRecorder = null
        if (uri != null) {
            sendAttachment(uri)
        }
    }

    fun cancelVoiceRecording() {
        val recorder = voiceRecorder ?: return
        recordingTimerJob?.cancel()
        _isRecording.value = false
        recorder.cancelRecording()
        voiceRecorder = null
    }

    fun dismissUploadError() {
        _uploadError.value = null
    }

    private fun showRefreshing() {
        _isRefreshing.value = getPublicKey || isSocketConnecting
    }

    fun sendMessage(message: String) {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty()) {
            pushSocketDebugLog("Ignoring empty message")
            return
        }

        val publicKey = receiverPublicKey.value
        if (publicKey.isNullOrBlank()) {
            pushSocketDebugLog("Cannot send: missing receiver public key")
            return
        }

        val activeSocket = socket
        if (activeSocket == null || !activeSocket.connected()) {
            pushSocketDebugLog("Socket unavailable, reconnecting before send")
            pendingOutgoingMessages.addLast(trimmedMessage)
            connectSocketIfNeeded(forceReconnect = true)
            return
        }

        emitEncryptedMessage(trimmedMessage, activeSocket)
    }

    fun sendAttachment(fileUri: Uri, caption: String? = null) {
        val publicKey = receiverPublicKey.value
        if (publicKey.isNullOrBlank()) {
            pushSocketDebugLog("Cannot send attachment: missing receiver public key")
            return
        }

        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            pushSocketDebugLog("Uploading attachment...")

            uploadAttachmentUseCase.execute(fileUri).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _isUploading.value = true
                    }

                    is Resource.Success -> {
                        _isUploading.value = false
                        val attachment = result.data
                        pushSocketDebugLog("Upload success: ${attachment.type} (${attachment.fileName})")

                        val activeSocket = socket
                        if (activeSocket == null || !activeSocket.connected()) {
                            pushSocketDebugLog("Socket unavailable after upload, reconnecting")
                            connectSocketIfNeeded(forceReconnect = true)
                            return@collect
                        }

                        emitAttachmentMessage(
                            attachment = attachment,
                            caption = caption,
                            activeSocket = activeSocket
                        )
                    }

                    is Resource.Error -> {
                        _isUploading.value = false
                        val errorMsg = result.message.asString(getApplication())
                        _uploadError.value = errorMsg
                        pushSocketDebugLog("Upload failed: $errorMsg")
                    }
                }
            }
        }
    }

    private fun emitAttachmentMessage(
        attachment: com.dino.message.chatfeature.data.remote.dto.MessageAttachment,
        caption: String?,
        activeSocket: Socket
    ) {
        val publicKey = receiverPublicKey.value ?: return

        try {
            val encryptedCaption: String?
            val encryptedKey: String

            if (!caption.isNullOrBlank()) {
                val (ek, em) = CryptoUtil.encryptMessage(
                    message = caption,
                    receiverPublicKey = decodePublicKey(publicKey)
                )
                encryptedKey = ek
                encryptedCaption = em
            } else {
                val (ek, _) = CryptoUtil.encryptMessage(
                    message = "",
                    receiverPublicKey = decodePublicKey(publicKey)
                )
                encryptedKey = ek
                encryptedCaption = null
            }

            val attachmentJson = JSONObject().apply {
                put("type", attachment.type)
                put("url", attachment.url)
                put("mime_type", attachment.mimeType)
                attachment.fileName?.let { put("file_name", it) }
                attachment.sizeBytes?.let { put("size_bytes", it) }
            }

            val payload = JSONObject().apply {
                put("to", chatId)
                put("type", attachment.type)
                put("encrypted_key", encryptedKey)
                put("attachment", attachmentJson)
                if (encryptedCaption != null) {
                    put("message", encryptedCaption)
                }
            }

            pushSocketDebugLog("Socket emit send_message (${attachment.type}) to $chatId")
            activeSocket.emit("send_message", payload)

            val currentUsername = getUsernameFromPrefs().orEmpty().ifBlank { "self" }
            saveMessage(
                MessageEntity(
                    senderOrReceiver = currentUsername,
                    chatId = chatId,
                    content = caption ?: "",
                    timestamp = getCurrentTimestamp(),
                    messageType = attachment.type,
                    attachmentType = attachment.type,
                    attachmentUrl = attachment.url,
                    attachmentMimeType = attachment.mimeType,
                    attachmentFileName = attachment.fileName,
                    attachmentSizeBytes = attachment.sizeBytes
                )
            )
        } catch (e: Exception) {
            pushSocketDebugLog("Attachment send failed: ${e.message ?: "unknown error"}")
            Log.e("ChatViewModel", "emitAttachmentMessage: failed", e)
        }
    }

    private fun emitEncryptedMessage(message: String, activeSocket: Socket) {
        val publicKey = receiverPublicKey.value ?: return

        try {
            val (encryptedKey, encryptedMessage) = CryptoUtil.encryptMessage(
                message = message,
                receiverPublicKey = decodePublicKey(publicKey)
            )

            val payload = JSONObject().apply {
                put("to", chatId)
                put("message", encryptedMessage)
                put("encrypted_key", encryptedKey)
                put("type", "text")
            }

            pushSocketDebugLog("Socket emit send_message to $chatId")
            activeSocket.emit("send_message", payload)

            val currentUsername = getUsernameFromPrefs().orEmpty().ifBlank { "self" }
            saveMessage(
                MessageEntity(
                    senderOrReceiver = currentUsername,
                    chatId = chatId,
                    content = message,
                    timestamp = getCurrentTimestamp()
                )
            )
        } catch (e: Exception) {
            pushSocketDebugLog("Encryption/send failed: ${e.message ?: "unknown error"}")
            Log.e("ChatViewModel", "sendMessage: failed", e)
        }
    }

    private fun connectSocketIfNeeded(forceReconnect: Boolean = false) {
        if (!forceReconnect && hasSocketConnectionStarted && socket?.connected() == true) {
            requestPeerStatus()
            return
        }
        if (!forceReconnect && isSocketConnecting) {
            return
        }

        disconnectSocket()

        val token = spref.getToken().removePrefix("Bearer ").trim()
        if (token.isBlank()) {
            pushSocketDebugLog("Socket token is missing")
            return
        }

        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
        val opts = IO.Options.builder()
            .setForceNew(true)
            .setMultiplex(false)
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(1_000)
            .setReconnectionDelayMax(10_000)
            .setTimeout(20_000)
            .setPath("/socket.io/")
            .setTransports(arrayOf("websocket", "polling"))
            .setAuth(mapOf("token" to token))
            .build()

        try {
            val createdSocket = IO.socket(baseUrl, opts)
            registerSocketListeners(createdSocket)
            socket = createdSocket
            hasSocketConnectionStarted = true
            isSocketConnecting = true
            showRefreshing()
            pushSocketDebugLog("Socket connecting to $baseUrl")
            createdSocket.connect()
        } catch (e: URISyntaxException) {
            pushSocketDebugLog("Socket URL invalid: ${e.message ?: "unknown error"}")
            Log.e("ChatViewModel", "connectSocketIfNeeded: invalid socket url", e)
        } catch (e: Exception) {
            pushSocketDebugLog("Socket connect failed: ${e.message ?: "unknown error"}")
            Log.e("ChatViewModel", "connectSocketIfNeeded: failed", e)
        }
    }

    private fun sendAck(messageIds: List<String>) {
        val activeSocket = socket ?: return
        if (messageIds.isEmpty()) return
        val payload = JSONObject().apply {
            put("message_ids", JSONArray(messageIds))
        }
        activeSocket.emit("ack_messages", payload)
        pushSocketDebugLog("Sent ack for ${messageIds.size} message(s)")
    }

    private fun registerSocketListeners(createdSocket: Socket) {
        createdSocket.on(Socket.EVENT_CONNECT) {
            viewModelScope.launch {
                isSocketConnecting = false
                showRefreshing()
                pushSocketDebugLog("Socket connected")
                requestPeerStatus()
                flushPendingOutgoingMessages()
            }
        }

        createdSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            viewModelScope.launch {
                isSocketConnecting = false
                showRefreshing()
                val details = args.joinToString(separator = " | ") { it.toDebugString() }
                pushSocketDebugLog("Socket connect_error: ${details.ifBlank { "unknown" }}")
            }
        }

        createdSocket.on(Socket.EVENT_DISCONNECT) { args ->
            viewModelScope.launch {
                isSocketConnecting = false
                showRefreshing()
                _isPeerOnline.value = false
                pushSocketDebugLog("Socket disconnected: ${args.firstOrNull()?.toDebugString() ?: "unknown"}")
            }
        }

        createdSocket.on("connected") { args ->
            viewModelScope.launch {
                val raw = extractPayloadArg(args, "connected")
                pushSocketDebugLog("Socket auth ok: ${raw?.toDebugString() ?: "no payload"}")
            }
        }

        createdSocket.on("pending_messages") { args ->
            viewModelScope.launch {
                val raw = extractPayloadArg(args, "pending_messages")
                Log.d("ChatViewModel", "pending_messages raw type=${raw?.javaClass?.name}, value=${raw?.toString()?.take(500)}")
                pushSocketDebugLog("pending_messages raw type=${raw?.javaClass?.simpleName}, preview=${raw?.toString()?.take(200)}")

                val messages: JSONArray? = when (raw) {
                    is JSONObject -> raw.optJSONArray("messages")
                    is JSONArray -> raw
                    is String -> {
                        val trimmed = raw.trim()
                        when {
                            trimmed.startsWith("{") -> try {
                                JSONObject(trimmed).optJSONArray("messages")
                            } catch (e: Exception) {
                                Log.w("ChatViewModel", "pending_messages: failed to parse String as JSONObject", e)
                                null
                            }
                            trimmed.startsWith("[") -> try {
                                JSONArray(trimmed)
                            } catch (e: Exception) {
                                Log.w("ChatViewModel", "pending_messages: failed to parse String as JSONArray", e)
                                null
                            }
                            else -> null
                        }
                    }
                    else -> null
                }

                if (messages == null) {
                    pushSocketDebugLog("pending_messages: could not extract messages array (raw type=${raw?.javaClass?.simpleName})")
                    Log.e("ChatViewModel", "pending_messages: could not extract messages. raw type=${raw?.javaClass?.name}, raw=$raw")
                    return@launch
                }
                pushSocketDebugLog("Received ${messages.length()} pending message(s)")
                val ackedIds = mutableListOf<String>()
                repeat(messages.length()) { index ->
                    val item = messages.optJSONObject(index) ?: return@repeat
                    val msgId = item.optString("message_id", "")
                    if (msgId.isNotBlank() && deduplicator.isDuplicate(msgId)) {
                        pushSocketDebugLog("Skipping duplicate pending message $msgId")
                        ackedIds.add(msgId)
                        return@repeat
                    }
                    handleIncomingMessage(item)
                    if (msgId.isNotBlank()) {
                        ackedIds.add(msgId)
                    }
                }
                loadMessages()
                if (ackedIds.isNotEmpty()) {
                    sendAck(ackedIds)
                }
            }
        }

        createdSocket.on("new_message") { args ->
            viewModelScope.launch {
                val raw = extractPayloadArg(args, "new_message")
                Log.d("ChatViewModel", "new_message raw type=${raw?.javaClass?.name}, value=${raw?.toString()?.take(500)}")
                val payload = when (raw) {
                    is JSONObject -> raw
                    is String -> try { JSONObject(raw) } catch (e: Exception) {
                        Log.w("ChatViewModel", "new_message: failed to parse String as JSONObject", e)
                        null
                    }
                    else -> null
                }
                if (payload == null) {
                    pushSocketDebugLog("new_message payload invalid (type=${raw?.javaClass?.simpleName}, preview=${raw?.toString()?.take(200)})")
                    Log.e("ChatViewModel", "new_message: could not parse. raw type=${raw?.javaClass?.name}, raw=$raw")
                    return@launch
                }
                val msgId = payload.optString("message_id", "")
                if (msgId.isNotBlank() && deduplicator.isDuplicate(msgId)) {
                    pushSocketDebugLog("Skipping duplicate new_message $msgId")
                    sendAck(listOf(msgId))
                    return@launch
                }
                pushSocketDebugLog("Received live message from ${payload.optString("from")}")
                handleIncomingMessage(payload)
                loadMessages()
                if (msgId.isNotBlank()) {
                    sendAck(listOf(msgId))
                }
            }
        }

        createdSocket.on("message_sent") { args ->
            viewModelScope.launch {
                val raw = extractPayloadArg(args, "message_sent")
                pushSocketDebugLog("Sender ack: ${raw?.toDebugString() ?: "no payload"}")
            }
        }

        createdSocket.on("message_error") { args ->
            viewModelScope.launch {
                val raw = extractPayloadArg(args, "message_error")
                pushSocketDebugLog("Socket message_error: ${raw?.toDebugString() ?: "unknown"}")
            }
        }

        createdSocket.on("user_status") { args ->
            viewModelScope.launch {
                val payload = extractPayloadArg(args, "user_status") as? JSONObject ?: return@launch
                val username = payload.optString("username")
                if (username == chatId) {
                    val online = payload.optBoolean("online", false)
                    _isPeerOnline.value = online
                    pushSocketDebugLog("Peer status: $chatId is ${if (online) "online" else "offline"}")
                }
            }
        }
    }

    private suspend fun handleIncomingMessage(payload: JSONObject) {
        val sender = payload.optString("from")
        val messageId = payload.optString("message_id", "").ifBlank { null }

        val encryptedMessage = payload.optString("message")
        val encryptedKey = payload.optString("encrypted_key")
        val timestamp = payload.optString("timestamp").ifBlank { getCurrentTimestamp() }
        val type = payload.optString("type", "text")
        val attachmentObj = payload.optJSONObject("attachment")

        val targetChatId = sender

        if (attachmentObj != null) {
            val decryptedCaption = if (encryptedMessage.isNotBlank() && encryptedKey.isNotBlank()) {
                try {
                    val privateKey = spref.getPrivateKey() ?: ""
                    decryptMessage(encryptedMessage, encryptedKey, decodePrivateKey(privateKey))
                } catch (e: Exception) { "" }
            } else ""

            saveMessageToDb(
                MessageEntity(
                    messageId = messageId,
                    senderOrReceiver = sender,
                    chatId = targetChatId,
                    content = decryptedCaption,
                    timestamp = timestamp,
                    messageType = type,
                    attachmentType = attachmentObj.optString("type"),
                    attachmentUrl = attachmentObj.optString("url"),
                    attachmentMimeType = attachmentObj.optString("mime_type"),
                    attachmentFileName = attachmentObj.optString("file_name"),
                    attachmentSizeBytes = attachmentObj.optLong("size_bytes", 0)
                )
            )
        } else {
            if (encryptedMessage.isBlank() || encryptedKey.isBlank()) {
                pushSocketDebugLog("Incoming message payload missing fields")
                return
            }
            decryptAndSaveMessage(
                messageId = messageId,
                sender = sender,
                encryptedMessage = encryptedMessage,
                encryptedKey = encryptedKey,
                timestamp = timestamp,
                targetChatId = targetChatId
            )
        }
    }

    private fun requestPeerStatus() {
        socket?.emit(
            "get_user_status",
            JSONObject().apply {
                put("username", chatId)
            }
        )
        pushSocketDebugLog("Requested status for $chatId")
    }

    private fun flushPendingOutgoingMessages() {
        val activeSocket = socket ?: return
        while (pendingOutgoingMessages.isNotEmpty()) {
            val pendingMessage = pendingOutgoingMessages.removeFirst()
            pushSocketDebugLog("Flushing queued message to $chatId")
            emitEncryptedMessage(pendingMessage, activeSocket)
        }
    }

    private fun disconnectSocket() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    private suspend fun decryptAndSaveMessage(
        messageId: String?,
        sender: String,
        encryptedMessage: String,
        encryptedKey: String,
        timestamp: String,
        targetChatId: String = chatId
    ) {
        try {
            val privateKey = spref.getPrivateKey() ?: return
            val decryptedMsg = decryptMessage(
                encryptedMessage = encryptedMessage,
                encryptedAESKey = encryptedKey,
                privateKey = decodePrivateKey(privateKey)
            )
            Log.d("ChatViewModel", "decryptAndSaveMessage: decrypt success for sender=$sender timestamp=$timestamp")

            saveMessageToDb(
                MessageEntity(
                    messageId = messageId,
                    senderOrReceiver = sender,
                    chatId = targetChatId,
                    content = decryptedMsg,
                    timestamp = timestamp
                )
            )
        } catch (e: Exception) {
            pushSocketDebugLog("Decrypt failed for $sender: ${e.message ?: "unknown error"}")
            Log.e("ChatViewModel", "decryptAndSaveMessage: decrypt failed for sender=$sender", e)
        }
    }

    private fun Any.toDebugString(): String = when (this) {
        is JSONObject -> toString()
        else -> toString()
    }

    /**
     * The Java Socket.IO client v2.0.0 sometimes includes the event name as
     * args[0].  This helper finds the first non-event-name element that looks
     * like the actual data payload (JSONObject, JSONArray, or a JSON string).
     */
    private fun extractPayloadArg(args: Array<out Any>, eventName: String): Any? {
        for (arg in args) {
            when (arg) {
                is JSONObject, is JSONArray -> return arg
                is String -> {
                    if (arg != eventName) return arg
                }
                else -> return arg
            }
        }
        return null
    }

    override fun onCleared() {
        voiceRecorder?.release()
        disconnectSocket()
        super.onCleared()
    }
}
