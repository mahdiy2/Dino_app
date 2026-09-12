package com.dino.message.corefeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.navigation.AppRoute
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.UIEvent
import com.dino.message.corefeature.presentation.util.UiEventBus
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HaveUIEventImplTest {
    @Test
    fun navigateToDestinationEmitsTypedRouteCommand() = runTest {
        val uiEvent = HaveUIEventImpl(UiEventBus())
        val emitted = async(start = CoroutineStart.UNDISPATCHED) {
            uiEvent.uIEventFlow.first()
        }

        uiEvent.navigateToDestination(AppRoute.ThreadCommentsSheet(postId = 7L), this).join()

        assertEquals(
            UIEvent.Navigate(AppRoute.ThreadCommentsSheet(postId = 7L)),
            emitted.await(),
        )
    }

    @Test
    fun navigateWithPopBackStackEmitsTypedRouteCommand() = runTest {
        val uiEvent = HaveUIEventImpl(UiEventBus())
        val emitted = async(start = CoroutineStart.UNDISPATCHED) {
            uiEvent.uIEventFlow.first()
        }

        uiEvent.navigateWithPopBackStackToDestination(
            route = AppRoute.Login,
            scope = this,
            level = PopBackStackLevel.ALL,
        ).join()

        assertEquals(
            UIEvent.NavigateWithPopBackStack(AppRoute.Login, PopBackStackLevel.ALL),
            emitted.await(),
        )
    }
}
