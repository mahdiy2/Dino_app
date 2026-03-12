package com.dino.message.chatfeature.presentation.viewmodel

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.chatfeature.domain.usecase.ClearMessageFromDBUseCase
import com.dino.message.chatfeature.domain.usecase.ConnectUseCase
import com.dino.message.chatfeature.domain.usecase.GetMessageFromDBUseCase
import com.dino.message.chatfeature.domain.usecase.GetReceiverPublicKeyUseCase
import com.dino.message.chatfeature.domain.usecase.SaveMessageInDbUseCase
import com.dino.message.chatfeature.domain.usecase.SendMessageUseCase
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val spref: SPrefManager,
    private val connectUseCase: ConnectUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val getReceiverPublicKeyUseCase: GetReceiverPublicKeyUseCase,
    private val getMessageFromDBUseCase: GetMessageFromDBUseCase,
    private val saveMessageInDbUseCase: SaveMessageInDbUseCase,
    private val clearMessageFromDBUseCase: ClearMessageFromDBUseCase,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val receiverPublicKey = mutableStateOf<String?>(null)

    private val _messages = mutableStateListOf<MessageEntity>()
    val messages: List<MessageEntity> = _messages

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var getPublicKey = false
    private var isSocketConnecting = false

    init {
        loadMessages()
        getReceiverPublicKey()
        connectSocket()
    }

    private fun connectSocket() = viewModelScope.launch {
        isSocketConnecting = true
        showRefreshing()

        connectUseCase(
            onConnected = {
                isSocketConnecting = false
                showRefreshing()
                connectUseCase.getUserStatus(chatId)
            },
            onPendingMessages = { pendingMessages ->
                pendingMessages
                    .filter { it.from == chatId }
                    .forEach {
                        decryptAndSaveMessage(
                            sender = it.from,
                            encryptedMessage = it.message,
                            encryptedKey = it.encryptedKey,
                            timestamp = it.timestamp
                        )
                    }
            },
            onNewMessage = {
                if (it.from == chatId) {
                    decryptAndSaveMessage(
                        sender = it.from,
                        encryptedMessage = it.message,
                        encryptedKey = it.encryptedKey,
                        timestamp = it.timestamp
                    )
                }
            },
            onMessageSent = {
                Log.d("ChatViewModel", "Message sent to ${it.to}")
            },
            onMessageError = {
                Log.e("ChatViewModel", "Socket message error: $it")
            },
            onUserStatus = { username, online ->
                if (username == chatId) {
                    Log.d("ChatViewModel", "$username is ${if (online) "online" else "offline"}")
                }
            }
        )
    }

    private fun getReceiverPublicKey() = viewModelScope.launch {
        getReceiverPublicKeyUseCase.execute(chatId).collect { res ->
            when (res) {
                is Resource.Success -> {
                    receiverPublicKey.value = res.data.public_key
                    getPublicKey = false
                    showRefreshing()
                }

                is Resource.Loading -> {
                    getPublicKey = true
                    showRefreshing()
                }

                is Resource.Error -> {
                    getPublicKey = false
                    showRefreshing()
                }
            }
        }
    }

    fun sendMessage(message: String) {
        val publicKey = receiverPublicKey.value
        if (publicKey != null) {
            val (encryptedKey, encryptedMessage) = CryptoUtil.encryptMessage(
                message = message,
                receiverPublicKey = decodePublicKey(publicKey)
            )

            sendMessageUseCase.execute(chatId, encryptedMessage, encryptedKey)

            saveMessage(
                MessageEntity(
                    senderOrReceiver = getUsernameFromPrefs().toString(),
                    content = message,
                    timestamp = getCurrentTimestamp()
                )
            )
        } else {
            getReceiverPublicKey()
        }
    }

    fun loadMessages() = viewModelScope.launch {
        _messages.clear()
        getMessageFromDBUseCase.execute().collect {
            _messages.addAll(it)
        }
    }

    fun saveMessage(message: MessageEntity) = viewModelScope.launch {
        saveMessageInDbUseCase.execute(message).collect {
            _messages.add(message)
            loadMessages()
        }
    }

    fun clearMessages() = viewModelScope.launch {
        clearMessageFromDBUseCase.execute().collect {
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

    private fun showRefreshing() {
        _isRefreshing.value = getPublicKey || isSocketConnecting
    }

    private fun decryptAndSaveMessage(
        sender: String,
        encryptedMessage: String,
        encryptedKey: String,
        timestamp: String
    ) {
        try {
            val privateKey = spref.getPrivateKey() ?: return
            val decryptedMsg = decryptMessage(
                encryptedMessage = encryptedMessage,
                encryptedAESKey = encryptedKey,
                privateKey = decodePrivateKey(privateKey)
            )

            saveMessage(
                MessageEntity(
                    senderOrReceiver = sender,
                    content = decryptedMsg,
                    timestamp = timestamp
                )
            )
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Decrypt failed", e)
        }
    }
}
