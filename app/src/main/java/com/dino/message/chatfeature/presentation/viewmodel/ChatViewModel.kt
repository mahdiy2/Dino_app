package com.dino.message.chatfeature.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.chatfeature.domain.model.MessageDeliveryStatus
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.chatfeature.domain.usecase.ClearMessageFromDBUseCase
import com.dino.message.chatfeature.domain.usecase.GetMessageFromDBUseCase
import com.dino.message.chatfeature.domain.usecase.GetReceiverPublicKeyUseCase
import com.dino.message.chatfeature.domain.usecase.SaveMessageInDbUseCase
import com.dino.message.chatfeature.domain.usecase.UploadAttachmentUseCase
import com.dino.message.corefeature.data.socket.AppSocketManager
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.profilefeature.domain.usecase.FollowUserUseCase
import com.dino.message.corefeature.presentation.util.CryptoUtil
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.util.decodePrivateKey
import com.dino.message.corefeature.presentation.util.decodePublicKey
import com.dino.message.corefeature.presentation.util.decodePublicKeyOrNull
import com.dino.message.corefeature.presentation.util.decryptMessage
import com.dino.message.corefeature.presentation.util.formatPublicKeyForLog
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.domain.util.LocalHistoryPaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.dino.message.chatfeature.data.notification.ActiveChatTracker
import com.dino.message.chatfeature.data.notification.NotificationHelper
import com.dino.message.chatfeature.domain.model.PrivateHistoryMessage
import java.util.ArrayDeque
import java.util.UUID
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlin.math.abs
import com.dino.message.chatfeature.domain.util.MessageDeduplicator
import com.dino.message.chatfeature.domain.repository.ChatRepository
import com.dino.message.threadfeature.domain.usecase.IsTrackInPlaylistUseCase
import com.ramcosta.composedestinations.generated.destinations.PrivateKeyResetDialogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PlaylistBottomSheetScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SharedPostDetailsScreenDestination
import java.security.PublicKey
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
    private val chatRepository: ChatRepository,
    private val isTrackInPlaylistUseCase: IsTrackInPlaylistUseCase,
    private val appSocketManager: AppSocketManager,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    companion object {
        private const val HISTORY_PAGE_SIZE = 50
        private const val LOCAL_DB_PAGE_SIZE = 100
        private val HISTORY_STALE_AFTER: Duration = Duration.ofMinutes(2)
        private const val PRIVATE_DECRYPTION_FALLBACK = "Could not decrypt this message"
        private const val SENT_MESSAGE_PLACEHOLDER = "Sent message"
    }

    private data class SenderEncryptedPayload(
        val encryptedMessage: String,
        val encryptedKey: String,
    )

    private data class PendingSentResolution(
        val clientMessageId: String,
        val message: MessageEntity,
    )

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val receiverPublicKey = mutableStateOf<String?>(null)
    private var receiverEncryptionPublicKey: PublicKey? = null

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _messageDeliveryStatuses = MutableStateFlow<Map<String, MessageDeliveryStatus>>(emptyMap())
    val messageDeliveryStatuses: StateFlow<Map<String, MessageDeliveryStatus>> = _messageDeliveryStatuses.asStateFlow()

    private val _socketDebugEvents = MutableStateFlow<List<String>>(emptyList())
    val socketDebugEvents: StateFlow<List<String>> = _socketDebugEvents.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingOlderHistory = MutableStateFlow(false)
    val isLoadingOlderHistory: StateFlow<Boolean> = _isLoadingOlderHistory.asStateFlow()

    private val _canLoadOlderHistory = MutableStateFlow(false)
    val canLoadOlderHistory: StateFlow<Boolean> = _canLoadOlderHistory.asStateFlow()

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

    private val _showBlockedDialog = MutableStateFlow(false)
    val showBlockedDialog: StateFlow<Boolean> = _showBlockedDialog.asStateFlow()

    private val _blockedDialogMessage = MutableStateFlow<String?>(null)
    val blockedDialogMessage: StateFlow<String?> = _blockedDialogMessage.asStateFlow()

    private val _replyingTo = MutableStateFlow<MessageEntity?>(null)
    val replyingTo: StateFlow<MessageEntity?> = _replyingTo.asStateFlow()

    private var getPublicKey = false
    private var isSocketConnecting = false
    private val socketOwner = "ChatViewModel-$chatId"
    private val currentUsername: String = spref.getUsername().orEmpty().ifBlank { "self" }
    private val pendingSentMessagesByClientId = linkedMapOf<String, MessageEntity>()
    private val pendingSentMessageOrder = ArrayDeque<String>()
    private val pendingVisibleMessagesByClientId = linkedMapOf<String, MessageEntity>()
    private var latestDbMessages: List<MessageEntity> = emptyList()
    private var lastPendingMessageTimestamp: Instant = Instant.EPOCH

    private val deduplicator = MessageDeduplicator(app)
    private val messagesMutex = Mutex()
    private val historyMutex = Mutex()
    private var privateHistoryCursorInitialized = false
    private var privateHistoryHasMore = false
    private var privateHistoryNextBefore: String? = null
    private var loadedLocalMessageCount = 0
    private var totalLocalMessageCount = 0

    init {
        loadMessages(resetPaging = true)
        viewModelScope.launch {
            syncHistoryOnOpen()
        }
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
                    Log.d("ChatViewModel", "Fetched receiver public key for $chatId: ${formatPublicKeyForLog(publicKey)}")
                    if (publicKey.isNullOrBlank()) {
                        Log.e("ChatViewModel", "getReceiverPublicKey: empty public key for chatId=$chatId")
                        _publicKeyError.value = "Received empty public key"
                        _isPublicKeyLoading.value = false
                        getPublicKey = false
                        showRefreshing()
                        return@collect
                    }
                    val resolvedPublicKey = decodePublicKeyOrNull(publicKey)
                    if (resolvedPublicKey == null) {
                        Log.w(
                            "ChatViewModel",
                            "getReceiverPublicKey: unsupported public key format for chatId=$chatId, ${formatPublicKeyForLog(publicKey)}"
                        )
                        receiverPublicKey.value = null
                        receiverEncryptionPublicKey = null
                        _isPublicKeyReady.value = false
                        _publicKeyError.value = "This user's encryption key is invalid. Ask them to reset their key."
                        _isPublicKeyLoading.value = false
                        getPublicKey = false
                        showRefreshing()
                        return@collect
                    }

                    receiverPublicKey.value = publicKey
                    receiverEncryptionPublicKey = resolvedPublicKey
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

    private fun resolveReceiverEncryptionPublicKey(): PublicKey? {
        receiverEncryptionPublicKey?.let { return it }
        val rawKey = receiverPublicKey.value.orEmpty()
        if (rawKey.isBlank()) return null
        val decoded = decodePublicKeyOrNull(rawKey) ?: run {
            Log.w(
                "ChatViewModel",
                "resolveReceiverEncryptionPublicKey: unsupported key format for chatId=$chatId, ${formatPublicKeyForLog(rawKey)}"
            )
            receiverPublicKey.value = null
            _isPublicKeyReady.value = false
            _publicKeyError.value = "This user's encryption key is invalid. Ask them to reset their key."
            return null
        }
        receiverEncryptionPublicKey = decoded
        return decoded
    }

    fun loadMessages(resetPaging: Boolean = false) = viewModelScope.launch {
        messagesMutex.withLock {
            val dbMessages = loadMessagesFromDb(resetPaging = resetPaging)
            onMessagesLoaded(dbMessages)
        }
    }

    private suspend fun saveMessageToDb(message: MessageEntity) {
        Log.d("ChatViewModel", "saveMessageToDb: senderOrReceiver=${message.senderOrReceiver}, timestamp=${message.timestamp}")
        saveMessageInDbUseCase.execute(message).collect { }
    }

    fun saveMessage(message: MessageEntity) = viewModelScope.launch {
        saveMessageToDb(message)
        messagesMutex.withLock {
            val dbMessages = loadMessagesFromDb(resetPaging = false)
            onMessagesLoaded(dbMessages)
        }
    }

    fun clearMessages() = viewModelScope.launch {
        clearMessageFromDBUseCase.execute(chatId).collect {
            latestDbMessages = emptyList()
            pendingVisibleMessagesByClientId.clear()
            lastPendingMessageTimestamp = Instant.EPOCH
            _messageDeliveryStatuses.value = emptyMap()
            _messages.value = emptyList()
            privateHistoryCursorInitialized = false
            privateHistoryNextBefore = null
            privateHistoryHasMore = false
            loadedLocalMessageCount = 0
            totalLocalMessageCount = 0
            updateCanLoadOlderHistoryFlag()
        }
    }

    fun loadOlderHistoryIfNeeded() = viewModelScope.launch {
        historyMutex.withLock {
            if (_isLoadingOlderHistory.value) {
                return@withLock
            }

            if (loadOlderMessagesFromLocalIfAvailable()) {
                return@withLock
            }

            if (!privateHistoryHasMore) {
                updateCanLoadOlderHistoryFlag()
                return@withLock
            }

            val beforeCursor = privateHistoryNextBefore
                ?: oldestTimestampFromMessages(_messages.value)
                ?: run {
                    privateHistoryHasMore = false
                    updateCanLoadOlderHistoryFlag()
                    return@withLock
                }

            _isLoadingOlderHistory.value = true
            fetchPrivateHistoryPage(before = beforeCursor)
            _isLoadingOlderHistory.value = false
        }
    }

    private suspend fun syncHistoryOnOpen() {
        historyMutex.withLock {
            // Always merge the latest page on open to self-heal stale local cache entries.
            fetchPrivateHistoryPage(before = null)
        }
    }

    private suspend fun fetchPrivateHistoryPage(before: String?) {
        when (
            val result = chatRepository.getPrivateHistory(
                chatId = chatId,
                limit = HISTORY_PAGE_SIZE,
                before = before,
            )
        ) {
            is Resource.Success -> {
                val page = result.data
                resolvePendingMessagesFromHistory(page.messages)
                val mappedMessages = page.messages.mapNotNull { historyMessage ->
                    mapHistoryMessageToEntity(historyMessage)
                }
                mappedMessages.forEach { message ->
                    message.messageId?.let { deduplicator.isDuplicate(it) }
                }
                if (mappedMessages.isNotEmpty()) {
                    chatRepository.upsertMessagesByMessageId(mappedMessages)
                }
                updatePrivateHistoryCursor(page.hasMore, page.nextBefore, mappedMessages)
                loadMessages()
            }
            is Resource.Error -> {
                pushSocketDebugLog("History fetch failed: ${result.message.asString(getApplication())}")
            }
            is Resource.Loading -> Unit
        }
    }

    private fun seedPrivateHistoryCursorFromLocal(localMessages: List<MessageEntity>) {
        val oldestTimestamp = oldestTimestampFromMessages(localMessages)
        if (!privateHistoryCursorInitialized) {
            privateHistoryHasMore = !oldestTimestamp.isNullOrBlank()
            privateHistoryCursorInitialized = true
        }
        if (!oldestTimestamp.isNullOrBlank()) {
            privateHistoryNextBefore = oldestTimestamp
        }
        if (privateHistoryHasMore && privateHistoryNextBefore.isNullOrBlank()) {
            privateHistoryHasMore = false
        }
        updateCanLoadOlderHistoryFlag()
    }

    private fun shouldRefreshLatestHistory(localMessages: List<MessageEntity>): Boolean {
        val newestTimestamp = newestTimestampFromMessages(localMessages) ?: return true
        val newestInstant = runCatching { Instant.parse(newestTimestamp) }.getOrNull() ?: return true
        val age = Duration.between(newestInstant, Instant.now())
        return age > HISTORY_STALE_AFTER
    }

    private fun updatePrivateHistoryCursor(
        hasMore: Boolean,
        nextBefore: String?,
        mergedMessages: List<MessageEntity>,
    ) {
        privateHistoryCursorInitialized = true
        privateHistoryHasMore = hasMore
        privateHistoryNextBefore = nextBefore?.takeIf { it.isNotBlank() }
            ?: oldestTimestampFromMessages(mergedMessages)
            ?: privateHistoryNextBefore

        if (privateHistoryHasMore && privateHistoryNextBefore.isNullOrBlank()) {
            privateHistoryHasMore = false
        }
        updateCanLoadOlderHistoryFlag()
    }

    private suspend fun loadMessagesFromDb(resetPaging: Boolean): List<MessageEntity> {
        val previouslyLoadedCount = loadedLocalMessageCount
        val previousTotalCount = totalLocalMessageCount
        totalLocalMessageCount = chatRepository.getMessageCount(chatId)
        val queryLimit = LocalHistoryPaging.resolveQueryLimit(
            resetPaging = resetPaging,
            previouslyLoadedCount = previouslyLoadedCount,
            previousTotalCount = previousTotalCount,
            currentTotalCount = totalLocalMessageCount,
            pageSize = LOCAL_DB_PAGE_SIZE,
        )
        var dbMessages: List<MessageEntity> = emptyList()
        if (queryLimit > 0) {
            getMessageFromDBUseCase.execute(chatId = chatId, limit = queryLimit, offset = 0).collect { loaded ->
                dbMessages = loaded
            }
        }
        loadedLocalMessageCount = queryLimit
        latestDbMessages = dbMessages
        resolvePendingVisibleMessagesFromDb(dbMessages)
        _messages.value = mergeDbAndPendingMessages(dbMessages)
        seedPrivateHistoryCursorFromLocal(dbMessages)
        return dbMessages
    }

    private suspend fun loadOlderMessagesFromLocalIfAvailable(): Boolean {
        if (loadedLocalMessageCount >= totalLocalMessageCount) {
            return false
        }
        loadedLocalMessageCount = (loadedLocalMessageCount + LOCAL_DB_PAGE_SIZE).coerceAtMost(totalLocalMessageCount)
        val dbMessages = loadMessagesFromDb(resetPaging = false)
        onMessagesLoaded(dbMessages)
        return true
    }

    private fun onMessagesLoaded(dbMessages: List<MessageEntity>) {
        syncOutgoingStatuses(dbMessages)
        requestDeliveredStatuses(dbMessages)
        requestSeenStatuses(dbMessages)
        requestDeletedStatuses(dbMessages)
    }

    private fun updateCanLoadOlderHistoryFlag() {
        val hasMoreLocal = loadedLocalMessageCount < totalLocalMessageCount
        _canLoadOlderHistory.value = hasMoreLocal || privateHistoryHasMore
    }

    private fun newestTimestampFromMessages(messages: List<MessageEntity>): String? {
        return messages.firstOrNull { !it.messageId.isNullOrBlank() && it.timestamp.isNotBlank() }?.timestamp
    }

    private fun oldestTimestampFromMessages(messages: List<MessageEntity>): String? {
        return messages.lastOrNull { !it.messageId.isNullOrBlank() && it.timestamp.isNotBlank() }?.timestamp
    }

    private fun mapHistoryMessageToEntity(historyMessage: PrivateHistoryMessage): MessageEntity? {
        val sender = historyMessage.from.trim()
        if (sender.isBlank()) return null

        val messageContent = decryptHistoryMessageText(historyMessage)
        val replyPreview = decryptHistoryReplyPreview(historyMessage)

        return MessageEntity(
            messageId = historyMessage.messageId,
            chatId = chatId,
            senderOrReceiver = sender,
            content = messageContent,
            timestamp = historyMessage.timestamp,
            messageType = historyMessage.type,
            attachmentType = historyMessage.attachment?.type,
            attachmentUrl = historyMessage.attachment?.url,
            attachmentMimeType = historyMessage.attachment?.mimeType,
            attachmentFileName = historyMessage.attachment?.fileName,
            attachmentSizeBytes = historyMessage.attachment?.sizeBytes,
            replyToMessageId = historyMessage.replyToMessageId,
            replyToSender = historyMessage.replyToSender,
            replyPreview = replyPreview,
        )
    }

    private fun decryptHistoryMessageText(historyMessage: PrivateHistoryMessage): String {
        val encryptedMessage = historyMessage.message.orEmpty()
        val encryptedKey = historyMessage.encryptedKey.orEmpty()
        if (encryptedMessage.isBlank() || encryptedKey.isBlank()) {
            return ""
        }
        return runCatching {
            decryptMessage(
                encryptedMessage = encryptedMessage,
                encryptedAESKey = encryptedKey,
                privateKey = decodePrivateKey(spref.getPrivateKey() ?: ""),
            )
        }.getOrElse {
            if (historyMessage.from == currentUsername) {
                SENT_MESSAGE_PLACEHOLDER
            } else {
                PRIVATE_DECRYPTION_FALLBACK
            }
        }
    }

    private fun decryptHistoryReplyPreview(historyMessage: PrivateHistoryMessage): String? {
        val encryptedPreview = historyMessage.encryptedReplyPreview ?: return null
        val decryptKey = historyMessage.encryptedReplyKey ?: historyMessage.encryptedKey ?: return null
        return runCatching {
            decryptMessage(
                encryptedMessage = encryptedPreview,
                encryptedAESKey = decryptKey,
                privateKey = decodePrivateKey(spref.getPrivateKey() ?: ""),
            )
        }.getOrNull()
    }

    private fun syncOutgoingStatuses(dbMessages: List<MessageEntity>) {
        if (dbMessages.isEmpty()) return
        val updated = _messageDeliveryStatuses.value.toMutableMap()
        dbMessages.forEach { message ->
            val messageId = message.messageId
            if (!messageId.isNullOrBlank() && message.senderOrReceiver != chatId) {
                val existingStatus = updated[messageId]
                updated[messageId] = when {
                    message.isSeen || existingStatus == MessageDeliveryStatus.SEEN -> MessageDeliveryStatus.SEEN
                    existingStatus == MessageDeliveryStatus.DELIVERED -> MessageDeliveryStatus.DELIVERED
                    else -> MessageDeliveryStatus.SENT
                }
            }
        }
        _messageDeliveryStatuses.value = updated
    }

    private fun markMessageAsSent(messageId: String?) {
        if (messageId.isNullOrBlank()) return
        val updated = _messageDeliveryStatuses.value.toMutableMap()
        when (updated[messageId]) {
            MessageDeliveryStatus.DELIVERED,
            MessageDeliveryStatus.SEEN -> return
            else -> {
                updated[messageId] = MessageDeliveryStatus.SENT
                _messageDeliveryStatuses.value = updated
            }
        }
    }

    private suspend fun markMessageAsSeen(messageId: String?) {
        if (messageId.isNullOrBlank()) return
        chatRepository.markMessageSeenByMessageId(messageId)
        val updated = _messageDeliveryStatuses.value.toMutableMap()
        updated[messageId] = MessageDeliveryStatus.SEEN
        _messageDeliveryStatuses.value = updated
    }

    private fun markMessageAsDelivered(messageId: String?) {
        if (messageId.isNullOrBlank()) return
        val updated = _messageDeliveryStatuses.value.toMutableMap()
        if (updated[messageId] != MessageDeliveryStatus.SEEN) {
            updated[messageId] = MessageDeliveryStatus.DELIVERED
            _messageDeliveryStatuses.value = updated
        }
    }

    private fun clearMessageStatus(messageId: String?) {
        if (messageId.isNullOrBlank()) return
        val updated = _messageDeliveryStatuses.value.toMutableMap()
        updated.remove(messageId)
        _messageDeliveryStatuses.value = updated
    }

    private fun requestDeliveredStatuses(messages: List<MessageEntity> = latestDbMessages) {
        if (!appSocketManager.isConnected()) return

        val outgoingIds = messages
            .asSequence()
            .filter { it.senderOrReceiver != chatId }
            .mapNotNull { it.messageId?.takeIf { id -> id.isNotBlank() } }
            .distinct()
            .toList()
        if (outgoingIds.isEmpty()) return

        appSocketManager.emit(
            "get_delivered_messages",
            JSONObject().apply {
                put("chat_id", chatId)
                put("message_ids", JSONArray(outgoingIds))
            }
        )
    }

    private fun requestSeenStatuses(messages: List<MessageEntity> = latestDbMessages) {
        if (!appSocketManager.isConnected()) return

        val pendingIds = messages
            .asSequence()
            .filter { it.senderOrReceiver != chatId }
            .filter { !it.isSeen }
            .mapNotNull { it.messageId?.takeIf { id -> id.isNotBlank() } }
            .distinct()
            .toList()
        if (pendingIds.isEmpty()) return

        appSocketManager.emit(
            "get_seen_messages",
            JSONObject().apply {
                put("chat_id", chatId)
                put("message_ids", JSONArray(pendingIds))
            }
        )
    }

    private fun requestDeletedStatuses(messages: List<MessageEntity> = latestDbMessages) {
        if (!appSocketManager.isConnected()) return

        val existingIds = messages
            .mapNotNull { it.messageId?.takeIf { id -> id.isNotBlank() } }
            .distinct()
        if (existingIds.isEmpty()) return

        appSocketManager.emit(
            "get_deleted_messages",
            JSONObject().apply {
                put("chat_id", chatId)
                put("message_ids", JSONArray(existingIds))
            }
        )
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }

    fun openPrivateKeyResetDialog() {
        navigateToDestination(PrivateKeyResetDialogScreenDestination, viewModelScope)
    }

    fun openSharedPost(postId: Long) {
        navigateToDestination(
            SharedPostDetailsScreenDestination(postId = postId),
            viewModelScope
        )
    }

    fun openPeerProfile() {
        navigateToDestination(
            ProfileScreenDestination(username = chatId),
            viewModelScope
        )
    }

    fun openPlaylistBottomSheet() {
        navigateToDestination(PlaylistBottomSheetScreenDestination, viewModelScope)
    }

    fun openPlaylistBottomSheetIfTrackExists(trackUrl: String) = viewModelScope.launch {
        val normalizedUrl = trackUrl.trim()
        if (normalizedUrl.isBlank()) return@launch

        isTrackInPlaylistUseCase.execute(trackUrl = normalizedUrl).collect { res ->
            if (res is Resource.Success) {
                if (res.data) {
                    openPlaylistBottomSheet()
                }
            }
        }
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

    fun acknowledgeBlockedDialogAndExit() {
        _showBlockedDialog.value = false
        _blockedDialogMessage.value = null
        navigateUp(viewModelScope)
    }

    fun setReplyingTo(message: MessageEntity) {
        _replyingTo.value = message
    }

    fun clearReplyingTo() {
        _replyingTo.value = null
    }

    private suspend fun deleteMessageLocally(message: MessageEntity) {
        if (!message.messageId.isNullOrBlank()) {
            chatRepository.deleteMessageByMessageId(message.messageId)
            clearMessageStatus(message.messageId)
        } else {
            chatRepository.deleteMessageByLocalId(message.id)
        }
        loadMessages()
    }

    fun deleteMessageOnlyForMe(message: MessageEntity) = viewModelScope.launch {
        deleteMessageLocally(message)

        val messageId = message.messageId
        if (messageId.isNullOrBlank()) return@launch

        val emitted = appSocketManager.emit(
            "delete_message_for_me",
            JSONObject().apply {
                put("chat_id", chatId)
                put("message_id", messageId)
            }
        )
        if (!emitted) {
            _socketError.value = "Unable to sync message deletion while offline."
        }
    }

    fun deleteMessageForEveryone(message: MessageEntity) = viewModelScope.launch {
        deleteMessageLocally(message)

        val messageId = message.messageId
        if (messageId.isNullOrBlank()) {
            return@launch
        }

        val emitted = appSocketManager.emit(
            event = "delete_message",
            payload = JSONObject().apply {
                put("chat_id", chatId)
                put("message_id", messageId)
            },
        )
        if (emitted) {
            pushSocketDebugLog("Requested delete_message for $messageId")
        } else {
            _socketError.value = "Unable to delete for everyone right now."
            pushSocketDebugLog("delete_message emit failed for $messageId")
        }
    }

    fun retrySocketConnection() {
        _socketError.value = null
        connectSocketIfNeeded(forceReconnect = true)
    }

    private fun showRefreshing() {
        _isRefreshing.value = getPublicKey || isSocketConnecting
    }

    private fun triggerBlockedDialog(message: String?) {
        if (_showBlockedDialog.value) {
            return
        }
        _blockedDialogMessage.value = message ?: "You have been blocked and can no longer chat with this user."
        _showBlockedDialog.value = true
    }

    fun sendMessage(message: String) {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty()) {
            pushSocketDebugLog("Ignoring empty message")
            return
        }
        val clientMessageId = generateClientMessageId()
        val replyTo = _replyingTo.value

        val publicKey = receiverPublicKey.value
        if (publicKey.isNullOrBlank() || resolveReceiverEncryptionPublicKey() == null) {
            pushSocketDebugLog("Cannot send: missing receiver public key")
            return
        }

        emitEncryptedMessage(
            message = trimmedMessage,
            replyTo = replyTo,
            clientMessageId = clientMessageId,
        )
    }

    fun sendAttachment(fileUri: Uri, caption: String? = null) {
        val publicKey = receiverPublicKey.value
        if (publicKey.isNullOrBlank() || resolveReceiverEncryptionPublicKey() == null) {
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

                        emitAttachmentMessage(
                            attachment = attachment,
                            caption = caption,
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
        replyTo: MessageEntity? = _replyingTo.value,
        clientMessageId: String = generateClientMessageId(),
    ) {
        val receiverKey = resolveReceiverEncryptionPublicKey() ?: return

        try {
            val encryptedCaption: String?
            val encryptedKey: String
            val senderEncryptedCaption: SenderEncryptedPayload?

            if (!caption.isNullOrBlank()) {
                val (ek, em) = CryptoUtil.encryptMessage(
                    message = caption,
                    receiverPublicKey = receiverKey
                )
                encryptedKey = ek
                encryptedCaption = em
                senderEncryptedCaption = encryptForCurrentUser(caption)
            } else {
                val (ek, _) = CryptoUtil.encryptMessage(
                    message = "",
                    receiverPublicKey = receiverKey
                )
                encryptedKey = ek
                encryptedCaption = null
                senderEncryptedCaption = null
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
                put("client_message_id", clientMessageId)
                if (encryptedCaption != null) {
                    put("message", encryptedCaption)
                }
                if (senderEncryptedCaption != null) {
                    put("sender_encrypted_message", senderEncryptedCaption.encryptedMessage)
                    put("sender_encrypted_key", senderEncryptedCaption.encryptedKey)
                }
                if (replyTo != null) {
                    put("reply_to_message_id", replyTo.messageId ?: "")
                    put("reply_to_sender", replyTo.senderOrReceiver)
                   val preview = replyTo.content.take(100)
                   if (preview.isNotBlank()) {
                        val (encReplyKey, encReplyPreview) = CryptoUtil.encryptMessage(
                           message = preview,
                           receiverPublicKey = receiverKey
                       )
                      put("encrypted_reply_preview", encReplyPreview)
                       put("encrypted_reply_key", encReplyKey)
                   }
                }
            }

            pushSocketDebugLog("Socket emit send_message (${attachment.type}) to $chatId")
            trackPendingSentMessage(
                clientMessageId = clientMessageId,
                pendingMessage = MessageEntity(
                    senderOrReceiver = currentUsername,
                    chatId = chatId,
                    content = caption ?: "",
                    timestamp = "",
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
            addPendingVisibleMessage(
                clientMessageId = clientMessageId,
                pendingMessage = MessageEntity(
                    messageId = "pending:$clientMessageId",
                    senderOrReceiver = currentUsername,
                    chatId = chatId,
                    content = caption ?: "",
                    timestamp = nextPendingMessageTimestamp().toString(),
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
            schedulePendingHistoryReconcile(clientMessageId)
            emitPrivateMessageWithRetry(
                payload = payload,
                clientMessageId = clientMessageId,
            )
            _replyingTo.value = null
        } catch (e: Exception) {
            pushSocketDebugLog("Attachment send failed: ${e.message ?: "unknown error"}")
            Log.e("ChatViewModel", "emitAttachmentMessage: failed", e)
        }
    }

    private fun emitEncryptedMessage(
        message: String,
        replyTo: MessageEntity? = _replyingTo.value,
        clientMessageId: String = generateClientMessageId(),
    ) {
        val receiverKey = resolveReceiverEncryptionPublicKey() ?: return

        try {
            val (encryptedKey, encryptedMessage) = CryptoUtil.encryptMessage(
                message = message,
                receiverPublicKey = receiverKey
            )
            val senderEncryptedMessage = encryptForCurrentUser(message)

            val payload = JSONObject().apply {
                put("to", chatId)
                put("message", encryptedMessage)
                put("encrypted_key", encryptedKey)
                if (senderEncryptedMessage != null) {
                    put("sender_encrypted_message", senderEncryptedMessage.encryptedMessage)
                    put("sender_encrypted_key", senderEncryptedMessage.encryptedKey)
                }
                put("type", "text")
                put("client_message_id", clientMessageId)
                if (replyTo != null) {
                    put("reply_to_message_id", replyTo.messageId ?: "")
                    put("reply_to_sender", replyTo.senderOrReceiver)
                    val preview = replyTo.content.take(100)
                    if (preview.isNotBlank()) {
                        val (encReplyKey, encReplyPreview) = CryptoUtil.encryptMessage(
                            message = preview,
                            receiverPublicKey = receiverKey
                        )
                       put("encrypted_reply_preview", encReplyPreview)
                        put("encrypted_reply_key", encReplyKey)
                   }
                }
            }

            pushSocketDebugLog("Socket emit send_message to $chatId")
            trackPendingSentMessage(
                clientMessageId = clientMessageId,
                pendingMessage = MessageEntity(
                    senderOrReceiver = currentUsername,
                    chatId = chatId,
                    content = message,
                    timestamp = "",
                    messageType = "text",
                    replyToMessageId = replyTo?.messageId,
                    replyToSender = replyTo?.senderOrReceiver,
                    replyPreview = replyTo?.content?.take(100)
                )
            )
            addPendingVisibleMessage(
                clientMessageId = clientMessageId,
                pendingMessage = MessageEntity(
                    messageId = "pending:$clientMessageId",
                    senderOrReceiver = currentUsername,
                    chatId = chatId,
                    content = message,
                    timestamp = nextPendingMessageTimestamp().toString(),
                    messageType = "text",
                    replyToMessageId = replyTo?.messageId,
                    replyToSender = replyTo?.senderOrReceiver,
                    replyPreview = replyTo?.content?.take(100)
                )
            )
            schedulePendingHistoryReconcile(clientMessageId)
            emitPrivateMessageWithRetry(
                payload = payload,
                clientMessageId = clientMessageId,
            )
            Log.d("ChatViewModel", "sendMessage: queued/emitted send_message to=$chatId")

            _replyingTo.value = null
        } catch (e: Exception) {
            pushSocketDebugLog("Encryption/send failed: ${e.message ?: "unknown error"}")
            Log.e("ChatViewModel", "sendMessage: failed", e)
        }
    }

    private fun emitPrivateMessageWithRetry(
        payload: JSONObject,
        clientMessageId: String,
    ) {
        when (
            appSocketManager.emitWithRetry(
                event = "send_message",
                payload = payload,
                dedupeKey = "private:$chatId:$clientMessageId",
            )
        ) {
            AppSocketManager.EmitWithRetryResult.EMITTED -> Unit
            AppSocketManager.EmitWithRetryResult.QUEUED,
            AppSocketManager.EmitWithRetryResult.ALREADY_QUEUED -> {
                _socketError.value = "Disconnected. Message queued and will retry automatically."
            }
            AppSocketManager.EmitWithRetryResult.QUEUE_FULL -> {
                _socketError.value = "Send queue is full. Please retry in a moment."
                markPendingMessageFailed(clientMessageId)
            }
        }
    }

    private fun connectSocketIfNeeded(forceReconnect: Boolean = false) {
        val connectPlan = ChatSocketConnectPlanner.plan(
            forceReconnect = forceReconnect,
            isConnected = appSocketManager.isConnected(),
            isSocketConnecting = isSocketConnecting,
        )

        if (connectPlan.shouldRegisterListeners) {
            // The app-wide notification manager may connect first, so always
            // attach chat listeners before making connect/return decisions.
            registerSocketListeners()
        }

        if (connectPlan.shouldRunConnectedSync) {
            isSocketConnecting = false
            showRefreshing()
            pushSocketDebugLog("Socket already connected; chat listeners ready")
            requestPeerStatus()
            requestDeliveredStatuses()
            requestSeenStatuses()
            requestDeletedStatuses()
            syncPrivateChatReadState()
            return
        }

        if (!connectPlan.shouldConnectSocket) {
            return
        }

        val connectStarted = appSocketManager.connect(forceReconnect = forceReconnect)
        if (!connectStarted) {
            pushSocketDebugLog("Socket token is missing")
            isSocketConnecting = false
            showRefreshing()
            return
        }
        isSocketConnecting = !appSocketManager.isConnected()
        showRefreshing()
        pushSocketDebugLog("Socket connect requested")
    }

    private fun sendAck(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        if (!ActiveChatTracker.isChattingWith(chatId)) {
            pushSocketDebugLog("Skipping ack for inactive chat $chatId")
            return
        }
        val payload = JSONObject().apply {
            put("chat_id", chatId)
            put("message_ids", JSONArray(messageIds))
        }
        appSocketManager.emit("ack_messages", payload)
        pushSocketDebugLog("Sent ack for ${messageIds.size} message(s)")
    }

    private fun sendRead(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        if (!ActiveChatTracker.isChattingWith(chatId)) {
            pushSocketDebugLog("Skipping read for inactive chat $chatId")
            return
        }
        val payload = JSONObject().apply {
            put("chat_id", chatId)
            put("message_ids", JSONArray(messageIds))
        }
        appSocketManager.emit("mark_read_messages", payload)
        pushSocketDebugLog("Sent read for ${messageIds.size} message(s)")
    }

    private fun syncPrivateChatReadState() {
        if (!appSocketManager.isConnected()) return
        appSocketManager.emit(
            "sync_private_chat_read_state",
            JSONObject().apply {
                put("chat_id", chatId)
            }
        )
        pushSocketDebugLog("Requested private chat read sync for $chatId")
    }

    private fun registerSocketListeners() {
        appSocketManager.registerListener(socketOwner, Socket.EVENT_CONNECT) {
            viewModelScope.launch {
                isSocketConnecting = false
                _socketError.value = null
                showRefreshing()
                pushSocketDebugLog("Socket connected")
                requestPeerStatus()
                requestDeliveredStatuses()
                requestSeenStatuses()
                requestDeletedStatuses()
                syncPrivateChatReadState()
            }
        }

        appSocketManager.registerListener(socketOwner, Socket.EVENT_CONNECT_ERROR) { args ->
            viewModelScope.launch {
                isSocketConnecting = false
                showRefreshing()
                val details = args.joinToString(separator = " | ") { it.toDebugString() }
                pushSocketDebugLog("Socket connect_error: ${details.ifBlank { "unknown" }}")
                _socketError.value = "Connection error. Please check your network."
            }
        }

        appSocketManager.registerListener(socketOwner, Socket.EVENT_DISCONNECT) { args ->
            viewModelScope.launch {
                isSocketConnecting = false
                showRefreshing()
                _isPeerOnline.value = false
                pushSocketDebugLog("Socket disconnected: ${args.firstOrNull()?.toDebugString() ?: "unknown"}")
                _socketError.value = "Disconnected from chat server."
            }
        }

        appSocketManager.registerListener(socketOwner, "connected") { args ->
            viewModelScope.launch {
                val raw = extractPayloadArg(args, "connected")
                pushSocketDebugLog("Socket auth ok: ${raw?.toDebugString() ?: "no payload"}")
            }
        }

        appSocketManager.registerListener(socketOwner, "private_chat_state_synced") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args, "private_chat_state_synced") ?: return@launch
                if (payload.optString("chat_id", "") != chatId) return@launch
                pushSocketDebugLog(
                    "Private read sync done: delivered=${payload.optInt("marked_delivered", 0)} seen=${payload.optInt("marked_seen", 0)}"
                )
            }
        }

        appSocketManager.registerListener(socketOwner, "pending_messages") { args ->
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
                val ackedIds = withContext(Dispatchers.Default) {
                    val processedIds = mutableListOf<String>()
                    repeat(messages.length()) { index ->
                        val item = messages.optJSONObject(index) ?: return@repeat
                        val msgId = item.optString("message_id", "")
                        val ackableMessageId = PrivateAckScope.ackableMessageId(
                            payload = item,
                            chatId = chatId,
                            currentlyActiveChatId = ActiveChatTracker.activeChatId.value,
                        )
                        if (msgId.isNotBlank() && deduplicator.isDuplicate(msgId)) {
                            pushSocketDebugLog("Skipping duplicate pending message $msgId")
                            if (ackableMessageId != null) {
                                processedIds.add(ackableMessageId)
                            }
                            return@repeat
                        }
                        handleIncomingMessage(item)
                        if (ackableMessageId != null) {
                            processedIds.add(ackableMessageId)
                        }
                    }
                    processedIds
                }
                loadMessages()
                if (ackedIds.isNotEmpty()) {
                    sendAck(ackedIds)
                    sendRead(ackedIds)
                }
            }
        }

        appSocketManager.registerListener(socketOwner, "new_message") { args ->
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
                val ackableMessageId = PrivateAckScope.ackableMessageId(
                    payload = payload,
                    chatId = chatId,
                    currentlyActiveChatId = ActiveChatTracker.activeChatId.value,
                )
                if (msgId.isNotBlank() && deduplicator.isDuplicate(msgId)) {
                    pushSocketDebugLog("Skipping duplicate new_message $msgId")
                    if (ackableMessageId != null) {
                        sendAck(listOf(ackableMessageId))
                        sendRead(listOf(ackableMessageId))
                    }
                    return@launch
                }
                pushSocketDebugLog("Received live message from ${payload.optString("from")}")
                withContext(Dispatchers.Default) {
                    handleIncomingMessage(payload)
                }
                loadMessages()
                if (ackableMessageId != null) {
                    sendAck(listOf(ackableMessageId))
                    sendRead(listOf(ackableMessageId))
                }
            }
        }

        appSocketManager.registerListener(socketOwner, "message_sent") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args, "message_sent") ?: return@launch
                val detail = payload.toDebugString()
                pushSocketDebugLog("✅ message_sent: $detail")
                Log.d("ChatViewModel", "message_sent ack received: $detail")
                val to = payload.optString("to", "")
                val serverTimestamp = payload.optString("timestamp", "")
                val serverMessageId = payload.optString("message_id", "")
                val clientMessageId = payload.optString("client_message_id", "").ifBlank { null }

                if (serverMessageId.isBlank()) return@launch
                if (to.isNotBlank() && to != chatId) return@launch

                val pending = consumePendingSentMessage(clientMessageId)

                if (pending != null) {
                    val message = MessageEntity(
                        messageId = serverMessageId,
                        senderOrReceiver = pending.message.senderOrReceiver,
                        chatId = pending.message.chatId,
                        content = pending.message.content,
                        timestamp = serverTimestamp,
                        messageType = pending.message.messageType,
                        attachmentType = pending.message.attachmentType,
                        attachmentUrl = pending.message.attachmentUrl,
                        attachmentMimeType = pending.message.attachmentMimeType,
                        attachmentFileName = pending.message.attachmentFileName,
                        attachmentSizeBytes = pending.message.attachmentSizeBytes,
                        replyToMessageId = pending.message.replyToMessageId,
                        replyToSender = pending.message.replyToSender,
                        replyPreview = pending.message.replyPreview
                    )
                    saveMessageToDb(message)
                    loadMessages()
                    markMessageAsSent(serverMessageId)
                    pushSocketDebugLog("Saved sent message")
                } else {
                    clientMessageId?.let { removePendingVisibleMessage(it) }
                    pushSocketDebugLog(
                        "message_sent could not resolve pending message (client_message_id=${clientMessageId ?: "none"})"
                    )
                    // Recover from missed in-memory correlation by re-syncing latest server history.
                    fetchPrivateHistoryPage(before = null)
                }
            }
        }

        appSocketManager.registerListener(socketOwner, "message_seen") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args, "message_seen") ?: return@launch
                val targetChat = payload.optString("chat_id", "")
                val messageId = payload.optString("message_id", "")
                if (targetChat != chatId || messageId.isBlank()) return@launch

                markMessageAsSeen(messageId)
                pushSocketDebugLog("👀 message_seen: $messageId by ${payload.optString("seen_by", "peer")}")
            }
        }

        appSocketManager.registerListener(socketOwner, "message_delivered") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args, "message_delivered") ?: return@launch
                val targetChat = payload.optString("chat_id", "")
                val messageId = payload.optString("message_id", "")
                if (targetChat != chatId || messageId.isBlank()) return@launch

                markMessageAsDelivered(messageId)
                pushSocketDebugLog("📬 message_delivered: $messageId")
            }
        }

        appSocketManager.registerListener(socketOwner, "delivered_messages_status") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args, "delivered_messages_status") ?: return@launch
                if (payload.optString("chat_id", "") != chatId) return@launch
                val messageIds = payload.optJSONArray("message_ids") ?: return@launch
                for (index in 0 until messageIds.length()) {
                    val messageId = messageIds.optString(index, "").trim()
                    if (messageId.isNotBlank()) {
                        markMessageAsDelivered(messageId)
                    }
                }
            }
        }

        appSocketManager.registerListener(socketOwner, "seen_messages_status") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args, "seen_messages_status") ?: return@launch
                if (payload.optString("chat_id", "") != chatId) return@launch
                val messageIds = payload.optJSONArray("message_ids") ?: return@launch
                for (index in 0 until messageIds.length()) {
                    val messageId = messageIds.optString(index, "").trim()
                    if (messageId.isNotBlank()) {
                        markMessageAsSeen(messageId)
                    }
                }
            }
        }

        appSocketManager.registerListener(socketOwner, "message_error") { args ->
            viewModelScope.launch {
                val raw = extractPayloadArg(args, "message_error")
                val payload = parseSocketPayload(args, "message_error")
                val detail = payload?.toDebugString() ?: raw?.toDebugString() ?: "unknown"
                pushSocketDebugLog("❌ message_error: $detail")
                Log.e("ChatViewModel", "message_error from server: $detail")

                val errorText = payload?.optString("error", "").orEmpty()
                val errorCode = payload?.optString("code", "").orEmpty()
                val clientMessageId = payload?.optString("client_message_id", "").orEmpty().ifBlank { null }
                val isBlocked = errorCode.equals("blocked", ignoreCase = true) ||
                    errorText.contains("blocked", ignoreCase = true)

                if (isBlocked) {
                    triggerBlockedDialog(
                        message = errorText.ifBlank {
                            "You have been blocked and can no longer chat with this user."
                        }
                    )
                } else if (errorText.isNotBlank()) {
                    if (clientMessageId != null) {
                        markPendingMessageFailed(clientMessageId)
                    }
                    _socketError.value = errorText
                }
            }
        }

        appSocketManager.registerListener(socketOwner, "chat_blocked") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args, "chat_blocked") ?: return@launch
                val blockedBy = payload.optString("blocked_by", "")
                val targetChat = payload.optString("chat_id", "")
                if (blockedBy == chatId || targetChat == chatId) {
                    triggerBlockedDialog(
                        payload.optString(
                            "message",
                            "You have been blocked by $chatId."
                        )
                    )
                }
            }
        }

        appSocketManager.registerListener(socketOwner, "message_deleted") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args, "message_deleted") ?: return@launch
                val messageId = payload.optString("message_id", "").ifBlank { return@launch }
                chatRepository.deleteMessageByMessageId(messageId)
                clearMessageStatus(messageId)
                loadMessages()
                pushSocketDebugLog("🗑️ message_deleted: $messageId")
            }
        }

        appSocketManager.registerListener(socketOwner, "message_deleted_for_me") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args, "message_deleted_for_me") ?: return@launch
                if (payload.optString("chat_id", "") != chatId) return@launch
                val messageId = payload.optString("message_id", "").ifBlank { return@launch }
                chatRepository.deleteMessageByMessageId(messageId)
                clearMessageStatus(messageId)
                loadMessages()
                pushSocketDebugLog("🗑️ message_deleted_for_me: $messageId")
            }
        }

        appSocketManager.registerListener(socketOwner, "deleted_messages_status") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args, "deleted_messages_status") ?: return@launch
                if (payload.optString("chat_id", "") != chatId) return@launch
                val messageIds = payload.optJSONArray("message_ids") ?: return@launch
                for (index in 0 until messageIds.length()) {
                    val messageId = messageIds.optString(index, "").trim()
                    if (messageId.isNotBlank()) {
                        chatRepository.deleteMessageByMessageId(messageId)
                        clearMessageStatus(messageId)
                    }
                }
                loadMessages()
            }
        }

        appSocketManager.registerListener(socketOwner, "user_status") { args ->
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

        appSocketManager.registerListener(socketOwner, "user_public_key_updated") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args, "user_public_key_updated") ?: return@launch
                val updatedUsername = payload.optString("username", "")
                if (updatedUsername != chatId) return@launch

                pushSocketDebugLog("Peer key updated. Refreshing public key...")
                getReceiverPublicKey()
            }
        }
    }

    private suspend fun handleIncomingMessage(payload: JSONObject) {
        val sender = payload.optString("from")
        val messageId = payload.optString("message_id", "").ifBlank { null }

        val encryptedMessage = payload.optString("message")
        val encryptedKey = payload.optString("encrypted_key")
        val timestamp = payload.optString("timestamp", "")
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
                messageType = type,
                replyToMessageId = replyToMessageId,
                replyToSender = replyToSender,
                replyPreview = decryptedReplyPreview
            )
        }
    }

    private fun requestPeerStatus() {
        appSocketManager.emit(
            "get_user_status",
            JSONObject().apply {
                put("username", chatId)
            }
        )
        pushSocketDebugLog("Requested status for $chatId")
    }

    private fun generateClientMessageId(): String = UUID.randomUUID().toString()

    private fun encryptForCurrentUser(plainText: String): SenderEncryptedPayload? {
        if (plainText.isBlank()) return null
        val selfPublicKey = spref.getPublicKey().orEmpty()
        if (selfPublicKey.isBlank()) return null

        return runCatching {
            val (encryptedKey, encryptedMessage) = CryptoUtil.encryptMessage(
                message = plainText,
                receiverPublicKey = decodePublicKey(selfPublicKey)
            )
            SenderEncryptedPayload(
                encryptedMessage = encryptedMessage,
                encryptedKey = encryptedKey,
            )
        }.getOrNull()
    }

    private fun trackPendingSentMessage(clientMessageId: String, pendingMessage: MessageEntity) {
        if (!pendingSentMessagesByClientId.containsKey(clientMessageId)) {
            pendingSentMessageOrder.addLast(clientMessageId)
        }
        pendingSentMessagesByClientId[clientMessageId] = pendingMessage
    }

    private fun addPendingVisibleMessage(clientMessageId: String, pendingMessage: MessageEntity) {
        pendingVisibleMessagesByClientId[clientMessageId] = pendingMessage
        pendingMessage.messageId?.let { clearMessageStatus(it) }
        refreshVisibleMessages()
    }

    private fun removePendingVisibleMessage(clientMessageId: String) {
        val removed = pendingVisibleMessagesByClientId.remove(clientMessageId)
        removed?.messageId?.let { clearMessageStatus(it) }
        if (removed != null) {
            refreshVisibleMessages()
        }
    }

    private fun markPendingMessageFailed(clientMessageId: String) {
        pendingSentMessagesByClientId.remove(clientMessageId)
        pendingSentMessageOrder.remove(clientMessageId)

        val pendingVisible = pendingVisibleMessagesByClientId[clientMessageId] ?: return
        val pendingMessageId = pendingVisible.messageId ?: return
        val updated = _messageDeliveryStatuses.value.toMutableMap()
        updated[pendingMessageId] = MessageDeliveryStatus.FAILED
        _messageDeliveryStatuses.value = updated
        refreshVisibleMessages()
    }

    private fun refreshVisibleMessages() {
        _messages.value = mergeDbAndPendingMessages(latestDbMessages)
    }

    private fun mergeDbAndPendingMessages(dbMessages: List<MessageEntity>): List<MessageEntity> {
        if (pendingVisibleMessagesByClientId.isEmpty()) {
            return dbMessages
        }
        return (dbMessages + pendingVisibleMessagesByClientId.values)
            .sortedWith(
                compareByDescending<MessageEntity> { parseMessageTimestamp(it.timestamp) }
                    .thenByDescending { it.messageId.orEmpty() }
                    .thenByDescending { it.id }
            )
    }

    private fun parseMessageTimestamp(value: String): Instant {
        if (value.isBlank()) return Instant.EPOCH
        return try {
            Instant.parse(value)
        } catch (_: DateTimeParseException) {
            Instant.EPOCH
        }
    }

    private fun nextPendingMessageTimestamp(): Instant {
        val latestKnownInstant = (
            latestDbMessages.asSequence().map { parseMessageTimestamp(it.timestamp) } +
                pendingVisibleMessagesByClientId.values.asSequence().map { parseMessageTimestamp(it.timestamp) }
            )
            .maxOrNull()
            ?: Instant.EPOCH

        val now = Instant.now()
        var candidate = if (latestKnownInstant.isAfter(now)) {
            latestKnownInstant.plusMillis(1)
        } else {
            now
        }
        if (!candidate.isAfter(lastPendingMessageTimestamp)) {
            candidate = lastPendingMessageTimestamp.plusMillis(1)
        }
        lastPendingMessageTimestamp = candidate
        return candidate
    }

    private fun resolvePendingMessagesFromHistory(historyMessages: List<PrivateHistoryMessage>) {
        if (pendingVisibleMessagesByClientId.isEmpty()) return

        val correlations = HistoryPendingCorrelationResolver.resolve(
            historyMessages = historyMessages,
            currentUsername = currentUsername,
        )
        if (correlations.isEmpty()) return

        var hasPendingChanges = false
        correlations.forEach { correlation ->
            val pendingVisible = pendingVisibleMessagesByClientId[correlation.clientMessageId] ?: return@forEach
            if (pendingVisible.chatId != chatId) return@forEach

            pendingSentMessagesByClientId.remove(correlation.clientMessageId)
            pendingSentMessageOrder.remove(correlation.clientMessageId)
            pendingVisibleMessagesByClientId.remove(correlation.clientMessageId)?.messageId?.let {
                clearMessageStatus(it)
            }
            markMessageAsSent(correlation.messageId)
            hasPendingChanges = true
        }

        if (hasPendingChanges) {
            refreshVisibleMessages()
        }
    }

    private fun timestampsAreClose(first: Instant, second: Instant): Boolean {
        if (first == Instant.EPOCH || second == Instant.EPOCH) return false
        return abs(Duration.between(first, second).seconds) <= 180
    }

    private fun resolvePendingVisibleMessagesFromDb(dbMessages: List<MessageEntity>) {
        if (pendingVisibleMessagesByClientId.isEmpty()) return

        val pendingCandidates = pendingVisibleMessagesByClientId.toMap()
        val unmatchedOutgoingMessages = dbMessages
            .filter { message ->
                !message.messageId.isNullOrBlank() &&
                    message.senderOrReceiver == currentUsername &&
                    message.chatId == chatId
            }
            .toMutableList()

        val resolvedClientIds = mutableListOf<String>()

        pendingCandidates.forEach { (clientMessageId, pendingMessage) ->
            val pendingTimestamp = parseMessageTimestamp(pendingMessage.timestamp)
            val matchIndex = unmatchedOutgoingMessages.indexOfFirst { dbMessage ->
                dbMessage.messageType == pendingMessage.messageType &&
                    dbMessage.content == pendingMessage.content &&
                    dbMessage.attachmentType == pendingMessage.attachmentType &&
                    dbMessage.attachmentUrl == pendingMessage.attachmentUrl &&
                    timestampsAreClose(
                        parseMessageTimestamp(dbMessage.timestamp),
                        pendingTimestamp
                    )
            }
            if (matchIndex < 0) return@forEach

            val matchedMessage = unmatchedOutgoingMessages.removeAt(matchIndex)
            resolvedClientIds.add(clientMessageId)
            pendingSentMessagesByClientId.remove(clientMessageId)
            pendingSentMessageOrder.remove(clientMessageId)
            matchedMessage.messageId?.let { markMessageAsSent(it) }
        }

        if (resolvedClientIds.isEmpty()) return

        resolvedClientIds.forEach { clientMessageId ->
            pendingVisibleMessagesByClientId[clientMessageId]?.messageId?.let { clearMessageStatus(it) }
            pendingVisibleMessagesByClientId.remove(clientMessageId)
        }
    }

    private fun schedulePendingHistoryReconcile(clientMessageId: String) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            if (!pendingVisibleMessagesByClientId.containsKey(clientMessageId)) {
                return@launch
            }
            if (!appSocketManager.isConnected()) {
                return@launch
            }
            historyMutex.withLock {
                if (!pendingVisibleMessagesByClientId.containsKey(clientMessageId)) {
                    return@withLock
                }
                pushSocketDebugLog("Reconciling pending message via history sync")
                fetchPrivateHistoryPage(before = null)
            }
        }
    }

    private fun consumePendingSentMessage(clientMessageId: String?): PendingSentResolution? {
        val resolved = ClientMessageCorrelationResolver.consumePending(
            pendingByClientId = pendingSentMessagesByClientId,
            pendingOrder = pendingSentMessageOrder,
            clientMessageId = clientMessageId,
        ) ?: return null
        return PendingSentResolution(
            clientMessageId = resolved.clientMessageId,
            message = resolved.item,
        )
    }

    private fun disconnectSocket() {
        appSocketManager.unregisterOwner(socketOwner)
    }

    private suspend fun decryptAndSaveMessage(
        messageId: String?,
        sender: String,
        encryptedMessage: String,
        encryptedKey: String,
        timestamp: String,
        targetChatId: String = chatId,
        messageType: String = "text",
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
                    messageType = messageType,
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

    private fun parseSocketPayload(args: Array<out Any>, eventName: String): JSONObject? {
        val raw = extractPayloadArg(args, eventName) ?: return null
        return when (raw) {
            is JSONObject -> raw
            is String -> runCatching { JSONObject(raw) }.getOrNull()
            is Map<*, *> -> mapToJsonObject(raw)
            else -> null
        }
    }

    private fun mapToJsonObject(raw: Map<*, *>): JSONObject {
        val json = JSONObject()
        raw.forEach { (key, value) ->
            val safeKey = key as? String ?: return@forEach
            json.put(safeKey, convertToJsonCompatibleValue(value))
        }
        return json
    }

    private fun convertToJsonCompatibleValue(value: Any?): Any? {
        return when (value) {
            null -> JSONObject.NULL
            is Map<*, *> -> mapToJsonObject(value)
            is Iterable<*> -> JSONArray(value.map { convertToJsonCompatibleValue(it) })
            is Array<*> -> JSONArray(value.map { convertToJsonCompatibleValue(it) })
            else -> value
        }
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
        pendingVisibleMessagesByClientId.clear()
        voiceRecorder?.release()
        disconnectSocket()
        super.onCleared()
    }
}
