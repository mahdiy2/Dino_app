package com.dino.message.groupfeature.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.BuildConfig
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.CryptoUtil
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.util.decodePrivateKey
import com.dino.message.corefeature.presentation.util.decodePublicKey
import com.dino.message.corefeature.presentation.util.decryptMessage
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.groupfeature.domain.model.GroupMessageEntity
import com.dino.message.groupfeature.domain.usecase.GetGroupDetailUseCase
import com.dino.message.groupfeature.domain.usecase.UploadGroupAttachmentUseCase
import com.dino.message.groupfeature.domain.usecase.SaveGroupMessageUseCase
import com.dino.message.groupfeature.domain.usecase.GetGroupMessagesFromDbUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray
import java.net.URISyntaxException
import java.security.PublicKey
import javax.inject.Inject

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val spref: SPrefManager,
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val uploadGroupAttachmentUseCase: UploadGroupAttachmentUseCase,
    private val saveGroupMessageUseCase: SaveGroupMessageUseCase,
    private val getGroupMessagesFromDbUseCase: GetGroupMessagesFromDbUseCase,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    companion object {
        private const val TAG = "GroupChatVM"
    }

    val groupId: Int = checkNotNull(savedStateHandle["groupId"])
    val groupName: String = savedStateHandle["groupName"] ?: "Group"

    private val currentUsername: String = spref.getUsername() ?: ""

    private val _messages = MutableStateFlow<List<GroupMessageEntity>>(emptyList())
    val messages: StateFlow<List<GroupMessageEntity>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _socketError = MutableStateFlow<String?>(null)
    val socketError: StateFlow<String?> = _socketError.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _replyingTo = MutableStateFlow<GroupMessageEntity?>(null)
    val replyingTo: StateFlow<GroupMessageEntity?> = _replyingTo.asStateFlow()

    private var voiceRecorder: com.dino.message.chatfeature.presentation.component.VoiceRecorderManager? = null
    private var recordingTimerJob: kotlinx.coroutines.Job? = null

    private var socket: Socket? = null
    private var hasSocketStarted = false
    private val seenMessageIds = mutableSetOf<String>()

    private var memberPublicKeys: Map<String, PublicKey> = emptyMap()
    private var memberKeysReady = false

    init {
        loadMessagesFromDb()
        loadGroupDetailAndConnect()
    }

    fun getMyUsername(): String = currentUsername

    private fun loadMessagesFromDb() = viewModelScope.launch {
        try {
            getGroupMessagesFromDbUseCase.execute(groupId).collect { dbMessages ->
                _messages.value = dbMessages
                seenMessageIds.addAll(dbMessages.map { it.messageId })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load messages from DB", e)
        }
    }

    private suspend fun saveMessageToDb(entity: GroupMessageEntity) {
        try {
            saveGroupMessageUseCase.execute(entity).collect { }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save message to DB", e)
        }
    }

    private fun loadGroupDetailAndConnect() = viewModelScope.launch {
        getGroupDetailUseCase.execute(groupId).collect { res ->
            when (res) {
                is Resource.Loading -> _isLoading.value = true
                is Resource.Success -> {
                    _isLoading.value = false
                    val group = res.data
                    val keys = mutableMapOf<String, PublicKey>()
                    group.members?.forEach { member ->
                        val pk = member.publicKey
                        if (!pk.isNullOrBlank()) {
                            try {
                                keys[member.username] = decodePublicKey(pk)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to decode public key for ${member.username}", e)
                            }
                        }
                    }
                    memberPublicKeys = keys
                    memberKeysReady = true
                    Log.d(TAG, "Loaded ${keys.size} member public keys for group $groupId")
                    connectSocket()
                }
                is Resource.Error -> {
                    _isLoading.value = false
                    _socketError.value = res.message.asString(getApplication<DinoOrderApplication>())
                }
            }
        }
    }

    private fun decryptGroupMessage(
        encryptedMessage: String?,
        encryptedKeys: JSONObject?,
    ): String {
        if (encryptedMessage.isNullOrBlank() || encryptedKeys == null) return encryptedMessage ?: ""

        val myEncryptedKey = encryptedKeys.optString(currentUsername, "")
        if (myEncryptedKey.isBlank()) {
            Log.w(TAG, "No encrypted key found for $currentUsername")
            return "\u26A0\uFE0F Could not decrypt: no key for your user"
        }

        return try {
            val privateKeyStr = spref.getPrivateKey() ?: return "\u26A0\uFE0F Could not decrypt: missing private key"
            decryptMessage(
                encryptedMessage = encryptedMessage,
                encryptedAESKey = myEncryptedKey,
                privateKey = decodePrivateKey(privateKeyStr),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt group message", e)
            "\u26A0\uFE0F Could not decrypt this message"
        }
    }

    private fun encryptForGroup(plaintext: String): CryptoUtil.GroupEncryptionResult? {
        if (!memberKeysReady || memberPublicKeys.isEmpty()) {
            Log.e(TAG, "Cannot encrypt: member keys not ready")
            _socketError.value = "Encryption keys not loaded yet"
            return null
        }
        return try {
            CryptoUtil.encryptGroupMessage(plaintext, memberPublicKeys)
        } catch (e: Exception) {
            Log.e(TAG, "Group encryption failed", e)
            _socketError.value = "Encryption failed: ${e.message}"
            null
        }
    }

    private fun buildEncryptedKeysJson(keys: Map<String, String>): JSONObject {
        return JSONObject().apply {
            for ((username, encKey) in keys) {
                put(username, encKey)
            }
        }
    }

    private fun connectSocket() {
        if (hasSocketStarted && socket?.connected() == true) return
        disconnectSocket()

        val token = spref.getToken().removePrefix("Bearer ").trim()
        if (token.isBlank()) return

        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')
        val opts = IO.Options.builder()
            .setForceNew(true)
            .setMultiplex(false)
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(1_000)
            .setReconnectionDelayMax(10_000)
            .setTimeout(20_000)
            .setAuth(mapOf("token" to token))
            .build()

        val socket: Socket
        try {
            socket = IO.socket(baseUrl, opts)
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid socket URL", e)
            return
        }
        this.socket = socket
        hasSocketStarted = true

        socket.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Socket connected, joining group room")
            socket.emit("join_group", JSONObject().apply { put("group_id", groupId) })
        }

        socket.on("group_joined") {
            Log.d(TAG, "Joined group room $groupId")
        }

        socket.on("pending_group_messages") { args ->
            viewModelScope.launch {
                try {
                    val raw = extractPayloadArg(args)
                    val obj = when (raw) {
                        is JSONObject -> raw
                        is String -> try { JSONObject(raw) } catch (_: Exception) { null }
                        else -> null
                    } ?: return@launch

                    val pendingGroupId = obj.optInt("group_id", -1)
                    if (pendingGroupId != groupId) return@launch

                    val messagesArr: JSONArray = obj.optJSONArray("messages") ?: return@launch
                    Log.d(TAG, "Received ${messagesArr.length()} pending group messages for group $groupId")

                    val ackedIds = mutableListOf<String>()
                    repeat(messagesArr.length()) { index ->
                        val item = messagesArr.optJSONObject(index) ?: return@repeat
                        val messageId = item.optString("message_id", "")
                        if (messageId.isBlank() || !seenMessageIds.add(messageId)) {
                            if (messageId.isNotBlank()) ackedIds.add(messageId)
                            return@repeat
                        }

                        val sender = item.optString("from", "")
                        val encryptedMessage = item.optString("message", "")
                        val type = item.optString("type", "text")
                        val timestamp = item.optString("timestamp", "")
                        val encryptedKeysObj = item.optJSONObject("encrypted_keys")

                        val decryptedContent = decryptGroupMessage(encryptedMessage, encryptedKeysObj)

                        val attachmentObj = item.optJSONObject("attachment")

                        val decryptedReplyPreview = run {
                            val encReply = if (item.has("encrypted_reply_preview") && !item.isNull("encrypted_reply_preview"))
                                item.optString("encrypted_reply_preview", "").ifBlank { null }
                            else null
                            if (encReply != null && encryptedKeysObj != null) {
                                decryptGroupMessage(encReply, encryptedKeysObj)
                            } else null
                        }

                        val entity = GroupMessageEntity(
                            messageId = messageId,
                            groupId = groupId,
                            sender = sender,
                            content = decryptedContent,
                            timestamp = timestamp,
                            messageType = type,
                            attachmentType = attachmentObj?.optString("type"),
                            attachmentUrl = attachmentObj?.optString("url"),
                            attachmentMimeType = attachmentObj?.optString("mime_type"),
                            attachmentFileName = attachmentObj?.optString("file_name"),
                            attachmentSizeBytes = attachmentObj?.optLong("size_bytes"),
                            replyToMessageId = if (item.has("reply_to_message_id") && !item.isNull("reply_to_message_id"))
                                item.optString("reply_to_message_id", "").ifBlank { null } else null,
                            replyToSender = if (item.has("reply_to_sender") && !item.isNull("reply_to_sender"))
                                item.optString("reply_to_sender", "").ifBlank { null } else null,
                            replyPreview = decryptedReplyPreview,
                        )
                        saveMessageToDb(entity)
                        if (messageId.isNotBlank()) ackedIds.add(messageId)
                    }
                    loadMessagesFromDb()
                    if (ackedIds.isNotEmpty()) {
                        sendGroupAck(ackedIds)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling pending_group_messages", e)
                }
            }
        }

        socket.on("new_group_message") { args ->
            viewModelScope.launch {
                try {
                    val payload = extractPayloadArg(args) as? JSONObject ?: return@launch
                    val messageId = payload.optString("message_id", "")
                    if (messageId.isBlank() || !seenMessageIds.add(messageId)) return@launch

                    val sender = payload.optString("from", "")
                    val encryptedMessage = payload.optString("message", "")
                    val type = payload.optString("type", "text")
                    val timestamp = payload.optString("timestamp", "")

                    val encryptedKeysObj = payload.optJSONObject("encrypted_keys")

                    val decryptedContent = decryptGroupMessage(encryptedMessage, encryptedKeysObj)

                    val attachmentObj = payload.optJSONObject("attachment")

                    val decryptedReplyPreview = run {
                        val encReply = if (payload.has("encrypted_reply_preview") && !payload.isNull("encrypted_reply_preview"))
                            payload.optString("encrypted_reply_preview", "").ifBlank { null }
                        else null
                        if (encReply != null && encryptedKeysObj != null) {
                            decryptGroupMessage(encReply, encryptedKeysObj)
                        } else {
                            null
                        }
                    }

                    val entity = GroupMessageEntity(
                        messageId = messageId,
                        groupId = groupId,
                        sender = sender,
                        content = decryptedContent,
                        timestamp = timestamp,
                        messageType = type,
                        attachmentType = attachmentObj?.optString("type"),
                        attachmentUrl = attachmentObj?.optString("url"),
                        attachmentMimeType = attachmentObj?.optString("mime_type"),
                        attachmentFileName = attachmentObj?.optString("file_name"),
                        attachmentSizeBytes = attachmentObj?.optLong("size_bytes"),
                        replyToMessageId = if (payload.has("reply_to_message_id") && !payload.isNull("reply_to_message_id"))
                            payload.optString("reply_to_message_id", "").ifBlank { null } else null,
                        replyToSender = if (payload.has("reply_to_sender") && !payload.isNull("reply_to_sender"))
                            payload.optString("reply_to_sender", "").ifBlank { null } else null,
                        replyPreview = decryptedReplyPreview,
                    )
                    saveMessageToDb(entity)
                    loadMessagesFromDb()
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling new_group_message", e)
                }
            }
        }

        socket.on("group_message_sent") { args ->
            Log.d(TAG, "group_message_sent ack received")
        }

        socket.on("message_error") { args ->
            viewModelScope.launch {
                val payload = extractPayloadArg(args)
                val errorMsg = if (payload is JSONObject) payload.optString("error", "Unknown error") else "Unknown error"
                _socketError.value = errorMsg
                Log.e(TAG, "message_error: $errorMsg")
            }
        }

        socket.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Socket disconnected")
        }

        socket.connect()
    }

    fun sendTextMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val encResult = encryptForGroup(trimmed) ?: return

        val replyTo = _replyingTo.value
        val json = JSONObject().apply {
            put("group_id", groupId)
            put("message", encResult.encryptedMessage)
            put("encrypted_keys", buildEncryptedKeysJson(encResult.encryptedKeys))
            put("type", "text")
            if (replyTo != null) {
                put("reply_to_message_id", replyTo.messageId)
                put("reply_to_sender", replyTo.sender)
                val preview = replyTo.content.take(100)
                if (preview.isNotBlank()) {
                    put("encrypted_reply_preview", CryptoUtil.encryptWithAesKey(preview, encResult.rawAesKey))
                }
            }
        }
        socket?.emit("send_group_message", json)
        clearReply()
    }

    fun sendAttachment(fileUri: Uri, mediaType: String) {
        viewModelScope.launch {
            uploadGroupAttachmentUseCase.execute(groupId, fileUri).collect { res ->
                when (res) {
                    is Resource.Loading -> _isUploading.value = true
                    is Resource.Success -> {
                        _isUploading.value = false
                        val att = res.data

                        val encResult = encryptForGroup("") ?: return@collect

                        val replyTo = _replyingTo.value
                        val json = JSONObject().apply {
                            put("group_id", groupId)
                            put("message", encResult.encryptedMessage)
                            put("encrypted_keys", buildEncryptedKeysJson(encResult.encryptedKeys))
                            put("type", mediaType)
                            put("attachment", JSONObject().apply {
                                put("type", att.type)
                                put("url", att.url)
                                put("mime_type", att.mimeType)
                                att.fileName?.let { put("file_name", it) }
                                att.sizeBytes?.let { put("size_bytes", it) }
                            })
                            if (replyTo != null) {
                                put("reply_to_message_id", replyTo.messageId)
                                put("reply_to_sender", replyTo.sender)
                                val preview = replyTo.content.take(100)
                                if (preview.isNotBlank()) {
                                    put("encrypted_reply_preview", CryptoUtil.encryptWithAesKey(preview, encResult.rawAesKey))
                                }
                            }
                        }
                        socket?.emit("send_group_message", json)
                        clearReply()
                    }
                    is Resource.Error -> {
                        _isUploading.value = false
                        _uploadError.value = res.message.asString(getApplication<DinoOrderApplication>())
                    }
                }
            }
        }
    }

    fun startRecording(context: Context) {
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

    fun stopRecordingAndSend() {
        val recorder = voiceRecorder ?: return
        recordingTimerJob?.cancel()
        _isRecording.value = false
        val uri = recorder.stopRecording()
        voiceRecorder = null
        if (uri != null) {
            sendAttachment(uri, "voice")
        }
    }

    fun cancelRecording() {
        val recorder = voiceRecorder ?: return
        recordingTimerJob?.cancel()
        _isRecording.value = false
        recorder.cancelRecording()
        voiceRecorder = null
    }

    fun setReplyingTo(message: GroupMessageEntity) {
        _replyingTo.value = message
    }

    fun clearReply() {
        _replyingTo.value = null
    }

    fun dismissUploadError() {
        _uploadError.value = null
    }

    fun dismissSocketError() {
        _socketError.value = null
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }

    private fun sendGroupAck(messageIds: List<String>) {
        val activeSocket = socket ?: return
        if (messageIds.isEmpty()) return
        val payload = JSONObject().apply {
            put("group_id", groupId)
            put("message_ids", JSONArray(messageIds))
        }
        activeSocket.emit("ack_group_messages", payload)
        Log.d(TAG, "Sent group ack for ${messageIds.size} message(s)")
    }

    private fun disconnectSocket() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    private fun extractPayloadArg(args: Array<out Any>): Any? {
        for (arg in args) {
            if (arg is JSONObject || arg is org.json.JSONArray) return arg
            if (arg is String && arg.startsWith("{")) return try { JSONObject(arg) } catch (_: Exception) { arg }
        }
        return args.firstOrNull()
    }

    override fun onCleared() {
        voiceRecorder?.release()
        disconnectSocket()
        super.onCleared()
    }
}
