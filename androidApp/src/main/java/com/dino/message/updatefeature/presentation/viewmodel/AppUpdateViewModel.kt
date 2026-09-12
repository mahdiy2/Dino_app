package com.dino.message.updatefeature.presentation.viewmodel

import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.corefeature.presentation.navigation.AppRoute
import com.dino.message.corefeature.presentation.util.UiEventBus

class AppUpdateViewModel constructor(
    uiEventBus: UiEventBus,
    private val sPrefManager: AuthSessionStore,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    fun continueWithoutUpdate() {
        val userLogIn = sPrefManager.getUserLogIn()

        if (userLogIn) {
            navigateWithPopBackStackToDestination(
                route = AppRoute.Thread,
                scope = scope,
                level = PopBackStackLevel.ALL,
            )
        } else if (sPrefManager.hasPendingRegistration()) {
            navigateWithPopBackStackToDestination(
                route = AppRoute.PrivateKeyAlert,
                scope = scope,
                level = PopBackStackLevel.ALL,
            )
        } else {
            navigateWithPopBackStackToDestination(
                route = AppRoute.Register,
                scope = scope,
                level = PopBackStackLevel.ALL,
            )
        }
    }
}
