package com.dino.message.groupfeature.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.BuildConfig
import com.dino.message.DinoOrderApplication
import com.dino.message.chatfeature.domain.model.MessageDeliveryStatus
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.CryptoUtil
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.util.decodePrivateKey
import com.dino.message.corefeature.presentation.util.decodePublicKey
import com.dino.message.corefeature.presentation.util.decryptMessage
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.chatfeature.data.notification.ActiveGroupTracker
import com.dino.message.chatfeature.data.notification.NotificationHelper
import com.dino.message.groupfeature.domain.model.GroupMessageEntity
import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.repository.GroupRepository
import com.dino.message.groupfeature.domain.usecase.GetGroupDetailUseCase
import com.dino.message.groupfeature.domain.usecase.UploadGroupAttachmentUseCase
import com.dino.message.groupfeature.domain.usecase.SaveGroupMessageUseCase
import com.dino.message.groupfeature.domain.usecase.GetGroupMessagesFromDbUseCase
import com.ramcosta.composedestinations.generated.destinations.GroupMembersScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PrivateKeyResetDialogScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.URISyntaxException
import java.security.PublicKey
import java.util.Collections
import javax.inject.Inject

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val spref: SPrefManager,
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val uploadGroupAttachmentUseCase: UploadGroupAttachmentUseCase,
    private val saveGroupMessageUseCase: SaveGroupMessageUseCase,
    private val getGroupMessagesFromDbUseCase: GetGroupMessagesFromDbUseCase,
    private val groupRepository: GroupRepository,
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

    private val _messageDeliveryStatuses = MutableStateFlow<Map<String, MessageDeliveryStatus>>(emptyMap())
    val messageDeliveryStatuses: StateFlow<Map<String, MessageDeliveryStatus>> = _messageDeliveryStatuses.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _socketError = MutableStateFlow<String?>(null)
    val socketError: StateFlow<String?> = _socketError.asStateFlow()

    private val _decryptionError = MutableStateFlow<String?>(null)
    val decryptionError: StateFlow<String?> = _decryptionError.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _replyingTo = MutableStateFlow<GroupMessageEntity?>(null)
    val replyingTo: StateFlow<GroupMessageEntity?> = _replyingTo.asStateFlow()

    private val _memberProfileImageUrls = MutableStateFlow<Map<String, String?>>(emptyMap())
    val memberProfileImageUrls: StateFlow<Map<String, String?>> = _memberProfileImageUrls.asStateFlow()

    private var voiceRecorder: com.dino.message.chatfeature.presentation.component.VoiceRecorderManager? = null
    private var recordingTimerJob: kotlinx.coroutines.Job? = null

    private var socket: Socket? = null
    private var hasSocketStarted = false
    private val seenMessageIds = Collections.synchronizedSet(mutableSetOf<String>())

    private var memberPublicKeys: Map<String, PublicKey> = emptyMap()
    private var memberKeysReady = false

    init {
        ActiveGroupTracker.setActiveGroup(groupId)
        NotificationHelper.clearNotificationForGroup(app, groupId)
        loadMessagesFromDb()
        loadGroupDetailAndConnect()
    }

    fun getMyUsername(): String = currentUsername

    private fun loadMessagesFromDb() = viewModelScope.launch {
        try {
            getGroupMessagesFromDbUseCase.execute(groupId).collect { dbMessages ->
                _messages.value = dbMessages
                seenMessageIds.addAll(dbMessages.map { it.messageId })
                syncOutgoingStatuses(dbMessages)
                requestSeenStatuses(dbMessages)
                requestDeletedStatuses(dbMessages)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load messages from DB", e)
        }
    }

    private fun syncOutgoingStatuses(dbMessages: List<GroupMessageEntity>) {
        if (dbMessages.isEmpty()) return
        val updated = _messageDeliveryStatuses.value.toMutableMap()
        dbMessages.forEach { message ->
            if (message.sender == currentUsername) {
                updated[message.messageId] = if (message.isSeen) {
                    MessageDeliveryStatus.SEEN
                } else {
                    MessageDeliveryStatus.SENT
                }
            }
        }
        _messageDeliveryStatuses.value = updated
    }

    private fun markMessageAsSent(messageId: String?) {
        if (messageId.isNullOrBlank()) return
        val updated = _messageDeliveryStatuses.value.toMutableMap()
        updated.putIfAbsent(messageId, MessageDeliveryStatus.SENT)
        _messageDeliveryStatuses.value = updated
    }

    private suspend fun markMessageAsSeen(messageId: String?) {
        if (messageId.isNullOrBlank()) return
        groupRepository.markGroupMessageSeenByMessageId(messageId)
        val updated = _messageDeliveryStatuses.value.toMutableMap()
        updated[messageId] = MessageDeliveryStatus.SEEN
        _messageDeliveryStatuses.value = updated
    }

    private fun clearMessageStatus(messageId: String?) {
        if (messageId.isNullOrBlank()) return
        val updated = _messageDeliveryStatuses.value.toMutableMap()
        updated.remove(messageId)
        _messageDeliveryStatuses.value = updated
    }

    private fun requestSeenStatuses(messages: List<GroupMessageEntity> = _messages.value) {
        val activeSocket = socket ?: return
        if (!activeSocket.connected()) return

        val pendingIds = messages
            .asSequence()
            .filter { it.sender == currentUsername }
            .filter { !it.isSeen }
            .map { it.messageId }
            .distinct()
            .toList()
        if (pendingIds.isEmpty()) return

        activeSocket.emit(
            "get_group_seen_messages",
            JSONObject().apply {
                put("group_id", groupId)
                put("message_ids", JSONArray(pendingIds))
            }
        )
    }

    private fun requestDeletedStatuses(messages: List<GroupMessageEntity> = _messages.value) {
        val activeSocket = socket ?: return
        if (!activeSocket.connected()) return

        val existingIds = messages
            .map { it.messageId }
            .distinct()
        if (existingIds.isEmpty()) return

        activeSocket.emit(
            "get_group_deleted_messages",
            JSONObject().apply {
                put("group_id", groupId)
                put("message_ids", JSONArray(existingIds))
            }
        )
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
                    applyMemberPublicKeys(res.data)
                    connectSocket()
                }
                is Resource.Error -> {
                    _isLoading.value = false
                    _socketError.value = res.message.asString(getApplication<DinoOrderApplication>())
                }
            }
        }
    }

    private fun applyMemberPublicKeys(group: Group) {
        val keys = mutableMapOf<String, PublicKey>()
        val profileImages = mutableMapOf<String, String?>()
        group.members?.forEach { member ->
            profileImages[member.username] = member.profileImageUrl
            val pk = member.publicKey
            if (!pk.isNullOrBlank()) {
                try {
                    keys[member.username] = decodePublicKey(pk)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode public key for ${member.username}", e)
                }
            }
        }
        _memberProfileImageUrls.value = profileImages
        memberPublicKeys = keys
        memberKeysReady = keys.isNotEmpty()
        Log.d(TAG, "Loaded ${keys.size} member public keys for group $groupId")
    }

    private fun refreshMemberPublicKeysSilently() = viewModelScope.launch {
        getGroupDetailUseCase.execute(groupId).collect { res ->
            when (res) {
                is Resource.Success -> {
                    applyMemberPublicKeys(res.data)
                }
                is Resource.Error -> {
                    Log.w(TAG, "Failed to refresh member keys: ${res.message.asString(getApplication<DinoOrderApplication>())}")
                }
                is Resource.Loading -> Unit
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
            _decryptionError.value =
                "Unable to decrypt some group messages. Your private key may not match your account key."
            return "\u26A0\uFE0F Could not decrypt: no key for your user"
        }

        return try {
            val privateKeyStr = spref.getPrivateKey() ?: run {
                _decryptionError.value =
                    "Unable to decrypt some group messages. Your private key is missing on this device."
                return "\u26A0\uFE0F Could not decrypt: missing private key"
            }
            decryptMessage(
                encryptedMessage = encryptedMessage,
                encryptedAESKey = myEncryptedKey,
                privateKey = decodePrivateKey(privateKeyStr),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt group message", e)
            _decryptionError.value =
                "Unable to decrypt some group messages. Your private key may not match your account key."
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
            requestSeenStatuses()
            requestDeletedStatuses()
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

                    val ackedIds = withContext(Dispatchers.Default) {
                        val processedIds = mutableListOf<String>()
                        repeat(messagesArr.length()) { index ->
                            val item = messagesArr.optJSONObject(index) ?: return@repeat
                            val messageId = item.optString("message_id", "")
                            if (messageId.isBlank() || !seenMessageIds.add(messageId)) {
                                if (messageId.isNotBlank()) processedIds.add(messageId)
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
                            if (messageId.isNotBlank()) processedIds.add(messageId)
                        }
                        processedIds
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

                    withContext(Dispatchers.Default) {
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
                    }
                    loadMessagesFromDb()
                    if (sender != currentUsername && messageId.isNotBlank()) {
                        sendGroupAck(listOf(messageId))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling new_group_message", e)
                }
            }
        }

        socket.on("group_message_sent") { args ->
            val payload = parseSocketPayload(args)
            val messageId = payload?.optString("message_id", "")
            markMessageAsSent(messageId)
            Log.d(TAG, "group_message_sent ack received: $messageId")
        }

        socket.on("group_message_seen") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                val payloadGroupId = payload.optInt("group_id", -1)
                if (payloadGroupId != groupId) return@launch

                val seenBy = payload.optString("seen_by", "")
                if (seenBy == currentUsername) return@launch

                val messageId = payload.optString("message_id", "")
                if (messageId.isBlank()) return@launch
                markMessageAsSeen(messageId)
            }
        }

        socket.on("group_seen_messages_status") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                if (payload.optInt("group_id", -1) != groupId) return@launch
                val messageIds = payload.optJSONArray("message_ids") ?: return@launch
                for (index in 0 until messageIds.length()) {
                    val messageId = messageIds.optString(index, "").trim()
                    if (messageId.isNotBlank()) {
                        markMessageAsSeen(messageId)
                    }
                }
            }
        }

        socket.on("group_message_deleted") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                val payloadGroupId = payload.optInt("group_id", -1)
                if (payloadGroupId != groupId) return@launch

                val messageId = payload.optString("message_id", "")
                if (messageId.isBlank()) return@launch
                groupRepository.deleteGroupMessageByMessageId(messageId)
                clearMessageStatus(messageId)
                loadMessagesFromDb()
            }
        }

        socket.on("group_deleted_messages_status") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                if (payload.optInt("group_id", -1) != groupId) return@launch
                val messageIds = payload.optJSONArray("message_ids") ?: return@launch
                for (index in 0 until messageIds.length()) {
                    val messageId = messageIds.optString(index, "").trim()
                    if (messageId.isNotBlank()) {
                        groupRepository.deleteGroupMessageByMessageId(messageId)
                        clearMessageStatus(messageId)
                    }
                }
                loadMessagesFromDb()
            }
        }

        socket.on("message_error") { args ->
            viewModelScope.launch {
                val payload = extractPayloadArg(args)
                val errorMsg = if (payload is JSONObject) payload.optString("error", "Unknown error") else "Unknown error"
                _socketError.value = errorMsg
                Log.e(TAG, "message_error: $errorMsg")
            }
        }

        socket.on("user_public_key_updated") { args ->
            val payload = parseSocketPayload(args) ?: return@on
            val updatedUsername = payload.optString("username", "")
            if (updatedUsername.isBlank()) return@on

            val shouldRefresh = memberPublicKeys.containsKey(updatedUsername) ||
                payloadGroupIdsContainsCurrentGroup(payload)
            if (!shouldRefresh) return@on

            Log.d(TAG, "Detected updated public key for $updatedUsername, refreshing group keys")
            refreshMemberPublicKeysSilently()
        }

        socket.on("group_member_key_updated") { args ->
            val payload = parseSocketPayload(args) ?: return@on
            val payloadGroupId = payload.optInt("group_id", -1)
            if (payloadGroupId != groupId) return@on

            Log.d(TAG, "Received group_member_key_updated for group $groupId")
            refreshMemberPublicKeysSilently()
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

    fun dismissDecryptionError() {
        _decryptionError.value = null
    }

    private suspend fun deleteGroupMessageLocally(message: GroupMessageEntity) {
        groupRepository.deleteGroupMessageByMessageId(message.messageId)
        clearMessageStatus(message.messageId)
        loadMessagesFromDb()
    }

    fun deleteMessageOnlyForMe(message: GroupMessageEntity) = viewModelScope.launch {
        deleteGroupMessageLocally(message)
    }

    fun deleteMessageForEveryone(message: GroupMessageEntity) = viewModelScope.launch {
        deleteGroupMessageLocally(message)
        val activeSocket = socket
        if (activeSocket == null || !activeSocket.connected()) {
            _socketError.value = "Unable to delete for everyone while disconnected."
            connectSocket()
            return@launch
        }
        activeSocket.emit(
            "delete_group_message",
            JSONObject().apply {
                put("group_id", groupId)
                put("message_id", message.messageId)
            }
        )
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }

    fun openPrivateKeyResetDialog() {
        navigateToDestination(PrivateKeyResetDialogScreenDestination, viewModelScope)
    }

    fun openGroupMembersScreen(currentGroupName: String) {
        navigateToDestination(
            GroupMembersScreenDestination(
                groupId = groupId,
                groupName = currentGroupName.ifBlank { groupName },
            ),
            viewModelScope,
        )
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

    private fun parseSocketPayload(args: Array<out Any>): JSONObject? {
        val raw = extractPayloadArg(args) ?: return null
        return when (raw) {
            is JSONObject -> raw
            is String -> runCatching { JSONObject(raw) }.getOrNull()
            else -> null
        }
    }

    private fun payloadGroupIdsContainsCurrentGroup(payload: JSONObject): Boolean {
        val groupIds = payload.optJSONArray("group_ids") ?: return false
        for (index in 0 until groupIds.length()) {
            if (groupIds.optInt(index, -1) == groupId) return true
        }
        return false
    }

    override fun onCleared() {
        ActiveGroupTracker.clearActiveGroup()
        voiceRecorder?.release()
        disconnectSocket()
        super.onCleared()
    }
}
