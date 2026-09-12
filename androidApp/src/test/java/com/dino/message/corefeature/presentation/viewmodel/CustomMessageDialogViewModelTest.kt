package com.dino.message.corefeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.util.UiEventBus
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomMessageDialogViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun navigateUpEmitsNavigationEvent() = runTest {
        val viewModel = CustomMessageDialogViewModel(UiEventBus())
        try {
            val emitted = async(start = CoroutineStart.UNDISPATCHED) {
                viewModel.uIEventFlow.first()
            }

            viewModel.navigateUp()

            assertTrue(emitted.await() is com.dino.message.corefeature.presentation.util.UIEvent.NavigateUp)
        } finally {
            viewModel.clear()
        }
    }
}
