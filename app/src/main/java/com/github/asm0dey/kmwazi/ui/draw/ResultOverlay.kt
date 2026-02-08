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

package com.github.asm0dey.kmwazi.ui.draw

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.github.asm0dey.kmwazi.domain.Result
import com.github.asm0dey.kmwazi.ui.Palette
import kotlin.math.hypot

@Composable
fun ResultOverlay(
    show: Boolean,
    result: Result?,
    progress: Animatable<Float, AnimationVector1D>,
    snapshot: Map<Long, Offset>?,
    fingerColors: Map<Long, Color>,
    palette: Palette,
    modifier: Modifier = Modifier,
) {
    if (!show || result == null) return

    val winnerIdOverlay = (result as? Result.One)?.winnerId
    val winnerPos = winnerIdOverlay?.let { id -> snapshot?.get(id) }

    Canvas(modifier = modifier.fillMaxSize()) {
        when (result) {
            is Result.One -> {
                val center = winnerPos ?: Offset(this.size.width / 2f, this.size.height / 2f)
                val maxRadius = hypot(this.size.width.toDouble(), this.size.height.toDouble()).toFloat()
                val r = maxRadius * progress.value
                val color = fingerColors[winnerIdOverlay] ?: palette.colors[(winnerIdOverlay ?: 0L).toInt() % palette.colors.size]
                drawCircle(color = color.copy(alpha = 0.5f), radius = r, center = center)
            }
            is Result.Order -> {
                val firstId = result.order.firstOrNull()
                val center = firstId?.let { fid -> snapshot?.get(fid) } ?: Offset(this.size.width / 2f, this.size.height / 2f)
                val maxRadius = hypot(this.size.width.toDouble(), this.size.height.toDouble()).toFloat()
                val r = maxRadius * progress.value
                val color = firstId?.let { fid -> fingerColors[fid] ?: palette.colors[fid.toInt() % palette.colors.size] } ?: palette.colors.firstOrNull() ?: Color(0xFF2196F3)
                drawCircle(color = color.copy(alpha = 0.5f), radius = r, center = center)
            }
            is Result.Groups -> {
                if (progress.value < 1f) {
                    val h = this.size.height * progress.value
                    val color = palette.colors.firstOrNull() ?: Color.Gray
                    drawRect(color = color.copy(alpha = 0.5f), size = androidx.compose.ui.geometry.Size(this.size.width, h))
                }
            }
        }
    }
}
