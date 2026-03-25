package com.dino.message.chatfeature.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
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
import com.dino.message.profilefeature.domain.usecase.FollowUserUseCase
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
import com.dino.message.chatfeature.data.notification.ActiveChatTracker
import com.dino.message.chatfeature.data.notification.NotificationHelper
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
    private val followUserUseCase: FollowUserUseCase,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val receiverPublicKey = mutableStateOf<String?>(null)

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

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

    private val _showFollowRequired = MutableStateFlow(false)
    val showFollowRequired: StateFlow<Boolean> = _showFollowRequired.asStateFlow()

    private val _isFollowLoading = MutableStateFlow(false)
    val isFollowLoading: StateFlow<Boolean> = _isFollowLoading.asStateFlow()

    private val _decryptionError = MutableStateFlow<String?>(null)
    val decryptionError: StateFlow<String?> = _decryptionError.asStateFlow()

    private val _socketError = MutableStateFlow<String?>(null)
    val socketError: StateFlow<String?> = _socketError.asStateFlow()

    private val _replyingTo = MutableStateFlow<MessageEntity?>(null)
    val replyingTo: StateFlow<MessageEntity?> = _replyingTo.asStateFlow()

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
        ActiveChatTracker.setActiveChat(chatId)
        NotificationHelper.clearNotificationForUser(app, chatId)
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
                    val errorMessage = res.message.asString(getApplication())
                    if (errorMessage.contains("Not in your contacts", ignoreCase = true)) {
                        _showFollowRequired.value = true
                        _publicKeyError.value = null
                        _isPublicKeyLoading.value = false
                    } else {
                        _publicKeyError.value = errorMessage
                        _isPublicKeyLoading.value = false
                    }
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
                _messages.value = dbMessages
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
                _messages.value = dbMessages
            }
        }
    }

    fun clearMessages() = viewModelScope.launch {
        clearMessageFromDBUseCase.execute(chatId).collect {
            _messages.value = emptyList()
        }
    }

    private fun getUsernameFromPrefs(): String? = spref.getUsername()

    private fun getCurrentTimestamp(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val now = java.time.Instant.now().atZone(java.time.ZoneOffset.UTC)
            now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"))
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

    fun followAndRetry() {
        viewModelScope.launch {
            _isFollowLoading.value = true
            followUserUseCase.execute(chatId).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _showFollowRequired.value = false
                        _isFollowLoading.value = false
                        _isPublicKeyLoading.value = true
                        prepareEncryptionSession()
                    }
                    is Resource.Loading -> {
                        _isFollowLoading.value = true
                    }
                    is Resource.Error -> {
                        _isFollowLoading.value = false
                        _showFollowRequired.value = false
                        _publicKeyError.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    fun dismissFollowRequired() {
        _showFollowRequired.value = false
        navigateUp(viewModelScope)
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

    fun dismissDecryptionError() {
        _decryptionError.value = null
    }

    fun dismissSocketError() {
        _socketError.value = null
    }

    fun setReplyingTo(message: MessageEntity) {
        _replyingTo.value = message
    }

    fun clearReplyingTo() {
        _replyingTo.value = null
    }

    fun retrySocketConnection() {
        _socketError.value = null
        connectSocketIfNeeded(forceReconnect = true)
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
            pushSocketDebugLog("⚠️ Socket unavailable (null=${activeSocket == null}, connected=${activeSocket?.connected()}), queuing & reconnecting")
            Log.w("ChatViewModel", "sendMessage: socket unavailable, queuing message and reconnecting")
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
        activeSocket: Socket,
        replyTo: MessageEntity? = _replyingTo.value
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
                if (replyTo != null) {
                    put("reply_to_message_id", replyTo.messageId ?: "")
                    put("reply_to_sender", replyTo.senderOrReceiver)
                   val preview = replyTo.content.take(100)
                   if (preview.isNotBlank()) {
                        val (encReplyKey, encReplyPreview) = CryptoUtil.encryptMessage(
                           message = preview,
                           receiverPublicKey = decodePublicKey(publicKey)
                       )
                      put("encrypted_reply_preview", encReplyPreview)
                       put("encrypted_reply_key", encReplyKey)
                   }
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
                    attachmentSizeBytes = attachment.sizeBytes,
                    replyToMessageId = replyTo?.messageId,
                    replyToSender = replyTo?.senderOrReceiver,
                    replyPreview = replyTo?.content?.take(100)
                )
            )
            _replyingTo.value = null
        } catch (e: Exception) {
            pushSocketDebugLog("Attachment send failed: ${e.message ?: "unknown error"}")
            Log.e("ChatViewModel", "emitAttachmentMessage: failed", e)
        }
    }

    private fun emitEncryptedMessage(message: String, activeSocket: Socket, replyTo: MessageEntity? = _replyingTo.value) {
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
                if (replyTo != null) {
                    put("reply_to_message_id", replyTo.messageId ?: "")
                    put("reply_to_sender", replyTo.senderOrReceiver)
                    val preview = replyTo.content.take(100)
                    if (preview.isNotBlank()) {
                        val (encReplyKey, encReplyPreview) = CryptoUtil.encryptMessage(
                            message = preview,
                            receiverPublicKey = decodePublicKey(publicKey)
                        )
                       put("encrypted_reply_preview", encReplyPreview)
                        put("encrypted_reply_key", encReplyKey)
                   }
                }
            }

            pushSocketDebugLog("Socket emit send_message to $chatId")
            activeSocket.emit("send_message", payload)
            Log.d("ChatViewModel", "sendMessage: emitted send_message to=$chatId, socketConnected=${activeSocket.connected()}")

            val currentUsername = getUsernameFromPrefs().orEmpty().ifBlank { "self" }
            saveMessage(
                MessageEntity(
                    senderOrReceiver = currentUsername,
                    chatId = chatId,
                    content = message,
                    timestamp = getCurrentTimestamp(),
                    replyToMessageId = replyTo?.messageId,
                    replyToSender = replyTo?.senderOrReceiver,
                    replyPreview = replyTo?.content?.take(100)
                )
            )
            _replyingTo.value = null
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
                _socketError.value = null
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
                _socketError.value = "Connection error. Please check your network."
            }
        }

        createdSocket.on(Socket.EVENT_DISCONNECT) { args ->
            viewModelScope.launch {
                isSocketConnecting = false
                showRefreshing()
                _isPeerOnline.value = false
                pushSocketDebugLog("Socket disconnected: ${args.firstOrNull()?.toDebugString() ?: "unknown"}")
                _socketError.value = "Disconnected from chat server."
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
                val detail = raw?.toDebugString() ?: "no payload"
                pushSocketDebugLog("✅ message_sent: $detail")
                Log.d("ChatViewModel", "message_sent ack received: $detail")
            }
        }

        createdSocket.on("message_error") { args ->
            viewModelScope.launch {
                val raw = extractPayloadArg(args, "message_error")
                val detail = raw?.toDebugString() ?: "unknown"
                pushSocketDebugLog("❌ message_error: $detail")
                Log.e("ChatViewModel", "message_error from server: $detail")
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
        val replyToMessageId = payload.optString("reply_to_message_id", "").ifBlank { null }
        val replyToSender = payload.optString("reply_to_sender", "").ifBlank { null }
       val encryptedReplyPreview = payload.optString("encrypted_reply_preview", "").ifBlank { null }
        val encryptedReplyKey = payload.optString("encrypted_reply_key", "").ifBlank { null }

        val replyDecryptKey = encryptedReplyKey ?: encryptedKey
        val decryptedReplyPreview = if (encryptedReplyPreview != null && replyDecryptKey.isNotBlank()) {
           try {
               val privateKey = spref.getPrivateKey() ?: ""
                decryptMessage(encryptedReplyPreview, replyDecryptKey, decodePrivateKey(privateKey))
           } catch (e: Exception) { null }
        } else null

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
                    attachmentSizeBytes = attachmentObj.optLong("size_bytes", 0),
                    replyToMessageId = replyToMessageId,
                    replyToSender = replyToSender,
                    replyPreview = decryptedReplyPreview
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
                targetChatId = targetChatId,
                replyToMessageId = replyToMessageId,
                replyToSender = replyToSender,
                replyPreview = decryptedReplyPreview
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
        targetChatId: String = chatId,
        replyToMessageId: String? = null,
        replyToSender: String? = null,
        replyPreview: String? = null
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
                    timestamp = timestamp,
                    replyToMessageId = replyToMessageId,
                    replyToSender = replyToSender,
                    replyPreview = replyPreview
                )
            )
        } catch (e: Exception) {
            pushSocketDebugLog("Decrypt failed for $sender: ${e.message ?: "unknown error"}")
            Log.e("ChatViewModel", "decryptAndSaveMessage: decrypt failed for sender=$sender", e)
            _decryptionError.value = "Unable to decrypt message from $sender. Your private key may not match. Please re-login with the correct private key."
            saveMessageToDb(
                MessageEntity(
                    messageId = messageId,
                    senderOrReceiver = sender,
                    chatId = targetChatId,
                    content = "⚠️ Could not decrypt this message. Wrong private key?",
                    timestamp = timestamp,
                    replyToMessageId = replyToMessageId,
                    replyToSender = replyToSender,
                    replyPreview = replyPreview
                )
            )
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
        ActiveChatTracker.clearActiveChat()
        voiceRecorder?.release()
        disconnectSocket()
        super.onCleared()
    }
}
