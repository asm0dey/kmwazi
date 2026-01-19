/*
 * kmwazi
 *
 * Copyright (C) 2025 asm0dey <pavel.finkelshtein+kmwazi@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 */

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
                    if (change.isConsumed) {
                        // If a child (like a button) consumed the touch, we should remove it from our tracking if it was there
                        if (points.remove(id) != null) {
                            listener.onFingerUp(id)
                            longPressJob?.cancel()
                            if (points.isEmpty()) {
                                listener.onAllFingersUp()
                            }
                        }
                        return@forEach
                    }

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
