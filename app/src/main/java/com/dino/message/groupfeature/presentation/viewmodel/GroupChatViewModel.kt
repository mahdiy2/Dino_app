package com.dino.message.groupfeature.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.chatfeature.domain.model.MessageDeliveryStatus
import com.dino.message.chatfeature.domain.util.encodeMessageReactions
import com.dino.message.chatfeature.domain.util.parseMessageReactionsFromPayload
import com.dino.message.corefeature.data.socket.AppSocketManager
import com.dino.message.corefeature.data.socket.RealtimeSocketEvents
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.data.upload.AndroidUploadContentProvider
import com.dino.message.corefeature.domain.config.AppConfig
import com.dino.message.corefeature.domain.notification.AppNotificationController
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.domain.util.LocalHistoryPaging
import com.dino.message.corefeature.presentation.util.CryptoUtil
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.util.decodePrivateKey
import com.dino.message.corefeature.presentation.util.decodePublicKey
import com.dino.message.corefeature.presentation.util.decodePublicKeyOrNull
import com.dino.message.corefeature.presentation.util.decryptMessage
import com.dino.message.corefeature.presentation.util.formatPublicKeyForLog
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.chatfeature.data.notification.ActiveGroupTracker
import com.dino.message.chatfeature.data.notification.ChatMessageSoundPlayer
import com.dino.message.chatfeature.presentation.component.PlatformAudioRecorder
import com.dino.message.chatfeature.presentation.component.VoiceRecorderManager
import com.dino.message.chatfeature.presentation.component.toAndroidUri
import com.dino.message.groupfeature.domain.model.GroupMessageEntity
import com.dino.message.groupfeature.domain.model.GroupHistoryMessage
import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.model.GroupOnlineUser
import com.dino.message.groupfeature.domain.repository.GroupRepository
import com.dino.message.groupfeature.domain.usecase.GetGroupDetailUseCase
import com.dino.message.groupfeature.domain.usecase.UploadGroupAttachmentUseCase
import com.dino.message.groupfeature.domain.usecase.SaveGroupMessageUseCase
import com.dino.message.groupfeature.domain.usecase.GetGroupMessagesFromDbUseCase
import com.dino.message.threadfeature.domain.usecase.IsTrackInPlaylistUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.time.Duration
import java.time.Instant
import java.security.PublicKey
import java.util.Collections
import java.util.UUID
import com.dino.message.corefeature.presentation.navigation.AppRoute

