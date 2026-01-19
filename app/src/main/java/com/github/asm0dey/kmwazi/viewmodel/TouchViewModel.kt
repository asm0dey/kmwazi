package com.github.asm0dey.kmwazi.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.asm0dey.kmwazi.di.ServiceLocator
import com.github.asm0dey.kmwazi.domain.CountdownController
import com.github.asm0dey.kmwazi.domain.Mode
import com.github.asm0dey.kmwazi.domain.Result
import com.github.asm0dey.kmwazi.domain.ResultEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TouchViewModel(
    private val resultEngine: ResultEngine = ServiceLocator.resultEngine,
    scope: CoroutineScope? = null,
) : ViewModel() {
    private val countdownController = CountdownController(
        scope = scope ?: viewModelScope,
        tickMs = TICK_MS,
    )
    private val _activePoints = MutableStateFlow<Map<Long, Offset>>(emptyMap())
    val activePoints: StateFlow<Map<Long, Offset>> get() = _activePoints

    val remainingMs: StateFlow<Long?> get() = countdownController.remainingMs

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
                countdownController.cancel()
            } else {
                restartTimer()
            }
        }
    }

    fun reset() {
        countdownController.cancel()
        _inputLocked.value = false
        _snapshot.value = null
        _activePoints.value = emptyMap()
        _result.value = null
    }

    private fun restartTimer() {
        val total = _decisionTimeoutMs.value
        countdownController.start(total) {
            // Expired - callback runs when countdown finishes
            val snap = _activePoints.value.toMap()
            if (snap.isNotEmpty()) {
                _snapshot.value = snap
                _inputLocked.value = true
                computeResult(snap)
            }
        }
    }

    private fun computeResult(snap: Map<Long, Offset>) {
        val ids = snap.keys.toList()
        _result.value =
            when (val m = _mode.value) {
                is Mode.ChooseOne -> Result.One(resultEngine.chooseOne(ids))
                is Mode.SplitIntoGroups -> Result.Groups(resultEngine.splitIntoGroups(ids, m.groupSize))
                is Mode.DefineOrder -> Result.Order(resultEngine.defineOrder(ids))
            }
    }

    companion object {
        private const val TICK_MS = 100L

        // Long-press duration to reset when result is shown
        const val LONG_PRESS_RESET_MS = 500L
    }
}
