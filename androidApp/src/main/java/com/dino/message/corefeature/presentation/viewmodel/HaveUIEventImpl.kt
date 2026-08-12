package com.dino.message.corefeature.presentation.viewmodel

import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.presentation.navigation.AppRoute
import com.dino.message.corefeature.domain.model.CustomMessageDialogData
import com.dino.message.corefeature.domain.model.ImageDialogData
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.UIEvent
import com.dino.message.corefeature.presentation.util.UiEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch


/**
 * Implementation of the [HaveUIEvent] interface that emits UI events.
 *
 * @param app The [AniBaranKSHDriverApplication] instance used to access the shared UI event flows.
 */
class HaveUIEventImpl(
    uiEventBus: UiEventBus
) : HaveUIEvent {
    constructor(app: DinoOrderApplication) : this(app.uiEventBus)

    override val mutableUIEventFlow: MutableSharedFlow<UIEvent> = uiEventBus.mutableUIEventFlow
    override val uIEventFlow = uiEventBus.uIEventFlow

    override fun navigateWithPopBackStackToDestination(
        route: AppRoute,
        scope: CoroutineScope,
        level: PopBackStackLevel
    ) = scope.launch {
        mutableUIEventFlow.emit(
            UIEvent.NavigateWithPopBackStack(route = route, level = level)
        )
    }

    override fun navigateToDestination(
        route: AppRoute,
        scope: CoroutineScope
    ) = scope.launch {
        mutableUIEventFlow.emit(
            UIEvent.Navigate(route = route)
        )
    }

    override fun navigateUp(scope: CoroutineScope) = scope.launch {
        mutableUIEventFlow.emit(UIEvent.NavigateUp)
    }

    override fun showCustomMessageDialog(
        customMessageDialogData: CustomMessageDialogData,
        scope: CoroutineScope
    ) = scope.launch {
        mutableUIEventFlow.emit(UIEvent.ShowCustomMessageDialog(customMessageDialogData))
    }

    override fun showImageInDialog(
        imageDialogData: ImageDialogData,
        scope: CoroutineScope
    ) = scope.launch {
        mutableUIEventFlow.emit(UIEvent.ShowImageDialog(imageDialogData))
    }
}
