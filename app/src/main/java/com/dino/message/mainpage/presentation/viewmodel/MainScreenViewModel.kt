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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.dino.message.BuildConfig
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.usecase.ListGroupsUseCase
import com.dino.message.mainpage.domain.model.MessageList
import com.dino.message.mainpage.domain.usecase.AddContactUseCase
import com.dino.message.mainpage.domain.usecase.GetContactsUseCase
import com.dino.message.mainpage.domain.usecase.GetDetailedContactsUseCase
import com.dino.message.threadfeature.domain.usecase.IsTrackInPlaylistUseCase
import com.ramcosta.composedestinations.generated.destinations.ChatScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CreateGroupScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GroupChatScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PlaylistBottomSheetScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ProfileScreenDestination
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val addContactUseCase: AddContactUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val getDetailedContactsUseCase: GetDetailedContactsUseCase,
    private val listGroupsUseCase: ListGroupsUseCase,
    private val isTrackInPlaylistUseCase: IsTrackInPlaylistUseCase,
    private val spref: SPrefManager,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    companion object {
        private const val TAG = "MainScreenVM"
        private const val PAGE_SIZE = 20
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

    private val context: Context = app.applicationContext

    private var socket: Socket? = null
    private var hasSocketStarted = false

    init {
        getDetailedContacts(page = 1, isRefresh = true)
        loadGroups()
        connectSocket()
    }

    fun refresh() {
        getDetailedContacts(page = 1, isRefresh = true)
        loadGroups()
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || !_hasMore.value || isGettingContacts) return
        getDetailedContacts(page = currentPage + 1, isRefresh = false)
    }

    private fun addContact(contact: String) = viewModelScope.launch {
        addContactUseCase.execute(contact).collect { res ->
            when (res) {
                is Resource.Success -> {
                    Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show()
                    hideBottomSheet()
                    getDetailedContacts(page = 1, isRefresh = true)
                    isAddingContacts = false
                }

                is Resource.Loading -> {
                    isAddingContacts = true
                }

                is Resource.Error -> {
                    Toast.makeText(context, "not found", Toast.LENGTH_SHORT).show()
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
                            time = contact.lastMessage?.timestamp ?: "",
                            newMessage = contact.hasMessage,
                            online = contact.online,
                            lastMessageType = contact.lastMessage?.type,
                            profileImageUrl = contact.profileImageUrl,
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
            .setPath("/socket.io/")
            .setTransports(arrayOf("websocket", "polling"))
            .setAuth(mapOf("token" to token))
            .build()

        try {
            val createdSocket = IO.socket(baseUrl, opts)
            registerSocketListeners(createdSocket)
            socket = createdSocket
            hasSocketStarted = true
            createdSocket.connect()
            Log.d(TAG, "Socket connecting to $baseUrl")
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Socket URL invalid", e)
        } catch (e: Exception) {
            Log.e(TAG, "Socket connect failed", e)
        }
    }

    private fun registerSocketListeners(socket: Socket) {
        socket.on("contacts_updated") {
            Log.d(TAG, "Received contacts_updated event")
            viewModelScope.launch {
                getDetailedContacts(page = 1, isRefresh = true)
            }
        }

        socket.on("new_notification") { args ->
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

        socket.on("pending_group_messages") { args ->
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

        socket.on("user_status") { args ->
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

        socket.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Socket connected")
        }

        socket.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Socket disconnected")
        }
    }

    private fun disconnectSocket() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    private fun updateGroupUnreadIndicator(groupId: Int, hasUnread: Boolean) {
        val index = _groupList.indexOfFirst { it.id == groupId }
        if (index >= 0) {
            _groupList[index] = _groupList[index].copy(hasUnread = hasUnread)
        }
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
        navigateToDestination(
            ChatScreenDestination(
                chatId = username,
                displayName = contact?.displayName,
                profileImageUrl = contact?.profileImageUrl,
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
