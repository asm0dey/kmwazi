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

import androidx.compose.ui.graphics.Color
import com.github.asm0dey.kmwazi.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Simple in-memory palette store for the current session
object PaletteRepository {
    private val _current = MutableStateFlow(Palettes.Vibrant)
    val current: StateFlow<Palette> get() = _current

    fun setPalette(palette: Palette) {
        _current.value = palette
    }
}

// Palette model and predefined palettes
data class Palette(val id: String, val nameRes: Int, val colors: List<Color>)

object Palettes {
    val Vibrant =
        Palette(
            id = "vibrant",
            nameRes = R.string.palette_vibrant,
            colors =
                listOf(
                    Color(0xFFFF0000), // Red
                    Color(0xFFFF8000), // Orange
                    Color(0xFFFFFF00), // Yellow
                    Color(0xFF00FF00), // Green
                    Color(0xFF00FFFF), // Cyan
                    Color(0xFF0000FF), // Blue
                    Color(0xFFFF00FF), // Magenta
                    Color(0xFF8000FF), // Violet
                    Color(0xFF00FF80), // Spring Green
                    Color(0xFFFF007F), // Rose
                ),
        )

    val Pastel =
        Palette(
            id = "pastel",
            nameRes = R.string.palette_pastel,
            colors =
                listOf(
                    Color(0xFFFFB3BA), // Pastel Red
                    Color(0xFFFFDFBA), // Pastel Orange
                    Color(0xFFFFFFBA), // Pastel Yellow
                    Color(0xFFBAFFC9), // Pastel Green
                    Color(0xFFBAE1FF), // Pastel Blue
                    Color(0xFFE0BBE4), // Pastel Purple
                    Color(0xFFFFC4E1), // Pastel Pink
                    Color(0xFFBFFCC6), // Pastel Mint
                    Color(0xFFD4F0F0), // Pastel Cyan
                    Color(0xFFFFEECC), // Pastel Peach
                ),
        )

    val Lucid =
        Palette(
            id = "lucid",
            nameRes = R.string.palette_lucid,
            colors =
                listOf(
                    Color(0xFF2E5BFF), // Lucid Blue
                    Color(0xFF8C52FF), // Lucid Purple
                    Color(0xFFFF2E63), // Lucid Pink/Red
                    Color(0xFF08D9D6), // Lucid Teal
                    Color(0xFFFFDE7D), // Lucid Yellow
                    Color(0xFFF9A828), // Lucid Orange
                    Color(0xFF4E9F3D), // Lucid Green
                    Color(0xFF950101), // Lucid Deep Red
                    Color(0xFF191919), // Lucid Blackish
                    Color(0xFF005A8D), // Lucid Navy
                ),
        )

    val Colorblind =
        Palette(
            id = "colorblind",
            nameRes = R.string.palette_colorblind,
            colors =
                listOf(
                    Color(0xFF000000), // Black
                    Color(0xFFE69F00), // Orange
                    Color(0xFF56B4E9), // Sky Blue
                    Color(0xFF009E73), // Bluish Green
                    Color(0xFFF0E442), // Yellow
                    Color(0xFF0072B2), // Blue
                    Color(0xFFD55E00), // Vermillion
                    Color(0xFFCC79A7), // Reddish Purple
                    Color(0xFF999999), // Grey
                    Color(0xFFFFFFFF), // White
                ),
        )

    val All = listOf(Vibrant, Pastel, Lucid, Colorblind)
}
