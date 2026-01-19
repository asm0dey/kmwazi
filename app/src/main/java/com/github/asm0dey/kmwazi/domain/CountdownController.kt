package com.github.asm0dey.kmwazi.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Controller for managing countdown timers.
 * Emits remaining time and executes a callback when the countdown expires.
 *
 * @param scope CoroutineScope in which countdown coroutines will run
 * @param tickMs How often to update the remaining time (default 100ms)
 */
class CountdownController(
    private val scope: CoroutineScope,
    private val tickMs: Long = 100L,
) {
    private val _remainingMs = MutableStateFlow<Long?>(null)
    val remainingMs: StateFlow<Long?> = _remainingMs.asStateFlow()

    private var job: Job? = null

    /**
     * Starts a countdown timer.
     *
     * @param durationMs Total duration of the countdown in milliseconds
     * @param onExpire Callback to execute when the countdown reaches zero
     */
    fun start(durationMs: Long, onExpire: () -> Unit) {
        cancel()
        _remainingMs.value = durationMs

        job = scope.launch {
            var remaining = durationMs
            while (remaining > 0) {
                delay(tickMs)
                remaining -= tickMs
                _remainingMs.value = remaining.coerceAtLeast(0)
            }
            _remainingMs.value = 0
            onExpire()
        }
    }

    /**
     * Cancels the current countdown and resets state.
     */
    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = null
    }

    /**
     * Returns true if a countdown is currently active.
     */
    fun isActive(): Boolean = job?.isActive == true
}
