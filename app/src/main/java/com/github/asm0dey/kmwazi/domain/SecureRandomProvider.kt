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

package com.github.asm0dey.kmwazi.domain

import java.security.SecureRandom

/**
 * Production implementation of RandomProvider using SecureRandom for cryptographically secure randomness.
 */
class SecureRandomProvider : RandomProvider {
    private val rng = SecureRandom()

    override fun nextInt(bound: Int): Int = rng.nextInt(bound)

    override fun <T> shuffle(list: List<T>): List<T> {
        val mutable = list.toMutableList()
        // Fisher-Yates shuffle
        for (i in mutable.indices.reversed()) {
            val j = nextInt(i + 1)
            val temp = mutable[i]
            mutable[i] = mutable[j]
            mutable[j] = temp
        }
        return mutable
    }
}
