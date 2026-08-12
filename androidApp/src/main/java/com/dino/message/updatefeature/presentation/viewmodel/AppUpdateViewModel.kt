package com.dino.message.updatefeature.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.navigation.AppRoute

class AppUpdateViewModel constructor(
    app: Context,
    private val sPrefManager: SPrefManager,
) : AndroidViewModel(app as Application),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    fun continueWithoutUpdate() {
        val userLogIn = sPrefManager.getUserLogIn()

        if (userLogIn) {
            navigateWithPopBackStackToDestination(
                route = AppRoute.Thread,
                scope = viewModelScope,
                level = PopBackStackLevel.ALL,
            )
        } else if (sPrefManager.hasPendingRegistration()) {
            navigateWithPopBackStackToDestination(
                route = AppRoute.PrivateKeyAlert,
                scope = viewModelScope,
                level = PopBackStackLevel.ALL,
            )
        } else {
            navigateWithPopBackStackToDestination(
                route = AppRoute.Register,
                scope = viewModelScope,
                level = PopBackStackLevel.ALL,
            )
        }
    }
}
