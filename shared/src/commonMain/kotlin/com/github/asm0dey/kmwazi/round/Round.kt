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

package com.github.asm0dey.kmwazi.round

data class Point(
    val x: Float,
    val y: Float,
)

data class Finger(
    val pos: Point,
    val colorIndex: Int,
)

data class Outcome(
    val result: Result,
    val snapshot: Map<Long, Finger>,
)

data class RoundState(
    val mode: Mode,
    val fingers: Map<Long, Finger> = emptyMap(),
    val nextColor: Int = 0,
    val armed: Int = 0,
    val outcome: Outcome? = null,
)

sealed interface Event {
    data class FingersChanged(
        val points: Map<Long, Point>,
    ) : Event

    data class Expired(
        val armed: Int,
    ) : Event

    data object Reset : Event

    data class ModeChanged(
        val mode: Mode,
    ) : Event
}

fun reduce(
    state: RoundState,
    event: Event,
    deal: Deal,
): RoundState =
    when (event) {
        is Event.FingersChanged -> onFingers(state, event.points)
        is Event.Expired ->
            if (event.armed != state.armed || state.outcome != null || state.fingers.isEmpty()) {
                state
            } else {
                state.copy(
                    outcome =
                        Outcome(deal.deal(state.mode, state.fingers.keys.toList()), state.fingers),
                )
            }
        Event.Reset -> fresh(state.mode, state)
        is Event.ModeChanged -> fresh(event.mode, state)
    }

// armed + 1 so any countdown already running for the old round is stale.
private fun fresh(
    mode: Mode,
    state: RoundState,
) = RoundState(mode = mode, armed = state.armed + 1)

private fun onFingers(
    state: RoundState,
    points: Map<Long, Point>,
): RoundState {
    if (state.outcome != null) {
        return when {
            points.isEmpty() -> state.copy(fingers = emptyMap())
            state.fingers.isEmpty() -> onFingers(fresh(state.mode, state), points)
            else -> state.copy(fingers = points.mapValues { (_, p) -> Finger(p, 0) })
        }
    }
    var next = state.nextColor
    val fingers =
        points.mapValues { (id, p) -> state.fingers[id]?.copy(pos = p) ?: Finger(p, next++) }
    return when {
        fingers.keys == state.fingers.keys -> state.copy(fingers = fingers)
        fingers.isEmpty() -> state.copy(fingers = fingers, nextColor = 0, armed = state.armed + 1)
        else -> state.copy(fingers = fingers, nextColor = next, armed = state.armed + 1)
    }
}
