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

// Reports the full set of pressed pointers whenever it changes. Pointers already consumed by a
// child (a button) are dropped, so tapping controls never counts as a finger.
fun Modifier.multiTouch(onChange: (Map<Long, Point>) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = linkedMapOf<Long, Point>()
            do {
                val event = awaitPointerEvent()
                val before = down.toMap()
                event.changes.forEach { c ->
                    if (c.isConsumed || !c.pressed) {
                        down.remove(c.id.value)
                    } else {
                        down[c.id.value] = Point(c.position.x, c.position.y)
                    }
                }
                if (down != before) onChange(down.toMap())
            } while (event.changes.any { it.pressed })
            if (down.isNotEmpty()) onChange(emptyMap())
        }
    }
