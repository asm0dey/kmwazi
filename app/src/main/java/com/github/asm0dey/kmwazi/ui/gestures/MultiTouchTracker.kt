package com.github.asm0dey.kmwazi.ui.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Helper to track multiple pointers and manage active points map.
 */
suspend fun PointerInputScope.trackMultiTouch(
    listener: TouchEventListener,
) {
    val points = mutableMapOf<Long, Offset>()
    coroutineScope {
        var longPressJob: kotlinx.coroutines.Job? = null

        awaitEachGesture {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val changes = event.changes
                if (changes.isEmpty()) break

                changes.forEach { change ->
                    val id = change.id.value
                    if (change.changedToDown()) {
                        points[id] = change.position
                        listener.onFingerDown(id, change.position)

                        if (points.size == 1) {
                            longPressJob?.cancel()
                            longPressJob = launch {
                                delay(2000)
                                listener.onLongPress()
                            }
                        } else {
                            longPressJob?.cancel()
                        }
                    } else if (change.changedToUp()) {
                        if (points.remove(id) != null) {
                            listener.onFingerUp(id)
                            longPressJob?.cancel()
                            if (points.isEmpty()) {
                                listener.onAllFingersUp()
                            }
                        }
                    } else if (change.pressed) {
                        val oldPos = points[id]
                        if (oldPos != null && (change.position - oldPos).getDistanceSquared() > 100f) {
                            longPressJob?.cancel()
                        }
                        points[id] = change.position
                        listener.onFingerMove(id, change.position)
                    }
                }
            }
        }
    }
}
