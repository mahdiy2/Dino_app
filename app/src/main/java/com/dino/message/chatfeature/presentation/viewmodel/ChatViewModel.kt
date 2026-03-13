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
import com.dino.message.chatfeature.domain.usecase.GetMessageFromDBUseCase
import com.dino.message.chatfeature.domain.usecase.GetReceiverPublicKeyUseCase
import com.dino.message.chatfeature.domain.usecase.SaveMessageInDbUseCase
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

    private val _socketDebugEvents = MutableStateFlow<List<String>>(emptyList())
    val socketDebugEvents: StateFlow<List<String>> = _socketDebugEvents.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isPeerOnline = MutableStateFlow(false)
    val isPeerOnline: StateFlow<Boolean> = _isPeerOnline.asStateFlow()

    private var getPublicKey = false
    private var isSocketConnecting = false
    private var hasSocketConnectionStarted = false

    init {
        loadMessages()
        prepareEncryptionSession()
    }

    private fun pushSocketDebugLog(message: String) {
        val now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) java.time.LocalTime.now().toString() else "time"
        val updated = (_socketDebugEvents.value + "[$now] $message").takeLast(40)
        _socketDebugEvents.value = updated
    }

    private fun prepareEncryptionSession() = viewModelScope.launch {
        pushSocketDebugLog("HTTP get public key for $chatId")
        getReceiverPublicKey()
    }

    private suspend fun getReceiverPublicKey(): Boolean {
        var isPublicKeyLoaded = false

        getReceiverPublicKeyUseCase.execute(chatId).collect { res ->
            when (res) {
                is Resource.Success -> {
                    val publicKey = res.data.public_key
                    if (publicKey.isNullOrBlank()) {
                        Log.e("ChatViewModel", "getReceiverPublicKey: empty public key for chatId=$chatId")
                        getPublicKey = false
                        showRefreshing()
                        return@collect
                    }

                    receiverPublicKey.value = publicKey
                    pushSocketDebugLog("HTTP public key loaded for $chatId")
                    Log.d("ChatViewModel", "getReceiverPublicKey: public key loaded for chatId=$chatId")
                    getPublicKey = false
                    showRefreshing()
                    isPublicKeyLoaded = true
                }

                is Resource.Loading -> {
                    getPublicKey = true
                    showRefreshing()
                }

                is Resource.Error -> {
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
        _messages.clear()
        getMessageFromDBUseCase.execute().collect {
            _messages.addAll(it)
        }
    }

    fun saveMessage(message: MessageEntity) = viewModelScope.launch {
        Log.d("ChatViewModel", "saveMessage: senderOrReceiver=${message.senderOrReceiver}, timestamp=${message.timestamp}")
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
            Log.d("ChatViewModel", "decryptAndSaveMessage: decrypt success for sender=$sender timestamp=$timestamp")

            saveMessage(
                MessageEntity(
                    senderOrReceiver = sender,
                    content = decryptedMsg,
                    timestamp = timestamp
                )
            )
        } catch (e: Exception) {
            Log.e("ChatViewModel", "decryptAndSaveMessage: decrypt failed for sender=$sender", e)
        }
    }
}
