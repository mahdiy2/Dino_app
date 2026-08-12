package com.dino.message.corefeature.domain.startup

import com.dino.message.corefeature.domain.lifecycle.AppLifecycleDispatcher
import com.dino.message.corefeature.domain.lifecycle.AppLifecycleEvent
import com.dino.message.corefeature.domain.lifecycle.AppLifecycleObserver
import org.junit.Assert.assertEquals
import org.junit.Test

class AppStartupCoordinatorTest {

    @Test
    fun initializeCommonState_registersObserverAndDispatchesApplicationStarted() {
        val dispatcher = AppLifecycleDispatcher()
        val events = mutableListOf<AppLifecycleEvent>()
        val observer = AppLifecycleObserver { event ->
            events += event
        }
        val coordinator = AppStartupCoordinator(
            lifecycleDispatcher = dispatcher,
            backgroundCleanupObserver = observer,
        )

        coordinator.initializeCommonState()
        dispatcher.dispatch(AppLifecycleEvent.EnteredBackground)

        assertEquals(
            listOf(
                AppLifecycleEvent.ApplicationStarted,
                AppLifecycleEvent.EnteredBackground,
            ),
            events,
        )
    }

    @Test
    fun initializeCommonState_whenCalledAgain_doesNotRegisterObserverAgain() {
        val dispatcher = AppLifecycleDispatcher()
        val events = mutableListOf<AppLifecycleEvent>()
        val observer = AppLifecycleObserver { event ->
            events += event
        }
        val coordinator = AppStartupCoordinator(
            lifecycleDispatcher = dispatcher,
            backgroundCleanupObserver = observer,
        )

        coordinator.initializeCommonState()
        coordinator.initializeCommonState()
        dispatcher.dispatch(AppLifecycleEvent.EnteredBackground)

        assertEquals(
            listOf(
                AppLifecycleEvent.ApplicationStarted,
                AppLifecycleEvent.EnteredBackground,
            ),
            events,
        )
    }
}
