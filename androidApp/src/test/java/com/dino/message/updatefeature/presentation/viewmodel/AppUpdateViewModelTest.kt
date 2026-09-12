package com.dino.message.updatefeature.presentation.viewmodel

import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.data.spref.SecureKeyValueStore
import com.dino.message.corefeature.presentation.navigation.AppRoute
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.UIEvent
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
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateViewModelTest {
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
    fun continueWithoutUpdate_routesToThreadWhenLoggedIn() = runTest {
        val viewModel = AppUpdateViewModel(
            uiEventBus = UiEventBus(),
            sPrefManager = SPrefManager(FakeSecureKeyValueStore().apply {
                putBoolean("USER_LOG_IN", true)
            }),
        )
        try {
            val emitted = async(start = CoroutineStart.UNDISPATCHED) {
                viewModel.uIEventFlow.first()
            }

            viewModel.continueWithoutUpdate()

            assertEquals(
                UIEvent.NavigateWithPopBackStack(AppRoute.Thread, PopBackStackLevel.ALL),
                emitted.await(),
            )
        } finally {
            viewModel.clear()
        }
    }

    private class FakeSecureKeyValueStore : SecureKeyValueStore {
        private val strings = mutableMapOf<String, String?>()
        private val booleans = mutableMapOf<String, Boolean>()
        private val longs = mutableMapOf<String, Long>()

        override fun putString(key: String, value: String) {
            strings[key] = value
        }

        override fun getString(key: String, defaultValue: String?): String? =
            strings[key] ?: defaultValue

        override fun putBoolean(key: String, value: Boolean) {
            booleans[key] = value
        }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
            booleans[key] ?: defaultValue

        override fun putLong(key: String, value: Long) {
            longs[key] = value
        }

        override fun getLong(key: String, defaultValue: Long): Long =
            longs[key] ?: defaultValue

        override fun remove(key: String): SecureKeyValueStore {
            strings.remove(key)
            booleans.remove(key)
            longs.remove(key)
            return this
        }

        override fun clear() {
            strings.clear()
            booleans.clear()
            longs.clear()
        }

        override fun apply() = Unit
    }
}
