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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CustomMessageDialogViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
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
