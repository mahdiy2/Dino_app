package com.dino.message.corefeature.data.socket

import com.dino.message.corefeature.domain.lifecycle.AppLifecycleDispatcher
import com.dino.message.corefeature.domain.lifecycle.AppLifecycleEvent
import com.dino.message.corefeature.domain.lifecycle.AppLifecycleObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Host-side bridge for Desktop, iOS, and Wasm. The gateway owns the persisted queue. */
class RealtimeRetryLifecycle(
    private val lifecycle: AppLifecycleDispatcher,
    scope: CoroutineScope,
    gateway: SharedRealtimeGateway,
    suspendInBackground: Boolean,
) : AppLifecycleObserver {
    private enum class Signal { Foreground, Background, NetworkAvailable }
    private val signals = Channel<Signal>(Channel.UNLIMITED)
    private val job = scope.launch {
        var canRun = !suspendInBackground
        for (signal in signals) {
            try {
                when (signal) {
                    Signal.Foreground -> {
                        canRun = true
                        gateway.connect()
                    }
                    Signal.Background -> if (suspendInBackground) {
                        canRun = false
                        gateway.disconnect()
                    }
                    Signal.NetworkAvailable -> if (canRun) gateway.connect()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A later foreground or connectivity event can retry unavailable storage.
            }
        }
    }

    init {
        lifecycle.addObserver(this)
    }

    override fun onAppLifecycleEvent(event: AppLifecycleEvent) {
        when (event) {
            AppLifecycleEvent.ApplicationStarted -> signals.trySend(Signal.Foreground)
            AppLifecycleEvent.EnteredBackground -> signals.trySend(Signal.Background)
            is AppLifecycleEvent.ScreenStarted -> if (event.activeScreenCount == 1) {
                signals.trySend(Signal.Foreground)
            }
            else -> Unit
        }
    }

    fun networkAvailable() {
        signals.trySend(Signal.NetworkAvailable)
    }

    fun close() {
        lifecycle.removeObserver(this)
        signals.close()
        job.cancel()
    }
}
