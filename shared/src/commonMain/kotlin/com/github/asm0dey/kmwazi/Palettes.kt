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

package com.github.asm0dey.kmwazi

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.github.asm0dey.kmwazi.resources.Res
import com.github.asm0dey.kmwazi.resources.palette_colorblind
import com.github.asm0dey.kmwazi.resources.palette_lucid
import com.github.asm0dey.kmwazi.resources.palette_pastel
import com.github.asm0dey.kmwazi.resources.palette_vibrant
import org.jetbrains.compose.resources.StringResource

data class Palette(
    val id: String,
    val name: StringResource,
    val colors: List<Color>,
) {
    fun color(index: Int): Color = colors[index.mod(colors.size)]
}

object Palettes {
    val Vibrant =
        Palette(
            "vibrant",
            Res.string.palette_vibrant,
            listOf(
                Color(0xFFFF0000),
                Color(0xFFFF8000),
                Color(0xFFFFFF00),
                Color(0xFF00FF00),
                Color(0xFF00FFFF),
                Color(0xFF0000FF),
                Color(0xFFFF00FF),
                Color(0xFF8000FF),
                Color(0xFF00FF80),
                Color(0xFFFF007F),
            ),
        )
    val Pastel =
        Palette(
            "pastel",
            Res.string.palette_pastel,
            listOf(
                Color(0xFFFFB3BA),
                Color(0xFFFFDFBA),
                Color(0xFFFFFFBA),
                Color(0xFFBAFFC9),
                Color(0xFFBAE1FF),
                Color(0xFFE0BBE4),
                Color(0xFFFFC4E1),
                Color(0xFFBFFCC6),
                Color(0xFFD4F0F0),
                Color(0xFFFFEECC),
            ),
        )
    val Lucid =
        Palette(
            "lucid",
            Res.string.palette_lucid,
            listOf(
                Color(0xFF2E5BFF),
                Color(0xFF8C52FF),
                Color(0xFFFF2E63),
                Color(0xFF08D9D6),
                Color(0xFFFFDE7D),
                Color(0xFFF9A828),
                Color(0xFF4E9F3D),
                Color(0xFF950101),
                Color(0xFFEEEEEE),
                Color(0xFF005A8D),
            ),
        )
    val Colorblind =
        Palette(
            "colorblind",
            Res.string.palette_colorblind,
            listOf(
                Color(0xFFE69F00),
                Color(0xFF56B4E9),
                Color(0xFF009E73),
                Color(0xFFF0E442),
                Color(0xFF0072B2),
                Color(0xFFD55E00),
                Color(0xFFCC79A7),
                Color(0xFF999999),
                Color(0xFFFFFFFF),
            ),
        )
    val All = listOf(Vibrant, Pastel, Lucid, Colorblind)
}

// WCAG 2 contrast ratio, 1..21.
fun contrast(
    a: Color,
    b: Color,
): Float {
    val hi = maxOf(a.luminance(), b.luminance())
    val lo = minOf(a.luminance(), b.luminance())
    return (hi + 0.05f) / (lo + 0.05f)
}

fun labelColor(background: Color): Color =
    if (contrast(background, Color.Black) >= contrast(background, Color.White)) Color.Black else Color.White
