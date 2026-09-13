package com.dino.message.threadfeature.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.domain.crypto.MessageCrypto
import com.dino.message.corefeature.domain.crypto.RsaPublicKey
import com.dino.message.corefeature.domain.socket.RealtimeSocketGateway
import com.dino.message.corefeature.presentation.util.encodePostShareMessage
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.chatfeature.domain.usecase.GetReceiverPublicKeyUseCase
import com.dino.message.groupfeature.domain.model.GroupMessageEntity
import com.dino.message.groupfeature.domain.usecase.GetGroupDetailUseCase
import com.dino.message.groupfeature.domain.usecase.ListGroupsUseCase
import com.dino.message.groupfeature.domain.usecase.SaveGroupMessageUseCase
import com.dino.message.mainpage.domain.usecase.GetDetailedContactsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

private const val CONTACTS_PAGE_SIZE = 50

data class ShareTargetContact(
    val username: String,
    val displayName: String,
    val badge: String? = null,
    val profileImageUrl: String?,
    val profileImageShape: String? = null,
)

data class ShareTargetGroup(
    val id: Int,
    val name: String,
    val memberCount: Int,
)

private enum class ShareDispatchStatus {
    SENT,
    QUEUED,
    FAILED,
}

private data class ShareDispatchResult(
    val status: ShareDispatchStatus,
    val error: String? = null,
)

class SharePostBottomSheetViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    private val getDetailedContactsUseCase: GetDetailedContactsUseCase,
    private val listGroupsUseCase: ListGroupsUseCase,
    private val getReceiverPublicKeyUseCase: GetReceiverPublicKeyUseCase,
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val saveGroupMessageUseCase: SaveGroupMessageUseCase,
    private val sPrefManager: AuthSessionStore,
    private val appSocketManager: RealtimeSocketGateway,
    private val messageCrypto: MessageCrypto,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    private val _contacts = mutableStateListOf<ShareTargetContact>()
    val contacts: List<ShareTargetContact> = _contacts

    private val _groups = mutableStateListOf<ShareTargetGroup>()
    val groups: List<ShareTargetGroup> = _groups

    private val selectedContacts = mutableStateMapOf<String, Boolean>()
    private val selectedGroups = mutableStateMapOf<Int, Boolean>()

    private val _isRecipientsLoading = MutableStateFlow(false)
    val isRecipientsLoading: StateFlow<Boolean> = _isRecipientsLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val selectedCount: Int
        get() = selectedContacts.values.count { it } + selectedGroups.values.count { it }

    init {
        loadRecipients()
        appSocketManager.connect()
    }

    fun isContactSelected(username: String): Boolean = selectedContacts[username] == true

    fun isGroupSelected(groupId: Int): Boolean = selectedGroups[groupId] == true

    fun toggleContact(username: String) {
        selectedContacts[username] = !isContactSelected(username)
        _error.value = null
    }

    fun toggleGroup(groupId: Int) {
        selectedGroups[groupId] = !isGroupSelected(groupId)
        _error.value = null
    }

    fun sendPostToSelectedRecipients(postId: Long, authorUsername: String, postText: String) {
        if (_isSending.value) return

        val targetsContacts = selectedContacts.filterValues { it }.keys.toList()
        val targetGroups = selectedGroups.filterValues { it }.keys.toList()
        if (targetsContacts.isEmpty() && targetGroups.isEmpty()) {
            _error.value = str(AppStringKey.ShareErrorSelectTarget)
            return
        }

        viewModelScope.launch {
            _isSending.value = true
            _error.value = null

            connectSocketIfNeeded()

            val message = buildSharedPostMessage(postId, authorUsername, postText)
            var sentCount = 0
            var queuedCount = 0
            val failedTargets = mutableListOf<String>()

            targetsContacts.forEach { username ->
                val result = sendPrivateSharedMessage(
                    username = username,
                    message = message,
                )
                when (result.status) {
                    ShareDispatchStatus.SENT -> sentCount++
                    ShareDispatchStatus.QUEUED -> queuedCount++
                    ShareDispatchStatus.FAILED -> failedTargets.add(
                        str(
                            AppStringKey.ShareFailedTargetFormat,
                            "@$username",
                            result.error ?: str(AppStringKey.UnknownErrorOccurred)
                        )
                    )
                }
            }

            targetGroups.forEach { groupId ->
                val group = groups.firstOrNull { it.id == groupId }
                val result = sendGroupSharedMessage(
                    groupId = groupId,
                    message = message,
                )
                when (result.status) {
                    ShareDispatchStatus.SENT -> sentCount++
                    ShareDispatchStatus.QUEUED -> queuedCount++
                    ShareDispatchStatus.FAILED -> failedTargets.add(
                        str(
                            AppStringKey.ShareFailedTargetFormat,
                            group?.name ?: str(AppStringKey.ShareGroupFallback, groupId),
                            result.error ?: str(AppStringKey.UnknownErrorOccurred)
                        )
                    )
                }
            }

            _isSending.value = false
            when {
                failedTargets.isEmpty() && queuedCount == 0 && sentCount > 0 -> {
                    navigateUp(viewModelScope)
                }
                failedTargets.isEmpty() && queuedCount > 0 -> {
                    _error.value = str(AppStringKey.ShareStatusQueued, queuedCount)
                }
                sentCount > 0 || queuedCount > 0 -> {
                    _error.value = str(
                        AppStringKey.ShareStatusPartial,
                        sentCount,
                        queuedCount,
                        failedTargets.joinToString(", ")
                    )
                }
                else -> {
                    _error.value = str(AppStringKey.ShareStatusFailed, failedTargets.joinToString(", "))
                }
            }
        }
    }

    private fun loadRecipients() = viewModelScope.launch {
        _isRecipientsLoading.value = true
        _error.value = null

        loadAllContacts()
        loadGroups()

        _isRecipientsLoading.value = false
    }

    private suspend fun loadAllContacts() {
        val allContacts = mutableListOf<ShareTargetContact>()
        var page = 1
        var hasMore = true

        while (hasMore) {
            var pageFailed = false
            getDetailedContactsUseCase.execute(page = page, limit = CONTACTS_PAGE_SIZE).collect { res ->
                when (res) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        allContacts += res.data.contacts.map { contact ->
                            ShareTargetContact(
                                username = contact.username,
                                displayName = contact.name?.takeIf { it.isNotBlank() }
                                    ?: contact.username,
                                badge = contact.badge,
                                profileImageUrl = contact.profileImageUrl,
                                profileImageShape = contact.profileImageShape,
                            )
                        }
                        hasMore = res.data.hasMore
                        page += 1
                    }
                    is Resource.Error -> {
                        pageFailed = true
                        hasMore = false
                        _error.value = res.message.resolveWith(stringResolver)
                    }
                }
            }
            if (pageFailed) break
        }

        _contacts.clear()
        _contacts.addAll(allContacts.distinctBy { it.username })
    }

    private suspend fun loadGroups() {
        listGroupsUseCase.execute().collect { res ->
            when (res) {
                is Resource.Loading -> Unit
                is Resource.Success -> {
                    _groups.clear()
                    _groups.addAll(
                        res.data.map { group ->
                            ShareTargetGroup(
                                id = group.id,
                                name = group.name,
                                memberCount = group.memberCount
                            )
                        }
                    )
                }
                is Resource.Error -> {
                    if (_error.value == null) {
                        _error.value = res.message.resolveWith(stringResolver)
                    }
                }
            }
        }
    }

    private suspend fun sendPrivateSharedMessage(
        username: String,
        message: String,
    ): ShareDispatchResult {
        val receiverPublicKey = fetchReceiverPublicKey(username)
            ?: return ShareDispatchResult(
                status = ShareDispatchStatus.FAILED,
                error = str(AppStringKey.ShareErrorFetchKey),
            )
        return try {
            val clientMessageId = UUID.randomUUID().toString()
            val encryptedPayload = messageCrypto.encryptMessage(
                message = message,
                receiverPublicKey = messageCrypto.decodePublicKey(receiverPublicKey)
            )
            val senderEncrypted = runCatching {
                val selfPublicKey = sPrefManager.getPublicKey().orEmpty()
                if (selfPublicKey.isBlank()) {
                    null
                } else {
                    val selfPayload = messageCrypto.encryptMessage(
                        message = message,
                        receiverPublicKey = messageCrypto.decodePublicKey(selfPublicKey)
                    )
                    selfPayload.encryptedMessage to selfPayload.encryptedKey
                }
            }.getOrNull()
            val payload = JSONObject().apply {
                put("to", username)
                put("message", encryptedPayload.encryptedMessage)
                put("encrypted_key", encryptedPayload.encryptedKey)
                if (senderEncrypted != null) {
                    put("sender_encrypted_message", senderEncrypted.first)
                    put("sender_encrypted_key", senderEncrypted.second)
                }
                put("type", "post")
                put("client_message_id", clientMessageId)
            }
            val dispatchResult = appSocketManager.emitWithRetry(
                event = "send_message",
                payload = payload,
                dedupeKey = "share-private:$username:$clientMessageId",
            )
            when (dispatchResult) {
                RealtimeSocketGateway.EmitWithRetryResult.EMITTED ->
                    ShareDispatchResult(status = ShareDispatchStatus.SENT)
                RealtimeSocketGateway.EmitWithRetryResult.QUEUED,
                RealtimeSocketGateway.EmitWithRetryResult.ALREADY_QUEUED -> {
                    ShareDispatchResult(status = ShareDispatchStatus.QUEUED)
                }
                RealtimeSocketGateway.EmitWithRetryResult.QUEUE_FULL -> {
                    ShareDispatchResult(
                        status = ShareDispatchStatus.FAILED,
                        error = str(AppStringKey.ShareErrorQueueFull),
                    )
                }
            }
        } catch (e: Exception) {
            val detail = e.message?.takeIf { it.isNotBlank() } ?: str(AppStringKey.ShareErrorInvalidPublicKey)
            ShareDispatchResult(
                status = ShareDispatchStatus.FAILED,
                error = str(AppStringKey.ShareErrorEncryptionFailed, detail),
            )
        }
    }

    private suspend fun sendGroupSharedMessage(
        groupId: Int,
        message: String,
    ): ShareDispatchResult {
        val memberKeys = fetchGroupMemberKeys(groupId)
        if (memberKeys.isEmpty()) {
            return ShareDispatchResult(
                status = ShareDispatchStatus.FAILED,
                error = str(AppStringKey.ShareErrorGroupNoKeys),
            )
        }

        return try {
            val clientMessageId = UUID.randomUUID().toString()
            val encryptedResult = messageCrypto.encryptGroupMessage(message, memberKeys)
            val payload = JSONObject().apply {
                put("group_id", groupId)
                put("message", encryptedResult.encryptedMessage)
                put("group_key_ref", UUID.randomUUID().toString())
                put("recipient_key_records", buildRecipientKeyRecordsJson(encryptedResult.encryptedKeys))
                put("type", "post")
                put("client_message_id", clientMessageId)
            }
            val dispatchResult = appSocketManager.emitWithRetry(
                event = "send_group_message",
                payload = payload,
                dedupeKey = "share-group:$groupId:$clientMessageId",
            )
            when (dispatchResult) {
                RealtimeSocketGateway.EmitWithRetryResult.EMITTED -> {
                    runCatching {
                        saveLocalGroupShare(groupId = groupId, encodedMessage = message)
                    }
                    ShareDispatchResult(status = ShareDispatchStatus.SENT)
                }
                RealtimeSocketGateway.EmitWithRetryResult.QUEUED,
                RealtimeSocketGateway.EmitWithRetryResult.ALREADY_QUEUED -> {
                    ShareDispatchResult(status = ShareDispatchStatus.QUEUED)
                }
                RealtimeSocketGateway.EmitWithRetryResult.QUEUE_FULL -> {
                    ShareDispatchResult(
                        status = ShareDispatchStatus.FAILED,
                        error = str(AppStringKey.ShareErrorQueueFull),
                    )
                }
            }
        } catch (e: Exception) {
            val detail = e.message?.takeIf { it.isNotBlank() } ?: str(AppStringKey.ShareErrorInvalidGroupKeyData)
            ShareDispatchResult(
                status = ShareDispatchStatus.FAILED,
                error = str(AppStringKey.ShareErrorGroupEncryptionFailed, detail),
            )
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

    private suspend fun fetchReceiverPublicKey(username: String): String? {
        var publicKey: String? = null
        getReceiverPublicKeyUseCase.execute(username).collect { res ->
            when (res) {
                is Resource.Loading -> Unit
                is Resource.Success -> {
                    publicKey = res.data.public_key.takeIf { it.isNotBlank() }
                }
                is Resource.Error -> {
                    _error.value = res.message.resolveWith(stringResolver)
                }
            }
        }
        return publicKey
    }

    private suspend fun fetchGroupMemberKeys(groupId: Int): Map<String, RsaPublicKey> {
        var keys: Map<String, RsaPublicKey> = emptyMap()
        getGroupDetailUseCase.execute(groupId).collect { res ->
            when (res) {
                is Resource.Loading -> Unit
                is Resource.Success -> {
                    keys = res.data.members.orEmpty()
                        .mapNotNull { member ->
                            val publicKeyText = member.publicKey?.takeIf { it.isNotBlank() }
                                ?: return@mapNotNull null
                            try {
                                member.username to messageCrypto.decodePublicKey(publicKeyText)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        .toMap()
                }
                is Resource.Error -> {
                    _error.value = res.message.resolveWith(stringResolver)
                }
            }
        }
        return keys
    }

    private fun buildSharedPostMessage(
        postId: Long,
        authorUsername: String,
        postText: String,
    ): String {
        return encodePostShareMessage(
            postId = postId,
            authorUsername = authorUsername,
            postText = postText
        )
    }

    private suspend fun saveLocalGroupShare(groupId: Int, encodedMessage: String) {
        val currentUsername = sPrefManager.getUsername().orEmpty().ifBlank { "self" }
        saveGroupMessageUseCase.execute(
            GroupMessageEntity(
                messageId = "local-share-${UUID.randomUUID()}",
                groupId = groupId,
                sender = currentUsername,
                content = encodedMessage,
                timestamp = Instant.now().toString(),
                messageType = "post",
            )
        ).collect { }
    }

    private fun connectSocketIfNeeded() {
        appSocketManager.connect()
    }

    private fun str(key: AppStringKey, vararg args: Any): String {
        return stringResolver.resolve(key, *args)
    }

}
