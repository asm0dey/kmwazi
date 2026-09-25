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

package com.github.asm0dey.kmwazi.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.github.asm0dey.kmwazi.round.Point

// Reports the full set of pressed pointers whenever it changes. A pointer whose down (or any
// later) event is consumed by a child (a button) is dropped and stays ignored for the rest of
// its gesture: consumption is per-PointerEvent, so a button only consumes down/up, not moves —
// without sticky tracking, an unconsumed move for that same pointer id would slip through and
// turn a button touch into a phantom finger.
fun Modifier.multiTouch(onChange: (Map<Long, Point>) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = linkedMapOf<Long, Point>()
            val ignored = mutableSetOf<Long>()
            do {
                val event = awaitPointerEvent()
                val before = down.toMap()
                event.changes.forEach { c ->
                    val id = c.id.value
                    when {
                        !c.pressed -> {
                            down.remove(id)
                            ignored.remove(id)
                        }
                        id in ignored -> Unit
                        c.isConsumed -> {
                            ignored.add(id)
                            down.remove(id)
                        }
                        else -> down[id] = Point(c.position.x, c.position.y)
                    }
                }
                if (down != before) onChange(down.toMap())
            } while (event.changes.any { it.pressed })
        }
    }
