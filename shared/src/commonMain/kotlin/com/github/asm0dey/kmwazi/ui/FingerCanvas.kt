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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.Palette
import com.github.asm0dey.kmwazi.labelColor
import com.github.asm0dey.kmwazi.round.Finger
import com.github.asm0dey.kmwazi.round.Mode
import com.github.asm0dey.kmwazi.round.Result
import com.github.asm0dey.kmwazi.round.RoundState
import kotlin.math.hypot

private val RADIUS = 40.dp

@Composable
fun FingerCanvas(
    state: RoundState,
    palette: Palette,
    pulse: Float,
    grow: Float,
    fade: Float,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val result = state.outcome?.result
    val fingers = state.outcome?.snapshot ?: state.fingers
    // Order: position in order. Groups: group number. Both 1-based.
    val labels =
        remember(result) {
            when (result) {
                is Result.Order -> result.order.withIndex().associate { (i, id) -> id to i + 1 }
                is Result.Groups ->
                    result.groups
                        .withIndex()
                        .flatMap { (g, ids) -> ids.map { it to g + 1 } }
                        .toMap()
                else -> emptyMap()
            }
        }
    Canvas(modifier.fillMaxSize()) {
        val radius = RADIUS.toPx() * pulse
        fingers.forEach { (id, finger) ->
            val color = circleColor(id, finger, state.mode, result, labels, palette)
            val center = Offset(finger.pos.x, finger.pos.y)
            drawCircle(color, radius, center)
            labels[id]?.let { n ->
                val text =
                    measurer.measure(
                        n.toString(),
                        TextStyle(color = labelColor(color), fontSize = (radius * 0.6f).toSp()),
                    )
                drawText(text, topLeft = center - Offset(text.size.width / 2f, text.size.height / 2f))
            }
        }
        if (result != null && fade > 0f) drawOverlay(result, fingers, palette, grow, 0.5f * fade)
    }
}

internal fun circleColor(
    id: Long,
    finger: Finger,
    mode: Mode,
    result: Result?,
    labels: Map<Long, Int>,
    palette: Palette,
): Color =
    when (result) {
        is Result.Groups -> palette.color(labels.getValue(id) - 1)
        is Result.One -> if (id == result.winner) palette.color(finger.colorIndex) else Color.DarkGray
        is Result.Order -> palette.color(finger.colorIndex)
        null -> if (mode is Mode.Groups) Color.Gray else palette.color(finger.colorIndex)
    }

private fun DrawScope.drawOverlay(
    result: Result,
    fingers: Map<Long, Finger>,
    palette: Palette,
    grow: Float,
    alpha: Float,
) {
    val lead =
        when (result) {
            is Result.One -> result.winner
            is Result.Order -> result.order.first()
            is Result.Groups -> null
        }
    if (lead == null) {
        drawRect(palette.color(0).copy(alpha = alpha), size = Size(size.width, size.height * grow))
    } else {
        val finger = fingers.getValue(lead)
        drawCircle(
            color = palette.color(finger.colorIndex).copy(alpha = alpha),
            radius = hypot(size.width, size.height) * grow,
            center = Offset(finger.pos.x, finger.pos.y),
        )
    }
}
