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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

class PaletteTest :
    FunSpec({
        test("every palette colour gets a label with at least 4.5:1 contrast") {
            Palettes.All.flatMap { it.colors }.forEach { c ->
                contrast(c, labelColor(c)) shouldBeGreaterThanOrEqual 4.5f
            }
        }

        test("light colours get black labels, dark colours white") {
            labelColor(Color.White) shouldBe Color.Black
            labelColor(Color(0xFFFFDE7D)) shouldBe Color.Black
            labelColor(Color(0xFF191919)) shouldBe Color.White
        }

        test("palette colours wrap past the palette size") {
            Palettes.Vibrant.color(10) shouldBe Palettes.Vibrant.color(0)
            Palettes.Vibrant.color(11) shouldBe Palettes.Vibrant.color(1)
        }

        test("palette ids match what 1.3.0 stored") {
            Palettes.All.map { it.id } shouldBe listOf("vibrant", "pastel", "lucid", "colorblind")
        }
    })