class GroupChatViewModel constructor(
    app: Context,
    private val spref: SPrefManager,
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val uploadGroupAttachmentUseCase: UploadGroupAttachmentUseCase,
    private val saveGroupMessageUseCase: SaveGroupMessageUseCase,
    private val getGroupMessagesFromDbUseCase: GetGroupMessagesFromDbUseCase,
    private val isTrackInPlaylistUseCase: IsTrackInPlaylistUseCase,
    private val groupRepository: GroupRepository,
    private val appSocketManager: AppSocketManager,
    private val appNotificationController: AppNotificationController,
    private val stringResolver: StringResolver,
    private val appConfig: AppConfig,
    private val uploadContentProvider: AndroidUploadContentProvider,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    companion object {
        private const val TAG = "GroupChatVM"
        private const val HISTORY_PAGE_SIZE = 50
        private const val LOCAL_DB_PAGE_SIZE = 100
        private val HISTORY_STALE_AFTER: Duration = Duration.ofMinutes(2)
        private const val GROUP_DECRYPTION_FALLBACK = "Could not decrypt this message"
    }

    private data class GroupKeySession(
        val keyRef: String,
        val memberFingerprint: String,
        val rawAesKey: ByteArray,
        val encryptedKeys: Map<String, String>,
        var includeKeyRecordsOnNextSend: Boolean = true,
    )

    private data class MemberKeySnapshot(
        val decodedKeys: Map<String, PublicKey>,
        val profileImages: Map<String, String?>,
        val profileImageShapes: Map<String, String?>,
        val memberBadges: Map<String, String?>,
        val memberIds: Map<String, Int>,
    )

    val groupId: Int = checkNotNull(savedStateHandle["groupId"])
    val groupName: String = savedStateHandle["groupName"] ?: "Group"
    val isPublicRoom: Boolean = savedStateHandle["isPublicRoom"] ?: false

    private val currentUsername: String = spref.getUsername() ?: ""

    private val _messages = MutableStateFlow<List<GroupMessageEntity>>(emptyList())
    val messages: StateFlow<List<GroupMessageEntity>> = _messages.asStateFlow()

    private val _messageDeliveryStatuses = MutableStateFlow<Map<String, MessageDeliveryStatus>>(emptyMap())
    val messageDeliveryStatuses: StateFlow<Map<String, MessageDeliveryStatus>> = _messageDeliveryStatuses.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingOlderHistory = MutableStateFlow(false)
    val isLoadingOlderHistory: StateFlow<Boolean> = _isLoadingOlderHistory.asStateFlow()

    private val _canLoadOlderHistory = MutableStateFlow(false)
    val canLoadOlderHistory: StateFlow<Boolean> = _canLoadOlderHistory.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _socketError = MutableStateFlow<String?>(null)
    val socketError: StateFlow<String?> = _socketError.asStateFlow()

    private val _isLeavingGroup = MutableStateFlow(false)
    val isLeavingGroup: StateFlow<Boolean> = _isLeavingGroup.asStateFlow()

    private val _exitChatReason = MutableStateFlow<String?>(null)
    val exitChatReason: StateFlow<String?> = _exitChatReason.asStateFlow()

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
    private val _memberProfileImageShapes = MutableStateFlow<Map<String, String?>>(emptyMap())
    val memberProfileImageShapes: StateFlow<Map<String, String?>> = _memberProfileImageShapes.asStateFlow()
    private val _memberBadges = MutableStateFlow<Map<String, String?>>(emptyMap())
    val memberBadges: StateFlow<Map<String, String?>> = _memberBadges.asStateFlow()

    private val _onlineGroupUsers = MutableStateFlow<List<GroupOnlineUser>>(emptyList())
    val onlineGroupUsers: StateFlow<List<GroupOnlineUser>> = _onlineGroupUsers.asStateFlow()

    private val _typingGroupUsers = MutableStateFlow<List<GroupOnlineUser>>(emptyList())
    val typingGroupUsers: StateFlow<List<GroupOnlineUser>> = _typingGroupUsers.asStateFlow()

    private var voiceRecorder: PlatformAudioRecorder? = null
    private var recordingTimerJob: kotlinx.coroutines.Job? = null
    private var composerTypingStopJob: kotlinx.coroutines.Job? = null
    private var typingHeartbeatJob: kotlinx.coroutines.Job? = null
    private var mainThreadWatchdogJob: kotlinx.coroutines.Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastMainThreadHeartbeatMs: Long = 0L
    private var mainThreadStallReported = false
    private val mainThreadHeartbeatRunnable = object : Runnable {
        override fun run() {
            lastMainThreadHeartbeatMs = SystemClock.uptimeMillis()
            mainHandler.postDelayed(this, 500L)
        }
    }

    private val socketOwner = "GroupChatViewModel-$groupId"
    private val seenMessageIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val messagesMutex = Mutex()
    private val historyMutex = Mutex()
    private var groupHistoryCursorInitialized = false
    private var groupHistoryHasMore = false
    private var groupHistoryNextBefore: String? = null
    private var loadedLocalMessageCount = 0
    private var totalLocalMessageCount = 0

    private var memberPublicKeys: Map<String, PublicKey> = emptyMap()
    private var memberIdsByUsername: Map<String, Int> = emptyMap()
    private var memberKeysReady = false
    private var groupKeySession: GroupKeySession? = null
    private var hasLoggedMissingEncryptedGroupKey = false
    private var hasLoggedMissingPrivateKey = false
    private var lastGroupDecryptFailureSignature: String? = null
    private var isTypingEmitted = false
    private val typingUsersByUsername = mutableMapOf<String, GroupOnlineUser>()
    private val typingExpiryJobsByUsername = mutableMapOf<String, kotlinx.coroutines.Job>()

    init {
        ActiveGroupTracker.setActiveGroup(groupId)
        appNotificationController.clearGroupChat(groupId)
        startMainThreadWatchdogIfDebug()
        loadMessagesFromDb(resetPaging = true)
        viewModelScope.launch {
            syncHistoryOnOpen()
        }
        loadGroupDetailAndConnect()
    }

    fun getMyUsername(): String = currentUsername

    private fun loadMessagesFromDb(resetPaging: Boolean = false) = viewModelScope.launch {
        try {
            messagesMutex.withLock {
                val dbMessages = loadGroupMessagesFromDb(resetPaging = resetPaging)
                onGroupMessagesLoaded(dbMessages)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load messages from DB", e)
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

            if (!groupHistoryHasMore) {
                updateCanLoadOlderHistoryFlag()
                return@withLock
            }

            val beforeCursor = groupHistoryNextBefore
                ?: oldestTimestampFromMessages(_messages.value)
                ?: run {
                    groupHistoryHasMore = false
                    updateCanLoadOlderHistoryFlag()
                    return@withLock
                }

            _isLoadingOlderHistory.value = true
            fetchGroupHistoryPage(before = beforeCursor)
            _isLoadingOlderHistory.value = false
        }
    }

    private suspend fun syncHistoryOnOpen() {
        historyMutex.withLock {
            val localMessages = groupRepository.getGroupMessagesFromDb(groupId = groupId, limit = 1, offset = 0)
            if (!shouldRefreshLatestHistory(localMessages)) {
                return
            }
            fetchGroupHistoryPage(before = null)
        }
    }

    private suspend fun fetchGroupHistoryPage(before: String?) {
        when (
            val result = groupRepository.getGroupHistory(
                groupId = groupId,
                limit = HISTORY_PAGE_SIZE,
                before = before,
            )
        ) {
            is Resource.Success -> {
                val page = result.data
                val mappedMessages = withContext(Dispatchers.Default) {
                    page.messages.mapNotNull { historyMessage ->
                        mapHistoryMessageToEntity(historyMessage)
                    }
                }
                seenMessageIds.addAll(mappedMessages.map { it.messageId })
                if (mappedMessages.isNotEmpty()) {
                    groupRepository.upsertGroupMessagesByMessageId(mappedMessages)
                }
                updateGroupHistoryCursor(page.hasMore, page.nextBefore, mappedMessages)
                loadMessagesFromDb()
            }
            is Resource.Error -> {
                val errorText = result.message.resolveWith(stringResolver)
                Log.w(TAG, "History fetch failed for group $groupId: $errorText")
            }
            is Resource.Loading -> Unit
        }
    }

    private fun seedGroupHistoryCursorFromLocal(localMessages: List<GroupMessageEntity>) {
        val oldestTimestamp = oldestTimestampFromMessages(localMessages)
        if (!groupHistoryCursorInitialized) {
            groupHistoryHasMore = !oldestTimestamp.isNullOrBlank()
            groupHistoryCursorInitialized = true
        }
        if (!oldestTimestamp.isNullOrBlank()) {
            groupHistoryNextBefore = oldestTimestamp
        }
        if (groupHistoryHasMore && groupHistoryNextBefore.isNullOrBlank()) {
            groupHistoryHasMore = false
        }
        updateCanLoadOlderHistoryFlag()
    }

    private fun shouldRefreshLatestHistory(localMessages: List<GroupMessageEntity>): Boolean {
        val newestTimestamp = newestTimestampFromMessages(localMessages) ?: return true
        val newestInstant = runCatching { Instant.parse(newestTimestamp) }.getOrNull() ?: return true
        val age = Duration.between(newestInstant, Instant.now())
        return age > HISTORY_STALE_AFTER
    }

    private fun updateGroupHistoryCursor(
        hasMore: Boolean,
        nextBefore: String?,
        mergedMessages: List<GroupMessageEntity>,
    ) {
        groupHistoryCursorInitialized = true
        groupHistoryHasMore = hasMore
        groupHistoryNextBefore = nextBefore?.takeIf { it.isNotBlank() }
            ?: oldestTimestampFromMessages(mergedMessages)
            ?: groupHistoryNextBefore
        if (groupHistoryHasMore && groupHistoryNextBefore.isNullOrBlank()) {
            groupHistoryHasMore = false
        }
        updateCanLoadOlderHistoryFlag()
    }

    private suspend fun loadGroupMessagesFromDb(resetPaging: Boolean): List<GroupMessageEntity> {
        val previouslyLoadedCount = loadedLocalMessageCount
        val previousTotalCount = totalLocalMessageCount
        totalLocalMessageCount = groupRepository.getGroupMessageCount(groupId)
        val queryLimit = LocalHistoryPaging.resolveQueryLimit(
            resetPaging = resetPaging,
            previouslyLoadedCount = previouslyLoadedCount,
            previousTotalCount = previousTotalCount,
            currentTotalCount = totalLocalMessageCount,
            pageSize = LOCAL_DB_PAGE_SIZE,
        )
        var dbMessages: List<GroupMessageEntity> = emptyList()
        if (queryLimit > 0) {
            getGroupMessagesFromDbUseCase.execute(groupId = groupId, limit = queryLimit, offset = 0).collect { loaded ->
                dbMessages = loaded
            }
        }
        loadedLocalMessageCount = queryLimit
        _messages.value = dbMessages
        seedGroupHistoryCursorFromLocal(dbMessages)
        return dbMessages
    }

    private suspend fun loadOlderMessagesFromLocalIfAvailable(): Boolean {
        if (loadedLocalMessageCount >= totalLocalMessageCount) {
            return false
        }
        loadedLocalMessageCount = (loadedLocalMessageCount + LOCAL_DB_PAGE_SIZE).coerceAtMost(totalLocalMessageCount)
        val dbMessages = loadGroupMessagesFromDb(resetPaging = false)
        onGroupMessagesLoaded(dbMessages)
        return true
    }

    private fun onGroupMessagesLoaded(dbMessages: List<GroupMessageEntity>) {
        seenMessageIds.addAll(dbMessages.map { it.messageId })
        syncOutgoingStatuses(dbMessages)
        requestDeliveredStatuses(dbMessages)
        requestSeenStatuses(dbMessages)
        requestDeletedStatuses(dbMessages)
    }

    private fun updateCanLoadOlderHistoryFlag() {
        val hasMoreLocal = loadedLocalMessageCount < totalLocalMessageCount
        _canLoadOlderHistory.value = hasMoreLocal || groupHistoryHasMore
    }

    private fun newestTimestampFromMessages(messages: List<GroupMessageEntity>): String? {
        return messages.firstOrNull { it.messageId.isNotBlank() && it.timestamp.isNotBlank() }?.timestamp
    }

    private fun oldestTimestampFromMessages(messages: List<GroupMessageEntity>): String? {
        return messages.lastOrNull { it.messageId.isNotBlank() && it.timestamp.isNotBlank() }?.timestamp
    }

    private fun mapHistoryMessageToEntity(
        historyMessage: GroupHistoryMessage,
    ): GroupMessageEntity? {
        val sender = historyMessage.from.trim()
        if (sender.isBlank()) return null

        val resolvedEncryptedKeys = resolveEncryptedKeys(
            encryptedKeys = historyMessage.encryptedKeys,
            encryptedKey = historyMessage.encryptedKey,
        )
        val decryptedContent = decryptGroupMessage(
            encryptedMessage = historyMessage.message,
            encryptedKeys = resolvedEncryptedKeys,
        )
        val decryptedReplyPreview = historyMessage.encryptedReplyPreview?.let { encryptedReply ->
            decryptGroupMessage(
                encryptedMessage = encryptedReply,
                encryptedKeys = resolvedEncryptedKeys,
            )
        }

        return GroupMessageEntity(
            messageId = historyMessage.messageId,
            groupId = groupId,
            sender = sender,
            content = decryptedContent,
            timestamp = historyMessage.timestamp,
            messageType = historyMessage.type,
            attachmentType = historyMessage.attachment?.type,
            attachmentUrl = historyMessage.attachment?.url,
            attachmentMimeType = historyMessage.attachment?.mimeType,
            attachmentFileName = historyMessage.attachment?.fileName,
            attachmentSizeBytes = historyMessage.attachment?.sizeBytes,
            replyToMessageId = historyMessage.replyToMessageId,
            replyToSender = historyMessage.replyToSender,
            replyPreview = decryptedReplyPreview,
            reactionsJson = encodeMessageReactions(historyMessage.reactions),
        )
    }

    private fun syncOutgoingStatuses(dbMessages: List<GroupMessageEntity>) {
        if (dbMessages.isEmpty()) return
        val updated = _messageDeliveryStatuses.value.toMutableMap()
        dbMessages.forEach { message ->
            if (message.sender == currentUsername) {
                val existingStatus = updated[message.messageId]
                updated[message.messageId] = when {
                    message.isSeen || existingStatus == MessageDeliveryStatus.SEEN ->
                        MessageDeliveryStatus.SEEN
                    existingStatus == MessageDeliveryStatus.DELIVERED ->
                        MessageDeliveryStatus.DELIVERED
                    else ->
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

    private fun requestDeliveredStatuses(messages: List<GroupMessageEntity> = _messages.value) {
        if (!appSocketManager.isConnected()) return

        val outgoingIds = messages
            .asSequence()
            .filter { it.sender == currentUsername }
            .map { it.messageId }
            .distinct()
            .toList()
        if (outgoingIds.isEmpty()) return

        appSocketManager.emit(
            "get_group_delivered_messages",
            JSONObject().apply {
                put("group_id", groupId)
                put("message_ids", JSONArray(outgoingIds))
            }
        )
    }

    private fun requestSeenStatuses(messages: List<GroupMessageEntity> = _messages.value) {
        if (!appSocketManager.isConnected()) return

        val pendingIds = messages
            .asSequence()
            .filter { it.sender == currentUsername }
            .filter { !it.isSeen }
            .map { it.messageId }
            .distinct()
            .toList()
        if (pendingIds.isEmpty()) return

        appSocketManager.emit(
            "get_group_seen_messages",
            JSONObject().apply {
                put("group_id", groupId)
                put("message_ids", JSONArray(pendingIds))
            }
        )
    }

    private fun requestDeletedStatuses(messages: List<GroupMessageEntity> = _messages.value) {
        if (!appSocketManager.isConnected()) return

        val existingIds = messages
            .map { it.messageId }
            .distinct()
        if (existingIds.isEmpty()) return

        appSocketManager.emit(
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
                    refreshGroupOnlineUsersFromApi()
                    connectSocket()
                }
                is Resource.Error -> {
                    _isLoading.value = false
                    _socketError.value = res.message.resolveWith(stringResolver)
                }
            }
        }
    }

    private suspend fun applyMemberPublicKeys(group: Group) {
        val snapshot = withContext(Dispatchers.Default) {
            val keys = mutableMapOf<String, PublicKey>()
            val profileImages = mutableMapOf<String, String?>()
            val profileImageShapes = mutableMapOf<String, String?>()
            val memberBadges = mutableMapOf<String, String?>()
            val memberIds = mutableMapOf<String, Int>()

            group.members?.forEach { member ->
                profileImages[member.username] = member.profileImageUrl
                profileImageShapes[member.username] = member.profileImageShape
                memberBadges[member.username] = member.badge
                memberIds[member.username] = member.id

                val pk = member.publicKey
                if (appConfig.isDebug) {
                    Log.d(TAG, "Group member key ${member.username}: ${formatPublicKeyForLog(pk)}")
                }
                if (!pk.isNullOrBlank()) {
                    val decodedPublicKey = decodePublicKeyOrNull(pk)
                    if (decodedPublicKey != null) {
                        keys[member.username] = decodedPublicKey
                    } else {
                        Log.w(
                            TAG,
                            "Skipping unsupported public key format for ${member.username}, ${formatPublicKeyForLog(pk)}"
                        )
                    }
                }
            }

            MemberKeySnapshot(
                decodedKeys = keys,
                profileImages = profileImages,
                profileImageShapes = profileImageShapes,
                memberBadges = memberBadges,
                memberIds = memberIds,
            )
        }

        val previousFingerprint = memberKeyFingerprint(memberPublicKeys)
        val newFingerprint = memberKeyFingerprint(snapshot.decodedKeys)
        _memberProfileImageUrls.value = snapshot.profileImages
        _memberProfileImageShapes.value = snapshot.profileImageShapes
        _memberBadges.value = snapshot.memberBadges
        memberIdsByUsername = snapshot.memberIds
        memberPublicKeys = if (isPublicRoom) emptyMap() else snapshot.decodedKeys
        memberKeysReady = isPublicRoom || snapshot.decodedKeys.isNotEmpty()
        reconcileOnlineUsersWithKnownMembers(snapshot.profileImages, snapshot.memberIds)
        if (previousFingerprint != newFingerprint) {
            invalidateGroupKeySession()
        }
        Log.d(TAG, "Loaded ${snapshot.decodedKeys.size} member public keys for group $groupId")
    }

    private fun reconcileOnlineUsersWithKnownMembers(
        profileImagesByUsername: Map<String, String?> = _memberProfileImageUrls.value,
        memberIdsByUsernameSnapshot: Map<String, Int> = memberIdsByUsername,
    ) {
        val memberUsernames = profileImagesByUsername.keys
        if (memberUsernames.isEmpty()) {
            _onlineGroupUsers.value = emptyList()
            return
        }

        val normalized = _onlineGroupUsers.value
            .asSequence()
            .filter { memberUsernames.contains(it.username) }
            .map { onlineUser ->
                val fallbackId = memberIdsByUsernameSnapshot[onlineUser.username]?.toString().orEmpty()
                onlineUser.copy(
                    userId = onlineUser.userId.ifBlank { fallbackId },
                    profileImageUrl = onlineUser.profileImageUrl
                        ?: profileImagesByUsername[onlineUser.username],
                    profileImageShape = onlineUser.profileImageShape
                        ?: _memberProfileImageShapes.value[onlineUser.username],
                    badge = onlineUser.badge ?: _memberBadges.value[onlineUser.username],
                )
            }
            .distinctBy { it.username }
            .sortedBy { it.username }
            .toList()

        _onlineGroupUsers.value = normalized
    }

    private fun memberKeyFingerprint(keys: Map<String, PublicKey>): String {
        if (keys.isEmpty()) return ""
        return keys.entries
            .sortedBy { it.key }
            .joinToString(separator = "|") { entry ->
                "${entry.key}:${entry.value.encoded.contentHashCode()}"
            }
    }

    private fun invalidateGroupKeySession() {
        groupKeySession = null
    }

    private fun getOrCreateGroupKeySession(): GroupKeySession? {
        if (!memberKeysReady || memberPublicKeys.isEmpty()) {
            Log.e(TAG, "Cannot encrypt: member keys not ready")
            _socketError.value = "Encryption keys not loaded yet"
            return null
        }

        val fingerprint = memberKeyFingerprint(memberPublicKeys)
        val existing = groupKeySession
        if (existing != null && existing.memberFingerprint == fingerprint) {
            return existing
        }

        return try {
            val seeded = CryptoUtil.encryptGroupMessage("", memberPublicKeys)
            GroupKeySession(
                keyRef = UUID.randomUUID().toString(),
                memberFingerprint = fingerprint,
                rawAesKey = seeded.rawAesKey.copyOf(),
                encryptedKeys = seeded.encryptedKeys.toMap(),
                includeKeyRecordsOnNextSend = true,
            ).also { groupKeySession = it }
        } catch (e: Exception) {
            Log.e(TAG, "Group key session bootstrap failed", e)
            _socketError.value = "Encryption failed: ${e.message}"
            null
        }
    }

    private fun refreshMemberPublicKeysSilently() = viewModelScope.launch {
        getGroupDetailUseCase.execute(groupId).collect { res ->
            when (res) {
                is Resource.Success -> {
                    applyMemberPublicKeys(res.data)
                }
                is Resource.Error -> {
                    Log.w(TAG, "Failed to refresh member keys: ${res.message.resolveWith(stringResolver)}")
                }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun decryptGroupMessage(
        encryptedMessage: String?,
        encryptedKeys: Map<String, String>?,
    ): String {
        if (encryptedMessage.isNullOrBlank()) return ""
        if (isPublicRoom) return encryptedMessage
        if (encryptedKeys.isNullOrEmpty()) return GROUP_DECRYPTION_FALLBACK

        val myEncryptedKey = encryptedKeys[currentUsername].orEmpty()
        if (myEncryptedKey.isBlank()) {
            if (!hasLoggedMissingEncryptedGroupKey) {
                Log.w(TAG, "No encrypted key found for $currentUsername")
                hasLoggedMissingEncryptedGroupKey = true
            }
            _decryptionError.value =
                "Unable to decrypt some group messages. Your private key may not match your account key."
            return GROUP_DECRYPTION_FALLBACK
        }

        return try {
            val privateKeyStr = spref.getPrivateKey() ?: run {
                if (!hasLoggedMissingPrivateKey) {
                    Log.w(TAG, "Private key missing on device for user $currentUsername")
                    hasLoggedMissingPrivateKey = true
                }
                _decryptionError.value =
                    "Unable to decrypt some group messages. Your private key is missing on this device."
                return GROUP_DECRYPTION_FALLBACK
            }
            decryptMessage(
                encryptedMessage = encryptedMessage,
                encryptedAESKey = myEncryptedKey,
                privateKey = decodePrivateKey(privateKeyStr),
            )
        } catch (e: Exception) {
            val signature = "${e::class.java.name}:${e.message.orEmpty()}"
            if (lastGroupDecryptFailureSignature != signature) {
                Log.e(TAG, "Failed to decrypt group message", e)
                lastGroupDecryptFailureSignature = signature
            }
            _decryptionError.value =
                "Unable to decrypt some group messages. Your private key may not match your account key."
            GROUP_DECRYPTION_FALLBACK
        }
    }

    private fun resolveEncryptedKeys(
        encryptedKeys: Map<String, String>?,
        encryptedKey: String?,
    ): Map<String, String>? {
        val resolvedMap = encryptedKeys
            ?.filterValues { it.isNotBlank() }
            ?.filterKeys { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
        if (resolvedMap != null) return resolvedMap

        val directKey = encryptedKey?.takeIf { it.isNotBlank() } ?: return null
        return mapOf(currentUsername to directKey)
    }

    private fun resolveEncryptedKeys(payload: JSONObject): Map<String, String>? {
        return resolveEncryptedKeys(
            encryptedKeys = payload.optJSONObject("encrypted_keys").toEncryptedKeysMap(),
            encryptedKey = payload.optString("encrypted_key", "").ifBlank { null },
        )
    }

    private fun encryptForGroup(plaintext: String): Pair<GroupKeySession, String>? {
        val session = getOrCreateGroupKeySession() ?: return null
        return try {
            session to CryptoUtil.encryptWithAesKey(plaintext, session.rawAesKey)
        } catch (e: Exception) {
            Log.e(TAG, "Group encryption failed", e)
            _socketError.value = "Encryption failed: ${e.message}"
            null
        }
    }

    private fun buildRecipientKeyRecordsJson(keys: Map<String, String>): JSONArray {
        val records = JSONArray()
        keys.entries
            .sortedBy { it.key }
            .forEach { entry ->
                records.put(
                    JSONObject().apply {
                        put("recipient", entry.key)
                        put("encrypted_key", entry.value)
                    }
                )
            }
        return records
    }

    private fun connectSocket() {
        registerSocketListeners()
        val connectRequested = appSocketManager.connect()
        if (appSocketManager.isConnected()) {
            joinGroupAndRequestStatusSync()
        } else if (!connectRequested) {
            _socketError.value = "Unable to connect to chat server."
        }
    }

    private fun joinGroupAndRequestStatusSync() {
        if (!appSocketManager.isConnected()) return
        appSocketManager.emit("join_group", JSONObject().apply { put("group_id", groupId) })
        requestGroupOnlineUsersFromSocket()
        requestDeliveredStatuses()
        requestSeenStatuses()
        requestDeletedStatuses()
    }

    private fun registerSocketListeners() {
        appSocketManager.registerListener(socketOwner, RealtimeSocketEvents.CONNECT) {
            Log.d(TAG, "Socket connected, joining group room")
            joinGroupAndRequestStatusSync()
        }

        appSocketManager.registerListener(socketOwner, "group_joined") {
            Log.d(TAG, "Joined group room $groupId")
            requestGroupOnlineUsersFromSocket()
        }

        appSocketManager.registerListener(socketOwner, "group_read_state_synced") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                if (payload.optInt("group_id", -1) != groupId) return@launch
                if (payload.optInt("marked", 0) <= 0) return@launch
                historyMutex.withLock {
                    fetchGroupHistoryPage(before = null)
                }
            }
        }

        appSocketManager.registerListener(socketOwner, "group_online_users") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                if (payload.optInt("group_id", -1) != groupId) return@launch
                applyOnlineUsersSnapshot(payload.optJSONArray("online_users"))
            }
        }

        appSocketManager.registerListener(socketOwner, "group_presence_changed") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                if (payload.optInt("group_id", -1) != groupId) return@launch
                applyOnlinePresenceDelta(payload)
            }
        }

        appSocketManager.registerListener(socketOwner, "group_typing") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                if (payload.optInt("group_id", -1) != groupId) return@launch
                handleIncomingGroupTyping(payload)
            }
        }

        appSocketManager.registerListener(socketOwner, "pending_group_messages") { args ->
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
                            val resolvedEncryptedKeys = resolveEncryptedKeys(item)

                            val decryptedContent = decryptGroupMessage(
                                encryptedMessage = encryptedMessage,
                                encryptedKeys = resolvedEncryptedKeys,
                            )

                            val attachmentObj = item.optJSONObject("attachment")

                            val decryptedReplyPreview = run {
                                val encReply = if (item.has("encrypted_reply_preview") && !item.isNull("encrypted_reply_preview"))
                                    item.optString("encrypted_reply_preview", "").ifBlank { null }
                                else null
                                if (encReply != null) {
                                    decryptGroupMessage(
                                        encryptedMessage = encReply,
                                        encryptedKeys = resolvedEncryptedKeys,
                                    )
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
                                reactionsJson = encodeMessageReactions(parseMessageReactionsFromPayload(item)),
                            )
                            saveMessageToDb(entity)
                            if (messageId.isNotBlank()) processedIds.add(messageId)
                        }
                        processedIds
                    }
                    loadMessagesFromDb()
                    if (ackedIds.isNotEmpty()) {
                        sendGroupAck(ackedIds)
                        sendGroupRead(ackedIds)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling pending_group_messages", e)
                }
            }
        }

        appSocketManager.registerListener(socketOwner, "new_group_message") { args ->
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

                        val resolvedEncryptedKeys = resolveEncryptedKeys(payload)

                        val decryptedContent = decryptGroupMessage(
                            encryptedMessage = encryptedMessage,
                            encryptedKeys = resolvedEncryptedKeys,
                        )

                        val attachmentObj = payload.optJSONObject("attachment")

                        val decryptedReplyPreview = run {
                            val encReply = if (payload.has("encrypted_reply_preview") && !payload.isNull("encrypted_reply_preview"))
                                payload.optString("encrypted_reply_preview", "").ifBlank { null }
                            else null
                            if (encReply != null) {
                                decryptGroupMessage(
                                    encryptedMessage = encReply,
                                    encryptedKeys = resolvedEncryptedKeys,
                                )
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
                            reactionsJson = encodeMessageReactions(parseMessageReactionsFromPayload(payload)),
                        )
                        saveMessageToDb(entity)
                    }
                    loadMessagesFromDb()
                    if (sender != currentUsername && ActiveGroupTracker.isViewingGroup(groupId)) {
                        ChatMessageSoundPlayer.playReceived(getApplication())
                    }
                    if (sender != currentUsername && messageId.isNotBlank()) {
                        sendGroupAck(listOf(messageId))
                        sendGroupRead(listOf(messageId))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling new_group_message", e)
                }
            }
        }

        appSocketManager.registerListener(socketOwner, "group_message_sent") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                val payloadGroupId = payload.optInt("group_id", -1)
                if (payloadGroupId != groupId) return@launch

                val messageId = payload.optString("message_id", "")
                if (messageId.isBlank()) return@launch

                markMessageAsSent(messageId)
                if (ActiveGroupTracker.isViewingGroup(groupId)) {
                    ChatMessageSoundPlayer.playSent(getApplication())
                }
                Log.d(
                    TAG,
                    "group_message_sent ack received: messageId=$messageId clientMessageId=${payload.optString("client_message_id", "")}"
                )

                if (_messages.value.none { it.messageId == messageId }) {
                    historyMutex.withLock {
                        fetchGroupHistoryPage(before = null)
                    }
                }
            }
        }

        appSocketManager.registerListener(socketOwner, "group_message_delivered") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                val payloadGroupId = payload.optInt("group_id", -1)
                if (payloadGroupId != groupId) return@launch

                val deliveredTo = payload.optString("delivered_to", "")
                if (deliveredTo == currentUsername) return@launch

                val messageId = payload.optString("message_id", "")
                if (messageId.isBlank()) return@launch
                markMessageAsDelivered(messageId)
            }
        }

        appSocketManager.registerListener(socketOwner, "group_message_seen") { args ->
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

        appSocketManager.registerListener(socketOwner, "group_delivered_messages_status") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                if (payload.optInt("group_id", -1) != groupId) return@launch
                val messageIds = payload.optJSONArray("message_ids") ?: return@launch
                for (index in 0 until messageIds.length()) {
                    val messageId = messageIds.optString(index, "").trim()
                    if (messageId.isNotBlank()) {
                        markMessageAsDelivered(messageId)
                    }
                }
            }
        }

        appSocketManager.registerListener(socketOwner, "group_seen_messages_status") { args ->
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

        appSocketManager.registerListener(socketOwner, "group_message_deleted") { args ->
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

        appSocketManager.registerListener(socketOwner, "group_message_deleted_for_me") { args ->
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

        appSocketManager.registerListener(socketOwner, "group_deleted_messages_status") { args ->
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

        appSocketManager.registerListener(socketOwner, "message_reaction_updated") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                if (payload.optString("scope", "") != "group") return@launch
                if (payload.optInt("group_id", -1) != groupId) return@launch
                val messageId = payload.optString("message_id", "").trim()
                if (messageId.isBlank()) return@launch
                val reactionsJson = encodeMessageReactions(parseMessageReactionsFromPayload(payload))
                groupRepository.updateGroupMessageReactionsByMessageId(messageId, reactionsJson)
                loadMessagesFromDb()
            }
        }

        appSocketManager.registerListener(socketOwner, "message_error") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args)
                val errorMsg = payload?.optString("error", "Unknown error") ?: "Unknown error"
                val errorCode = payload?.optString("code", "").orEmpty()
                val uiError = mapGroupMessageErrorToUiMessage(errorCode, errorMsg)

                if (
                    errorCode == "not_group_member" ||
                    errorMsg.contains("not a member of this group", ignoreCase = true)
                ) {
                    revokeMembershipAndExit("You are no longer a member of this group.")
                } else {
                    _socketError.value = uiError
                }
                Log.e(
                    TAG,
                    "message_error: code=$errorCode client_message_id=${payload?.optString("client_message_id", "")} error=$errorMsg"
                )
            }
        }

        appSocketManager.registerListener(socketOwner, "group_membership_revoked") { args ->
            viewModelScope.launch {
                val payload = parseSocketPayload(args) ?: return@launch
                if (payload.optInt("group_id", -1) != groupId) return@launch

                val reason = payload.optString("reason", "")
                val message = when (reason) {
                    "group_deleted" -> "This group was deleted."
                    "left" -> "You are no longer in this group."
                    else -> "You were removed from this group."
                }
                revokeMembershipAndExit(message)
            }
        }

        appSocketManager.registerListener(socketOwner, "user_public_key_updated") { args ->
            val payload = parseSocketPayload(args) ?: return@registerListener
            val updatedUsername = payload.optString("username", "")
            if (updatedUsername.isBlank()) return@registerListener

            val shouldRefresh = memberPublicKeys.containsKey(updatedUsername) ||
                payloadGroupIdsContainsCurrentGroup(payload)
            if (!shouldRefresh) return@registerListener

            Log.d(TAG, "Detected updated public key for $updatedUsername, refreshing group keys")
            refreshMemberPublicKeysSilently()
        }

        appSocketManager.registerListener(socketOwner, "group_member_key_updated") { args ->
            val payload = parseSocketPayload(args) ?: return@registerListener
            val payloadGroupId = payload.optInt("group_id", -1)
            if (payloadGroupId != groupId) return@registerListener

            Log.d(TAG, "Received group_member_key_updated for group $groupId")
            refreshMemberPublicKeysSilently()
        }

        appSocketManager.registerListener(socketOwner, RealtimeSocketEvents.DISCONNECT) {
            Log.d(TAG, "Socket disconnected")
            clearTypingUsers()
            typingHeartbeatJob?.cancel()
            typingHeartbeatJob = null
            isTypingEmitted = false
        }
    }

    fun sendTextMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val clientMessageId = UUID.randomUUID().toString()
        val replyTo = _replyingTo.value

        val json = if (isPublicRoom) {
            JSONObject().apply {
                put("group_id", groupId)
                put("message", trimmed)
                put("type", "text")
                put("client_message_id", clientMessageId)
                if (replyTo != null) {
                    put("reply_to_message_id", replyTo.messageId)
                    put("reply_to_sender", replyTo.sender)
                    val preview = replyTo.content.take(100)
                    if (preview.isNotBlank()) {
                        put("encrypted_reply_preview", preview)
                    }
                }
            }
        } else {
            val (keySession, encryptedMessage) = encryptForGroup(trimmed) ?: return
            JSONObject().apply {
                put("group_id", groupId)
                put("message", encryptedMessage)
                put("group_key_ref", keySession.keyRef)
                put("type", "text")
                put("client_message_id", clientMessageId)
                if (keySession.includeKeyRecordsOnNextSend) {
                    put(
                        "recipient_key_records",
                        buildRecipientKeyRecordsJson(keySession.encryptedKeys),
                    )
                }
                if (replyTo != null) {
                    put("reply_to_message_id", replyTo.messageId)
                    put("reply_to_sender", replyTo.sender)
                    val preview = replyTo.content.take(100)
                    if (preview.isNotBlank()) {
                        put(
                            "encrypted_reply_preview",
                            CryptoUtil.encryptWithAesKey(preview, keySession.rawAesKey),
                        )
                    }
                }
            }.also {
                keySession.includeKeyRecordsOnNextSend = false
            }
        }
        emitGroupMessageWithRetry(
            payload = json,
            clientMessageId = clientMessageId,
        )
        stopTypingNow()
        clearReply()
    }

    fun onComposerTextChanged(text: String) {
        val hasText = text.isNotBlank()
        if (!hasText) {
            stopTypingNow()
            return
        }

        if (!isTypingEmitted) {
            emitGroupTyping(isTyping = true)
            isTypingEmitted = true
        }
        startTypingHeartbeatIfNeeded()

        composerTypingStopJob?.cancel()
        composerTypingStopJob = viewModelScope.launch {
            kotlinx.coroutines.delay(2600)
            stopTypingNow()
        }
    }

    fun sendAttachment(fileUri: Uri, mediaType: String) {
        viewModelScope.launch {
            stopTypingNow()
            val uploadFile = uploadContentProvider.readableMediaFile(fileUri)
            uploadGroupAttachmentUseCase.execute(groupId, uploadFile).collect { res ->
                when (res) {
                    is Resource.Loading -> _isUploading.value = true
                    is Resource.Success -> {
                        _isUploading.value = false
                        val att = res.data

                        val clientMessageId = UUID.randomUUID().toString()
                        val replyTo = _replyingTo.value

                        val json = if (isPublicRoom) {
                            JSONObject().apply {
                                put("group_id", groupId)
                                put("message", "")
                                put("type", mediaType)
                                put("client_message_id", clientMessageId)
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
                                        put("encrypted_reply_preview", preview)
                                    }
                                }
                            }
                        } else {
                            val (keySession, encryptedMessage) = encryptForGroup("") ?: return@collect
                            JSONObject().apply {
                                put("group_id", groupId)
                                put("message", encryptedMessage)
                                put("group_key_ref", keySession.keyRef)
                                put("type", mediaType)
                                put("client_message_id", clientMessageId)
                                if (keySession.includeKeyRecordsOnNextSend) {
                                    put(
                                        "recipient_key_records",
                                        buildRecipientKeyRecordsJson(keySession.encryptedKeys),
                                    )
                                }
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
                                        put(
                                            "encrypted_reply_preview",
                                            CryptoUtil.encryptWithAesKey(preview, keySession.rawAesKey),
                                        )
                                    }
                                }
                            }.also {
                                keySession.includeKeyRecordsOnNextSend = false
                            }
                        }
                        emitGroupMessageWithRetry(
                            payload = json,
                            clientMessageId = clientMessageId,
                        )
                        clearReply()
                    }
                    is Resource.Error -> {
                        _isUploading.value = false
                        _uploadError.value = res.message.resolveWith(stringResolver)
                    }
                }
            }
        }
    }

    private fun emitGroupMessageWithRetry(
        payload: JSONObject,
        clientMessageId: String,
    ) {
        when (
            appSocketManager.emitWithRetry(
                event = "send_group_message",
                payload = payload,
                dedupeKey = "group:$groupId:$clientMessageId",
            )
        ) {
            AppSocketManager.EmitWithRetryResult.EMITTED -> Unit
            AppSocketManager.EmitWithRetryResult.QUEUED,
            AppSocketManager.EmitWithRetryResult.ALREADY_QUEUED -> {
                _socketError.value = "Disconnected. Message queued and will retry automatically."
            }
            AppSocketManager.EmitWithRetryResult.QUEUE_FULL -> {
                _socketError.value = "Send queue is full. Please retry in a moment."
            }
        }
    }

    fun startRecording(context: Context) {
        if (_isRecording.value) return
        val recorder = VoiceRecorderManager(context)
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
        val recording = recorder.stopRecording()
        voiceRecorder = null
        if (recording != null) {
            sendAttachment(recording.toAndroidUri(), "voice")
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

    fun consumeExitChatReason() {
        _exitChatReason.value = null
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

        val emitted = appSocketManager.emit(
            "delete_group_message_for_me",
            JSONObject().apply {
                put("group_id", groupId)
                put("message_id", message.messageId)
            }
        )
        if (!emitted) {
            _socketError.value = "Unable to sync message deletion while offline."
        }
    }

    fun deleteMessageForEveryone(message: GroupMessageEntity) = viewModelScope.launch {
        deleteGroupMessageLocally(message)
        val emitted = appSocketManager.emit(
            event = "delete_group_message",
            payload = JSONObject().apply {
                put("group_id", groupId)
                put("message_id", message.messageId)
            },
        )
        if (!emitted) {
            _socketError.value = "Unable to delete for everyone right now."
        }
    }

    fun reactToMessage(message: GroupMessageEntity, emoji: String) = viewModelScope.launch {
        val messageId = message.messageId.trim()
        if (messageId.isBlank()) return@launch
        val emitted = appSocketManager.emit(
            "react_to_message",
            JSONObject().apply {
                put("message_id", messageId)
                put("emoji", emoji)
            }
        )
        if (!emitted) {
            when (groupRepository.reactToMessage(messageId = messageId, emoji = emoji)) {
                is Resource.Success -> Unit
                is Resource.Error -> {
                    _socketError.value = "Unable to react right now."
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun navigateUp() {
        stopTypingNow()
        clearTypingUsers()
        navigateUp(viewModelScope)
    }

    private fun revokeMembershipAndExit(message: String) {
        if (_exitChatReason.value != null) return
        _exitChatReason.value = message
        stopTypingNow()
        clearTypingUsers()
        _onlineGroupUsers.value = emptyList()
        ActiveGroupTracker.clearActiveGroup()
        disconnectSocket()
    }

    fun openPrivateKeyResetDialog() {
        navigateToDestination(AppRoute.PrivateKeyResetDialog, viewModelScope)
    }

    fun leaveGroup() = viewModelScope.launch {
        if (_isLeavingGroup.value) return@launch
        _isLeavingGroup.value = true
        try {
            when (val result = groupRepository.leaveGroup(groupId)) {
                is Resource.Success -> {
                    groupRepository.clearGroupMessages(groupId)
                    revokeMembershipAndExit("You left the group.")
                }
                is Resource.Error -> {
                    _socketError.value = result.message.resolveWith(stringResolver)
                }
                is Resource.Loading -> Unit
            }
        } finally {
            _isLeavingGroup.value = false
        }
    }

    fun openSharedPost(postId: Long) {
        navigateToDestination(
            AppRoute.SharedPostDetails(postId = postId),
            viewModelScope
        )
    }

    fun openGroupMembersScreen(currentGroupName: String) {
        navigateToDestination(
            AppRoute.GroupMembers(
                groupId = groupId,
                groupName = currentGroupName.ifBlank { groupName },
            ),
            viewModelScope,
        )
    }

    fun openPlaylistBottomSheet() {
        navigateToDestination(AppRoute.PlaylistSheet, viewModelScope)
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

    private fun sendGroupAck(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        if (!ActiveGroupTracker.isViewingGroup(groupId)) {
            Log.d(TAG, "Skipping group ack for inactive group $groupId")
            return
        }
        val payload = JSONObject().apply {
            put("group_id", groupId)
            put("message_ids", JSONArray(messageIds))
        }
        appSocketManager.emit("ack_group_messages", payload)
        Log.d(TAG, "Sent group ack for ${messageIds.size} message(s)")
    }

    private fun sendGroupRead(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        if (!ActiveGroupTracker.isViewingGroup(groupId)) {
            Log.d(TAG, "Skipping group read for inactive group $groupId")
            return
        }
        val payload = JSONObject().apply {
            put("group_id", groupId)
            put("message_ids", JSONArray(messageIds))
        }
        appSocketManager.emit("mark_read_group_messages", payload)
        Log.d(TAG, "Sent group read for ${messageIds.size} message(s)")
    }

    private fun refreshGroupOnlineUsersFromApi() = viewModelScope.launch {
        when (val result = groupRepository.getGroupOnlineUsers(groupId)) {
            is Resource.Success -> {
                _onlineGroupUsers.value = normalizeOnlineUsers(result.data)
            }
            is Resource.Error -> {
                Log.w(
                    TAG,
                    "Failed to fetch online group users from API: ${result.message.resolveWith(stringResolver)}"
                )
            }
            is Resource.Loading -> Unit
        }
    }

    private fun requestGroupOnlineUsersFromSocket() {
        if (!appSocketManager.isConnected()) return
        appSocketManager.emit(
            "get_group_online_users",
            JSONObject().apply { put("group_id", groupId) }
        )
    }

    private fun applyOnlineUsersSnapshot(onlineUsersArray: JSONArray?) {
        if (onlineUsersArray == null) return
        val parsedUsers = buildList {
            for (index in 0 until onlineUsersArray.length()) {
                val userJson = onlineUsersArray.optJSONObject(index) ?: continue
                parseOnlineUser(userJson)?.let { add(it) }
            }
        }
        _onlineGroupUsers.value = normalizeOnlineUsers(parsedUsers)
    }

    private fun applyOnlinePresenceDelta(payload: JSONObject) {
        val userJson = payload.optJSONObject("user")
        val online = payload.optBoolean("online", false)
        val normalizedUsername = userJson
            ?.optString("username", "")
            ?.trim()
            .orEmpty()
        if (normalizedUsername.isBlank()) return

        val updated = _onlineGroupUsers.value.associateBy { it.username }.toMutableMap()
        if (online) {
            parseOnlineUser(userJson)?.let { parsed ->
                updated[parsed.username] = parsed
            }
        } else {
            updated.remove(normalizedUsername)
        }
        _onlineGroupUsers.value = normalizeOnlineUsers(updated.values)
    }

    private fun parseOnlineUser(userJson: JSONObject?): GroupOnlineUser? {
        if (userJson == null) return null
        val username = userJson.optString("username", "").trim()
        if (username.isBlank()) return null

        val userIdFromPayload = userJson.optString("user_id", "").trim()
        val resolvedUserId = userIdFromPayload.ifBlank {
            memberIdsByUsername[username]?.toString().orEmpty()
        }
        val profileImageFromPayload = userJson.optString("profile_image_url", "").ifBlank { null }
        val resolvedProfileImage = profileImageFromPayload ?: _memberProfileImageUrls.value[username]
        val profileImageShapeFromPayload = userJson.optString("profile_image_shape", "").ifBlank { null }
        val resolvedProfileImageShape =
            profileImageShapeFromPayload ?: _memberProfileImageShapes.value[username]
        val badgeFromPayload = userJson.optString("badge", "").ifBlank { null }
        val resolvedBadge = badgeFromPayload ?: _memberBadges.value[username]

        return GroupOnlineUser(
            userId = resolvedUserId,
            username = username,
            profileImageUrl = resolvedProfileImage,
            profileImageShape = resolvedProfileImageShape,
            badge = resolvedBadge,
        )
    }

    private fun normalizeOnlineUsers(users: Collection<GroupOnlineUser>): List<GroupOnlineUser> {
        val allowedMembers = memberIdsByUsername.keys
        val filteredUsers = users
            .asSequence()
            .filter { it.username.isNotBlank() }
            .filter { allowedMembers.isEmpty() || allowedMembers.contains(it.username) }
            .map { user ->
                val fallbackId = memberIdsByUsername[user.username]?.toString().orEmpty()
                val fallbackProfile = _memberProfileImageUrls.value[user.username]
                val fallbackProfileShape = _memberProfileImageShapes.value[user.username]
                val fallbackBadge = _memberBadges.value[user.username]
                user.copy(
                    userId = user.userId.ifBlank { fallbackId },
                    profileImageUrl = user.profileImageUrl ?: fallbackProfile,
                    profileImageShape = user.profileImageShape ?: fallbackProfileShape,
                    badge = user.badge ?: fallbackBadge,
                )
            }
            .distinctBy { it.username }
            .sortedBy { it.username }
            .toList()

        return filteredUsers
    }

    private fun emitGroupTyping(isTyping: Boolean) {
        if (!appSocketManager.isConnected()) return
        appSocketManager.emit(
            "group_typing",
            JSONObject().apply {
                put("group_id", groupId)
                put("is_typing", isTyping)
            }
        )
    }

    private fun stopTypingNow() {
        composerTypingStopJob?.cancel()
        composerTypingStopJob = null
        typingHeartbeatJob?.cancel()
        typingHeartbeatJob = null
        if (!isTypingEmitted) return
        emitGroupTyping(isTyping = false)
        isTypingEmitted = false
    }

    private fun startTypingHeartbeatIfNeeded() {
        if (typingHeartbeatJob?.isActive == true) return
        typingHeartbeatJob = viewModelScope.launch {
            while (isTypingEmitted) {
                kotlinx.coroutines.delay(1700)
                if (!isTypingEmitted) break
                emitGroupTyping(isTyping = true)
            }
        }
    }

    private fun handleIncomingGroupTyping(payload: JSONObject) {
        val isTyping = payload.optBoolean("is_typing", false)
        val userJson = payload.optJSONObject("user")
        val username = userJson
            ?.optString("username", "")
            ?.trim()
            .orEmpty()
        if (username.isBlank() || username == currentUsername) return

        if (isTyping) {
            val typingUser = parseOnlineUser(userJson) ?: return
            typingUsersByUsername[username] = typingUser
            publishTypingUsers()
            typingExpiryJobsByUsername.remove(username)?.cancel()
            typingExpiryJobsByUsername[username] = viewModelScope.launch {
                kotlinx.coroutines.delay(4200)
                removeTypingUser(username)
            }
        } else {
            removeTypingUser(username)
        }
    }

    private fun removeTypingUser(username: String) {
        typingExpiryJobsByUsername.remove(username)?.cancel()
        if (typingUsersByUsername.remove(username) != null) {
            publishTypingUsers()
        }
    }

    private fun publishTypingUsers() {
        _typingGroupUsers.value = typingUsersByUsername.values
            .sortedBy { it.username }
    }

    private fun clearTypingUsers() {
        typingExpiryJobsByUsername.values.forEach { it.cancel() }
        typingExpiryJobsByUsername.clear()
        typingUsersByUsername.clear()
        _typingGroupUsers.value = emptyList()
    }

    private fun disconnectSocket() {
        if (appSocketManager.isConnected()) {
            appSocketManager.emit(
                "leave_group",
                JSONObject().apply { put("group_id", groupId) }
            )
        }
        appSocketManager.unregisterOwner(socketOwner)
    }

    private fun startMainThreadWatchdogIfDebug() {
        if (!appConfig.isDebug) return
        lastMainThreadHeartbeatMs = SystemClock.uptimeMillis()
        mainHandler.post(mainThreadHeartbeatRunnable)
        mainThreadWatchdogJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                kotlinx.coroutines.delay(1200L)
                val lagMs = SystemClock.uptimeMillis() - lastMainThreadHeartbeatMs
                if (lagMs > 6000L) {
                    if (!mainThreadStallReported) {
                        mainThreadStallReported = true
                        val mainStack = Looper.getMainLooper().thread.stackTrace
                            .joinToString(separator = "\n") { it.toString() }
                        Log.e(
                            TAG,
                            "Main thread stall detected in GroupChatViewModel. lagMs=$lagMs\n$mainStack"
                        )
                    }
                } else {
                    mainThreadStallReported = false
                }
            }
        }
    }

    private fun mapGroupMessageErrorToUiMessage(errorCode: String, fallback: String): String {
        return when (errorCode) {
            "group_side_effects_unavailable" ->
                "Server is busy right now. Your message was not sent. Please try again."
            "group_send_failed" ->
                "Could not send this group message. Please retry."
            "invalid_client_message_id" ->
                "Message id was invalid. Please send again."
            "missing_group_id",
            "missing_message_or_attachment",
            "missing_encrypted_keys",
            "missing_group_key_material",
            "unknown_group_key_ref",
            "invalid_attachment_payload",
            "invalid_message_type" -> "Message format was invalid. Please retry."
            "not_group_member" -> "You are no longer a member of this group."
            else -> fallback.ifBlank { "Failed to send group message" }
        }
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

    private fun payloadGroupIdsContainsCurrentGroup(payload: JSONObject): Boolean {
        val groupIds = payload.optJSONArray("group_ids") ?: return false
        for (index in 0 until groupIds.length()) {
            if (groupIds.optInt(index, -1) == groupId) return true
        }
        return false
    }

    private fun JSONObject?.toEncryptedKeysMap(): Map<String, String>? {
        if (this == null) return null
        val mapped = mutableMapOf<String, String>()
        val keysIterator = keys()
        while (keysIterator.hasNext()) {
            val username = keysIterator.next()
            val encryptedKey = optString(username, "")
            if (encryptedKey.isNotBlank()) {
                mapped[username] = encryptedKey
            }
        }
        return mapped.ifEmpty { null }
    }

    override fun onCleared() {
        stopTypingNow()
        clearTypingUsers()
        mainThreadWatchdogJob?.cancel()
        mainThreadWatchdogJob = null
        mainHandler.removeCallbacks(mainThreadHeartbeatRunnable)
        ActiveGroupTracker.clearActiveGroup()
        voiceRecorder?.release()
        disconnectSocket()
        super.onCleared()
    }
}
