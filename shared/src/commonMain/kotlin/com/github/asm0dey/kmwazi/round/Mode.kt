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

sealed interface Mode {
    data object ChooseOne : Mode

    data class Groups(
        val size: Int,
    ) : Mode {
        init {
            require(size in SIZES) { "group size $size outside $SIZES" }
        }

        companion object {
            val SIZES = 2..10
        }
    }

    data object Order : Mode
}

sealed interface Result {
    data class One(
        val winner: Long,
    ) : Result

    data class Groups(
        val groups: List<List<Long>>,
    ) : Result

    data class Order(
        val order: List<Long>,
    ) : Result
}
