package com.dino.message.threadfeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.BuildConfig
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.CryptoUtil
import com.dino.message.corefeature.presentation.util.encodePostShareMessage
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.util.decodePublicKey
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.chatfeature.domain.usecase.GetReceiverPublicKeyUseCase
import com.dino.message.chatfeature.domain.usecase.SaveMessageInDbUseCase
import com.dino.message.groupfeature.domain.model.GroupMessageEntity
import com.dino.message.groupfeature.domain.usecase.GetGroupDetailUseCase
import com.dino.message.groupfeature.domain.usecase.ListGroupsUseCase
import com.dino.message.groupfeature.domain.usecase.SaveGroupMessageUseCase
import com.dino.message.mainpage.domain.usecase.GetDetailedContactsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.URISyntaxException
import java.security.PublicKey
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

private const val CONTACTS_PAGE_SIZE = 50
private const val SOCKET_CONNECT_TIMEOUT_MS = 7000L

data class ShareTargetContact(
    val username: String,
    val displayName: String,
    val profileImageUrl: String?,
)

data class ShareTargetGroup(
    val id: Int,
    val name: String,
    val memberCount: Int,
)

@HiltViewModel
class SharePostBottomSheetViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val getDetailedContactsUseCase: GetDetailedContactsUseCase,
    private val listGroupsUseCase: ListGroupsUseCase,
    private val getReceiverPublicKeyUseCase: GetReceiverPublicKeyUseCase,
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val saveMessageInDbUseCase: SaveMessageInDbUseCase,
    private val saveGroupMessageUseCase: SaveGroupMessageUseCase,
    private val sPrefManager: SPrefManager,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val postId: Long = savedStateHandle.get<Long>("postId") ?: 0L
    private val authorUsername: String = savedStateHandle.get<String>("authorUsername").orEmpty()
    private val postText: String = savedStateHandle.get<String>("postText").orEmpty()

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

    private var socket: Socket? = null

    val selectedCount: Int
        get() = selectedContacts.values.count { it } + selectedGroups.values.count { it }

    init {
        loadRecipients()
        connectSocketIfNeeded()
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

    fun sendPostToSelectedRecipients() {
        if (_isSending.value) return

        val targetsContacts = selectedContacts.filterValues { it }.keys.toList()
        val targetGroups = selectedGroups.filterValues { it }.keys.toList()
        if (targetsContacts.isEmpty() && targetGroups.isEmpty()) {
            _error.value = "Select at least one contact or group."
            return
        }

        viewModelScope.launch {
            _isSending.value = true
            _error.value = null

            val activeSocket = connectSocketIfNeeded()
            if (activeSocket == null || !ensureSocketConnected(activeSocket)) {
                _isSending.value = false
                _error.value = "Unable to connect to messaging server."
                return@launch
            }

            val message = buildSharedPostMessage()
            var successCount = 0
            val failedTargets = mutableListOf<String>()

            targetsContacts.forEach { username ->
                val error = sendPrivateSharedMessage(
                    activeSocket = activeSocket,
                    username = username,
                    message = message,
                )
                if (error == null) {
                    successCount++
                } else {
                    failedTargets.add("@$username ($error)")
                }
            }

            targetGroups.forEach { groupId ->
                val group = groups.firstOrNull { it.id == groupId }
                val error = sendGroupSharedMessage(
                    activeSocket = activeSocket,
                    groupId = groupId,
                    message = message,
                )
                if (error == null) {
                    successCount++
                } else {
                    failedTargets.add("${group?.name ?: "Group $groupId"} ($error)")
                }
            }

            _isSending.value = false
            when {
                successCount > 0 && failedTargets.isEmpty() -> navigateUp(viewModelScope)
                successCount > 0 -> {
                    _error.value = "Sent to $successCount target(s). Failed for: ${failedTargets.joinToString(", ")}"
                }
                else -> {
                    _error.value = "Failed to send. ${failedTargets.joinToString(", ")}"
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
                                profileImageUrl = contact.profileImageUrl,
                            )
                        }
                        hasMore = res.data.hasMore
                        page += 1
                    }
                    is Resource.Error -> {
                        pageFailed = true
                        hasMore = false
                        _error.value = res.message.asString(getApplication())
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
                        _error.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    private suspend fun sendPrivateSharedMessage(
        activeSocket: Socket,
        username: String,
        message: String,
    ): String? {
        val receiverPublicKey = fetchReceiverPublicKey(username)
            ?: return "Could not fetch encryption key"
        return try {
            val (encryptedKey, encryptedMessage) = CryptoUtil.encryptMessage(
                message = message,
                receiverPublicKey = decodePublicKey(receiverPublicKey)
            )
            val payload = JSONObject().apply {
                put("to", username)
                put("message", encryptedMessage)
                put("encrypted_key", encryptedKey)
                put("type", "post")
            }
            activeSocket.emit("send_message", payload)
            runCatching {
                saveLocalPrivateShare(username = username, encodedMessage = message)
            }
            null
        } catch (e: Exception) {
            val detail = e.message?.takeIf { it.isNotBlank() } ?: "invalid public key"
            "Encryption failed: $detail"
        }
    }

    private suspend fun sendGroupSharedMessage(
        activeSocket: Socket,
        groupId: Int,
        message: String,
    ): String? {
        val memberKeys = fetchGroupMemberKeys(groupId)
        if (memberKeys.isEmpty()) return "No valid group member keys"

        return try {
            val encryptedResult = CryptoUtil.encryptGroupMessage(message, memberKeys)
            val payload = JSONObject().apply {
                put("group_id", groupId)
                put("message", encryptedResult.encryptedMessage)
                put("encrypted_keys", JSONObject(encryptedResult.encryptedKeys))
                put("type", "post")
            }
            activeSocket.emit("send_group_message", payload)
            runCatching {
                saveLocalGroupShare(groupId = groupId, encodedMessage = message)
            }
            null
        } catch (e: Exception) {
            val detail = e.message?.takeIf { it.isNotBlank() } ?: "invalid group key data"
            "Group encryption failed: $detail"
        }
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
                    _error.value = res.message.asString(getApplication())
                }
            }
        }
        return publicKey
    }

    private suspend fun fetchGroupMemberKeys(groupId: Int): Map<String, PublicKey> {
        var keys: Map<String, PublicKey> = emptyMap()
        getGroupDetailUseCase.execute(groupId).collect { res ->
            when (res) {
                is Resource.Loading -> Unit
                is Resource.Success -> {
                    keys = res.data.members.orEmpty()
                        .mapNotNull { member ->
                            val publicKeyText = member.publicKey?.takeIf { it.isNotBlank() }
                                ?: return@mapNotNull null
                            try {
                                member.username to decodePublicKey(publicKeyText)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        .toMap()
                }
                is Resource.Error -> {
                    _error.value = res.message.asString(getApplication())
                }
            }
        }
        return keys
    }

    private fun buildSharedPostMessage(): String {
        return encodePostShareMessage(
            postId = postId,
            authorUsername = authorUsername,
            postText = postText
        )
    }

    private suspend fun saveLocalPrivateShare(username: String, encodedMessage: String) {
        val currentUsername = sPrefManager.getUsername().orEmpty().ifBlank { "self" }
        saveMessageInDbUseCase.execute(
            MessageEntity(
                messageId = null,
                chatId = username,
                senderOrReceiver = currentUsername,
                content = encodedMessage,
                timestamp = Instant.now().toString(),
                messageType = "post",
            )
        ).collect { }
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

    private fun connectSocketIfNeeded(): Socket? {
        if (socket?.connected() == true) {
            return socket
        }
        if (socket == null) {
            socket = createSocket()
        }
        socket?.connect()
        return socket
    }

    private fun createSocket(): Socket? {
        val token = sPrefManager.getToken()
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .trim()
        if (token.isBlank()) return null

        val opts = IO.Options.builder()
            .setForceNew(true)
            .setMultiplex(false)
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(1_000)
            .setReconnectionDelayMax(8_000)
            .setTimeout(20_000)
            .setPath("/socket.io/")
            .setTransports(arrayOf("websocket", "polling"))
            .setAuth(mapOf("token" to token))
            .build()

        return try {
            IO.socket(BuildConfig.BASE_URL.trimEnd('/'), opts)
        } catch (_: URISyntaxException) {
            null
        }
    }

    private suspend fun ensureSocketConnected(socket: Socket): Boolean {
        if (socket.connected()) return true

        val connected = CompletableDeferred<Boolean>()
        val onConnect = Emitter.Listener {
            if (!connected.isCompleted) connected.complete(true)
        }
        val onError = Emitter.Listener {
            if (!connected.isCompleted) connected.complete(false)
        }

        socket.once(Socket.EVENT_CONNECT, onConnect)
        socket.once(Socket.EVENT_CONNECT_ERROR, onError)
        socket.connect()

        val result = withTimeoutOrNull(SOCKET_CONNECT_TIMEOUT_MS) { connected.await() } ?: false
        socket.off(Socket.EVENT_CONNECT, onConnect)
        socket.off(Socket.EVENT_CONNECT_ERROR, onError)
        return result
    }

    override fun onCleared() {
        socket?.off()
        socket?.disconnect()
        socket = null
        super.onCleared()
    }
}
