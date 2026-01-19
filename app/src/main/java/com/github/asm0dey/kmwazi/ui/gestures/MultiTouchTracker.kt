package com.github.asm0dey.kmwazi.ui.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp

/**
 * Helper to track multiple pointers and manage active points map.
 * Provides hooks for when fingers are added or removed.
 */
suspend fun PointerInputScope.trackMultiTouch(
    points: MutableMap<Long, Offset>,
    onChanged: (Map<Long, Offset>) -> Unit,
    onFingerAdded: (() -> Unit)? = null,
    onFingerRemoved: (() -> Unit)? = null,
) {
    awaitEachGesture {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val changes = event.changes
            if (changes.isEmpty()) break

            changes.forEach { change ->
                val id = change.id.value
                if (change.changedToDown()) {
                    points[id] = change.position
                    onFingerAdded?.invoke()
                } else if (change.changedToUp()) {
                    if (points.remove(id) != null) {
                        onFingerRemoved?.invoke()
                    }
                } else if (change.pressed) {
                    points[id] = change.position
                }
            }
            onChanged(points.toMap())
        }
    }
}
