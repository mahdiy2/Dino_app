package com.dino.message.corefeature.data.session

import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.session.SessionProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SPrefSessionProvider @Inject constructor(
    private val sPrefManager: SPrefManager
) : SessionProvider {
    override fun hasActiveSession(): Boolean {
        val token = sPrefManager.getToken()
        return sPrefManager.getUserLogIn() && token.isNotBlank()
    }
}
