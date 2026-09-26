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

import kotlin.random.Random

class Draw(
    private val random: Random,
) {
    fun chooseOne(ids: List<Long>): Long = ids.random(random)

    fun groups(
        ids: List<Long>,
        size: Int,
    ): List<List<Long>> = ids.shuffled(random).chunked(size)

    fun order(ids: List<Long>): List<Long> = ids.shuffled(random)

    fun draw(
        mode: Mode,
        ids: List<Long>,
    ): Result =
        when (mode) {
            Mode.ChooseOne -> Result.One(chooseOne(ids))
            is Mode.Groups -> Result.Groups(groups(ids, mode.size))
            Mode.Order -> Result.Order(order(ids))
        }
}
