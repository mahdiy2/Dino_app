package com.dino.message.corefeature.presentation.viewmodel

import com.dino.message.authfeature.domain.usecase.RotatePublicKeyUseCase
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.domain.crypto.MessageCrypto
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.presentation.util.UiEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrivateKeyResetDialogViewModel constructor(
    uiEventBus: UiEventBus,
    private val rotatePublicKeyUseCase: RotatePublicKeyUseCase,
    private val authSessionStore: AuthSessionStore,
    private val messageCrypto: MessageCrypto,
    private val stringResolver: StringResolver,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun dismissDialog() {
        navigateUp(scope)
    }

    fun confirmReset() {
        if (_isSubmitting.value) return
        _errorMessage.value = null

        scope.launch {
            val keyPair = messageCrypto.generateRsaKeyPair()
            val publicKeyBase64 = keyPair.publicKeyBase64
            val privateKeyBase64 = keyPair.privateKeyBase64
            rotatePublicKeyUseCase.execute(publicKeyBase64).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _isSubmitting.value = true
                    }

                    is Resource.Success -> {
                        _isSubmitting.value = false
                        authSessionStore.savePrivateKey(privateKeyBase64)
                        authSessionStore.savePublicKey(publicKeyBase64)
                        dismissDialog()
                    }

                    is Resource.Error -> {
                        _isSubmitting.value = false
                        _errorMessage.value = result.message.resolveWith(stringResolver)
                    }
                }
            }
        }
    }
}
