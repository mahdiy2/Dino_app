package com.dino.message.mainpage.presentation.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.R
import com.dino.message.corefeature.data.socket.AppSocketManager
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.usecase.ListGroupsUseCase
import com.dino.message.mainpage.data.remote.dto.DetailedContact
import com.dino.message.mainpage.domain.model.MessageList
import com.dino.message.mainpage.domain.usecase.AddContactUseCase
import com.dino.message.mainpage.domain.usecase.GetContactsUseCase
import com.dino.message.mainpage.domain.usecase.GetContactsDeltaUseCase
import com.dino.message.mainpage.domain.usecase.GetDetailedContactsUseCase
import com.dino.message.mainpage.presentation.util.ContactsDeltaReducer
import com.dino.message.threadfeature.domain.usecase.IsTrackInPlaylistUseCase
import com.ramcosta.composedestinations.generated.destinations.ChatScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CreateGroupScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GroupChatScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PlaylistBottomSheetScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ProfileScreenDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainScreenViewModel constructor(
    app: Context,
    private val addContactUseCase: AddContactUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val getContactsDeltaUseCase: GetContactsDeltaUseCase,
    private val getDetailedContactsUseCase: GetDetailedContactsUseCase,
    private val listGroupsUseCase: ListGroupsUseCase,
    private val isTrackInPlaylistUseCase: IsTrackInPlaylistUseCase,
    private val appSocketManager: AppSocketManager,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    companion object {
        private const val TAG = "MainScreenVM"
        private const val PAGE_SIZE = 20
        private const val SOCKET_OWNER = "MainScreenViewModel"
        private const val FULL_REFRESH_STALE_MS = 90_000L
        private const val DELTA_EVENTS_BEFORE_FULL_REFRESH = 40
    }

    var isError = mutableStateOf(false)

    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible = _isBottomSheetVisible.asStateFlow()

    private val _addUserTextField = mutableStateOf("")
    val addUserTextField = _addUserTextField

    private var _contactList = mutableStateListOf<MessageList?>(null)
    val contactList = _contactList

    private var _groupList = mutableStateListOf<Group>()
    val groupList: List<Group> get() = _groupList

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var currentPage = 1
    private var totalContacts = 0
    private var isGettingContacts = false
    private var isAddingContacts = false
    private var lastFullRefreshAtMs = 0L
    private var pendingReconnectRefresh = false
    private var pendingDeltaEvents = 0
    private val pendingDeltaContactFetches = mutableSetOf<String>()

    private val context: Context = app.applicationContext

    init {
        refreshFromServer(force = true)
        connectSocket()
    }

    fun refresh() {
        refreshFromServer(force = true)
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || !_hasMore.value || isGettingContacts) return
        getDetailedContacts(page = currentPage + 1, isRefresh = false)
    }

    private fun refreshFromServer(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val isStale = now - lastFullRefreshAtMs >= FULL_REFRESH_STALE_MS
        val shouldRefreshContacts = force || pendingReconnectRefresh || isStale

        if (shouldRefreshContacts && !isGettingContacts) {
            getDetailedContacts(page = 1, isRefresh = true)
        }
        if (shouldRefreshContacts) {
            loadGroups()
        }
    }

    private fun addContact(contact: String) = viewModelScope.launch {
        addContactUseCase.execute(contact).collect { res ->
            when (res) {
                is Resource.Success -> {
                    Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show()
                    hideBottomSheet()
                    getDetailedContacts(page = 1, isRefresh = true)
                    isAddingContacts = false
                }

                is Resource.Loading -> {
                    isAddingContacts = true
                }

                is Resource.Error -> {
                    Toast.makeText(context, context.getString(R.string.not_found_short), Toast.LENGTH_SHORT).show()
                    isAddingContacts = false
                }
            }
        }
    }

    private fun getDetailedContacts(page: Int, isRefresh: Boolean) = viewModelScope.launch {
        getDetailedContactsUseCase.execute(page = page, limit = PAGE_SIZE).collect { res ->
            when (res) {
                is Resource.Success -> {
                    val paginated = res.data
                    val mapped = paginated.contacts.map { contact ->
                        MessageList(
                            username = contact.username,
                            displayName = contact.name?.takeIf { it.isNotBlank() } ?: contact.username,
                            badge = contact.badge,
                            time = contact.lastMessage?.timestamp ?: "",
                            newMessage = contact.hasMessage,
                            online = contact.online,
                            lastMessageType = contact.lastMessage?.type,
                            profileImageUrl = contact.profileImageUrl,
                            profileImageShape = contact.profileImageShape,
                            profileVideoUrl = contact.profileVideoUrl,
                        )
                    }

                    if (isRefresh) {
                        _contactList.clear()
                        _contactList.addAll(mapped)
                    } else {
                        _contactList.addAll(mapped)
                    }

                    currentPage = paginated.page
                    totalContacts = paginated.total
                    _hasMore.value = paginated.hasMore
                    if (isRefresh) {
                        lastFullRefreshAtMs = System.currentTimeMillis()
                        pendingReconnectRefresh = false
                        pendingDeltaEvents = 0
                    }

                    isGettingContacts = false
                    _isLoadingMore.value = false
                    showRefreshing()
                }

                is Resource.Loading -> {
                    if (isRefresh) {
                        isGettingContacts = true
                    } else {
                        _isLoadingMore.value = true
                    }
                    showRefreshing()
                }

                is Resource.Error -> {
                    isGettingContacts = false
                    _isLoadingMore.value = false
                    showRefreshing()
                }
            }
        }
    }

    private fun loadGroups() = viewModelScope.launch {
        listGroupsUseCase.execute().collect { res ->
            when (res) {
                is Resource.Success -> {
                    val localUnreadByGroupId = _groupList.associate { it.id to it.hasUnread }
                    _groupList.clear()
                    _groupList.addAll(
                        res.data.map { group ->
                            val localUnread = localUnreadByGroupId[group.id] == true
                            group.copy(hasUnread = group.hasUnread || localUnread)
                        }
                    )
                }
                is Resource.Loading -> { }
                is Resource.Error -> {
                    Log.e(TAG, "Failed to load groups")
                }
            }
        }
    }

    private fun connectSocket() {
        registerSocketListeners()
        appSocketManager.connect()
    }

    private fun registerSocketListeners() {
        appSocketManager.registerListener(SOCKET_OWNER, "contacts_updated") { args ->
            viewModelScope.launch {
                try {
                    val payload = extractPayloadArg(args) as? JSONObject ?: return@launch
                    applyContactsDelta(payload)
                } catch (e: Exception) {
                    Log.e(TAG, "contacts_updated handling error", e)
                }
            }
        }

        appSocketManager.registerListener(SOCKET_OWNER, "new_notification") { args ->
            viewModelScope.launch {
                try {
                    val payload = extractPayloadArg(args) as? JSONObject ?: return@launch
                    val groupId = payload.optInt("group_id", -1)
                    if (groupId > 0) {
                        updateGroupUnreadIndicator(groupId = groupId, hasUnread = true)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "new_notification handling error", e)
                }
            }
        }

        appSocketManager.registerListener(SOCKET_OWNER, "pending_group_messages") { args ->
            viewModelScope.launch {
                try {
                    val payload = extractPayloadArg(args) as? JSONObject ?: return@launch
                    val groupId = payload.optInt("group_id", -1)
                    if (groupId > 0) {
                        updateGroupUnreadIndicator(groupId = groupId, hasUnread = true)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "pending_group_messages handling error", e)
                }
            }
        }

        appSocketManager.registerListener(SOCKET_OWNER, "user_status") { args ->
            viewModelScope.launch {
                try {
                    val payload = args.firstOrNull()
                    if (payload is org.json.JSONObject) {
                        val username = payload.optString("username")
                        val online = payload.optBoolean("online", false)
                        val index = _contactList.indexOfFirst { it?.username == username }
                        if (index >= 0) {
                            _contactList[index] = _contactList[index]?.copy(online = online)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "user_status handling error", e)
                }
            }
        }

        appSocketManager.registerListener(SOCKET_OWNER, io.socket.client.Socket.EVENT_CONNECT) {
            Log.d(TAG, "Socket connected")
            viewModelScope.launch {
                refreshFromServer(force = false)
            }
        }

        appSocketManager.registerListener(SOCKET_OWNER, io.socket.client.Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Socket disconnected")
            pendingReconnectRefresh = true
        }
    }

    private fun disconnectSocket() {
        appSocketManager.unregisterOwner(SOCKET_OWNER)
    }

    private fun updateGroupUnreadIndicator(groupId: Int, hasUnread: Boolean) {
        val index = _groupList.indexOfFirst { it.id == groupId }
        if (index >= 0) {
            _groupList[index] = _groupList[index].copy(hasUnread = hasUnread)
        }
    }

    private fun applyContactsDelta(payload: JSONObject) {
        val contactUsername = resolveContactUsername(payload)
        if (contactUsername.isBlank()) return

        val messageTimestamp = payload.optString("timestamp").trim()
        val messageType = payload.optString("type").trim().ifBlank { null }
        val existingIndex = _contactList.indexOfFirst { it?.username == contactUsername }
        val existingItem = if (existingIndex >= 0) _contactList[existingIndex] else null

        val hasUnread = if (payload.has("has_unread")) {
            payload.optBoolean("has_unread", existingItem?.newMessage == true)
        } else {
            inferLegacyUnreadState(payload, contactUsername, existingItem)
        }
        val reduced = ContactsDeltaReducer.apply(
            currentContacts = _contactList,
            contactUsername = contactUsername,
            hasUnread = hasUnread,
            messageTimestamp = messageTimestamp,
            messageType = messageType,
        )
        _contactList.clear()
        _contactList.addAll(reduced.contacts)
        if (reduced.insertedNewContact) {
            totalContacts += 1
            fetchMissingContactDetails(contactUsername)
        }

        pendingDeltaEvents += 1
        if (pendingDeltaEvents >= DELTA_EVENTS_BEFORE_FULL_REFRESH) {
            refreshFromServer(force = true)
        }
    }

    private fun fetchMissingContactDetails(contactUsername: String) {
        if (!pendingDeltaContactFetches.add(contactUsername)) return

        viewModelScope.launch {
            try {
                getContactsDeltaUseCase.execute(listOf(contactUsername)).collect { res ->
                    when (res) {
                        is Resource.Success -> {
                            val detailed = res.data.firstOrNull { it.username == contactUsername }
                            if (detailed != null) {
                                mergeContactDeltaDetails(detailed)
                            }
                        }
                        is Resource.Loading -> Unit
                        is Resource.Error -> Unit
                    }
                }
            } finally {
                pendingDeltaContactFetches.remove(contactUsername)
            }
        }
    }

    private fun mergeContactDeltaDetails(detailed: DetailedContact) {
        val index = _contactList.indexOfFirst { it?.username == detailed.username }
        if (index < 0) return

        val item = _contactList[index] ?: return
        _contactList[index] = item.copy(
            displayName = detailed.name?.takeIf { it.isNotBlank() } ?: item.displayName,
            badge = detailed.badge ?: item.badge,
            profileImageUrl = detailed.profileImageUrl ?: item.profileImageUrl,
            profileImageShape = detailed.profileImageShape ?: item.profileImageShape,
            profileVideoUrl = detailed.profileVideoUrl ?: item.profileVideoUrl,
            online = detailed.online,
            newMessage = item.newMessage || detailed.hasMessage,
            lastMessageType = detailed.lastMessage?.type ?: item.lastMessageType,
            time = item.time.ifBlank { detailed.lastMessage?.timestamp.orEmpty() },
        )
    }

    private fun resolveContactUsername(payload: JSONObject): String {
        val directContact = payload.optString("contact").trim()
        if (directContact.isNotBlank()) return directContact

        val from = payload.optString("from").trim()
        val to = payload.optString("to").trim()

        val fromIndex = _contactList.indexOfFirst { it?.username == from }
        val toIndex = _contactList.indexOfFirst { it?.username == to }

        return when {
            from.isNotBlank() && to.isBlank() -> from
            to.isNotBlank() && from.isBlank() -> to
            fromIndex >= 0 && toIndex < 0 -> from
            toIndex >= 0 && fromIndex < 0 -> to
            from.isNotBlank() -> from
            else -> to
        }
    }

    private fun inferLegacyUnreadState(
        payload: JSONObject,
        contactUsername: String,
        existingItem: MessageList?,
    ): Boolean {
        val from = payload.optString("from").trim()
        return when {
            from.isBlank() -> existingItem?.newMessage == true
            from == contactUsername -> true
            else -> existingItem?.newMessage == true
        }
    }

    private fun clearLocalUnreadForContact(username: String) {
        val index = _contactList.indexOfFirst { it?.username == username }
        if (index < 0) return
        val item = _contactList[index] ?: return
        _contactList[index] = item.copy(newMessage = false)
    }

    private fun extractPayloadArg(args: Array<out Any>): Any? {
        for (arg in args) {
            when (arg) {
                is JSONObject, is JSONArray -> return arg
                is String -> {
                    val trimmed = arg.trim()
                    if (!trimmed.startsWith("{")) continue
                    return try {
                        JSONObject(trimmed)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
        return args.firstOrNull()
    }

    fun isConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun navigateToProfileScreen() {
        navigateToDestination(ProfileScreenDestination(), viewModelScope)
    }

    fun navigateToChatScreen(username: String) {
        val contact = _contactList.firstOrNull { it?.username == username }
        clearLocalUnreadForContact(username)
        navigateToDestination(
            ChatScreenDestination(
                chatId = username,
                displayName = contact?.displayName,
                badge = contact?.badge,
                profileImageUrl = contact?.profileImageUrl,
                profileImageShape = contact?.profileImageShape,
                profileVideoUrl = contact?.profileVideoUrl
            ),
            viewModelScope
        )
    }

    fun navigateToCreateGroupScreen() {
        navigateToDestination(CreateGroupScreenDestination, viewModelScope)
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

    fun navigateToGroupChatScreen(group: Group) {
        updateGroupUnreadIndicator(group.id, hasUnread = false)
        navigateToDestination(
            GroupChatScreenDestination(
                groupId = group.id,
                groupName = group.name,
                isPublicRoom = group.isPublicRoom,
            ),
            viewModelScope
        )
    }

    fun showBottomSheet() {
        _isBottomSheetVisible.value = true
    }

    fun hideBottomSheet() {
        _isBottomSheetVisible.value = false
        _addUserTextField.value = ""
    }

    fun onAddNewUserBtnClicked() {
        addContact(
            _addUserTextField.value
        )
    }

    private fun showRefreshing() {
        _isRefreshing.value = isGettingContacts || isAddingContacts
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }

    override fun onCleared() {
        disconnectSocket()
        super.onCleared()
    }
}
