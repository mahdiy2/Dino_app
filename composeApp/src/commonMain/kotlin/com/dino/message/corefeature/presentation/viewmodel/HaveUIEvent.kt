package com.dino.message.corefeature.presentation.viewmodel

import com.dino.message.corefeature.domain.model.CustomMessageDialogData
import com.dino.message.corefeature.domain.model.ImageDialogData
import com.dino.message.corefeature.presentation.navigation.AppRoute
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.UIEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

interface HaveUIEvent {
    val mutableUIEventFlow: MutableSharedFlow<UIEvent>
    val uIEventFlow: SharedFlow<UIEvent>

    fun navigateToDestination(
        route: AppRoute,
        scope: CoroutineScope
    ): Job

    fun navigateWithPopBackStackToDestination(
        route: AppRoute,
        scope: CoroutineScope,
        level: PopBackStackLevel
    ): Job

    fun navigateUp(scope: CoroutineScope): Job

    fun showCustomMessageDialog(
        customMessageDialogData: CustomMessageDialogData,
        scope: CoroutineScope
    ): Job

    fun showImageInDialog(
        imageDialogData: ImageDialogData,
        scope: CoroutineScope
    ): Job
}
