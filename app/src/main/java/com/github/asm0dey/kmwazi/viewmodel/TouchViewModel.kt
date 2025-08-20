package com.github.asm0dey.kmwazi.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.asm0dey.kmwazi.domain.Mode
import com.github.asm0dey.kmwazi.domain.Result
import com.github.asm0dey.kmwazi.domain.SecureRandomUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TouchViewModel : ViewModel() {
    private val _activePoints = MutableStateFlow<Map<Long, Offset>>(emptyMap())
    val activePoints: StateFlow<Map<Long, Offset>> get() = _activePoints

    private val _remainingMs = MutableStateFlow<Long?>(null)
    val remainingMs: StateFlow<Long?> get() = _remainingMs

    private val _inputLocked = MutableStateFlow(false)
    val inputLocked: StateFlow<Boolean> get() = _inputLocked

    private val _snapshot = MutableStateFlow<Map<Long, Offset>?>(null)
    val snapshot: StateFlow<Map<Long, Offset>?> get() = _snapshot

    private val _mode = MutableStateFlow<Mode>(Mode.ChooseOne)
    val mode: StateFlow<Mode> get() = _mode

    private val _result = MutableStateFlow<Result?>(null)
    val result: StateFlow<Result?> get() = _result

    private val _decisionTimeoutMs = MutableStateFlow(3000L)
    val decisionTimeoutMs: StateFlow<Long> get() = _decisionTimeoutMs

    private var timerJob: Job? = null

    fun setMode(newMode: Mode) {
        _mode.value = newMode
    }

    fun setDecisionTimeoutSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(1, 10)
        _decisionTimeoutMs.value = clamped * 1000L
    }

    fun updateActive(points: Map<Long, Offset>) {
        // If input is locked, ignore live updates
        if (_inputLocked.value) return

        val prevKeys = _activePoints.value.keys
        val newKeys = points.keys
        _activePoints.value = points

        if (newKeys != prevKeys) {
            // Touch set changed
            if (newKeys.isEmpty()) {
                cancelTimer()
            } else {
                restartTimer()
            }
        }
    }

    fun reset() {
        cancelTimer()
        _inputLocked.value = false
        _snapshot.value = null
        _activePoints.value = emptyMap()
        _result.value = null
    }

    private fun restartTimer() {
        cancelTimer()
        val total = _decisionTimeoutMs.value
        _remainingMs.value = total
        timerJob = viewModelScope.launch {
            var remaining = total
            while (remaining > 0) {
                delay(TICK_MS)
                remaining -= TICK_MS
                _remainingMs.value = remaining
                // If touches changed to zero during countdown, bail
                if (_activePoints.value.isEmpty()) {
                    cancelTimer()
                    return@launch
                }
            }
            // Expired
            _remainingMs.value = 0
            val snap = _activePoints.value.toMap()
            _snapshot.value = snap
            _inputLocked.value = true
            computeResult(snap)
        }
    }

    private fun computeResult(snap: Map<Long, Offset>) {
        val ids = snap.keys.toList()
        _result.value = when (val m = _mode.value) {
            is Mode.ChooseOne -> Result.One(SecureRandomUtils.chooseOne(ids))
            is Mode.SplitIntoGroups -> Result.Groups(SecureRandomUtils.splitIntoGroups(ids, m.groupSize))
            is Mode.DefineOrder -> Result.Order(SecureRandomUtils.defineOrder(ids))
        }
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingMs.value = null
    }

    companion object {
        private const val TICK_MS = 100L
        // Long-press duration to reset when result is shown
        const val LONG_PRESS_RESET_MS = 500L
    }
}