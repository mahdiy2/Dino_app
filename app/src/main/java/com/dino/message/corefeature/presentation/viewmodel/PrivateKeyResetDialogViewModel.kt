package com.dino.message.corefeature.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.authfeature.domain.usecase.RotatePublicKeyUseCase
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.crypto.MessageCrypto
import com.dino.message.corefeature.presentation.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrivateKeyResetDialogViewModel constructor(
    app: Context,
    private val rotatePublicKeyUseCase: RotatePublicKeyUseCase,
    private val sPrefManager: SPrefManager,
    private val messageCrypto: MessageCrypto,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun dismissDialog() {
        navigateUp(viewModelScope)
    }

    fun confirmReset() {
        if (_isSubmitting.value) return
        _errorMessage.value = null

        val keyPair = messageCrypto.generateRsaKeyPair()
        val publicKeyBase64 = keyPair.publicKeyBase64
        val privateKeyBase64 = keyPair.privateKeyBase64

        viewModelScope.launch {
            rotatePublicKeyUseCase.execute(publicKeyBase64).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _isSubmitting.value = true
                    }

                    is Resource.Success -> {
                        _isSubmitting.value = false
                        sPrefManager.savePrivateKey(privateKeyBase64)
                        sPrefManager.savePublicKey(publicKeyBase64)
                        dismissDialog()
                    }

                    is Resource.Error -> {
                        _isSubmitting.value = false
                        _errorMessage.value = result.message.asString(getApplication())
                    }
                }
            }
        }
    }
}
