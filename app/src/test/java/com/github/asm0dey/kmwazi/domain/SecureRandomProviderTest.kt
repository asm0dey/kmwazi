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

import org.junit.Assert.assertEquals
import org.junit.Test

class SecureRandomProviderTest {
    @Test
    fun `shuffle returns a permutation of the input`() {
        val provider = SecureRandomProvider()
        val input = (1L..100L).toList()

        val result = provider.shuffle(input)

        assertEquals("shuffle must preserve every element", input.toSet(), result.toSet())
        assertEquals("shuffle must not change size", input.size, result.size)
    }

    @Test
    fun `nextInt stays within bound`() {
        val provider = SecureRandomProvider()
        repeat(1000) {
            val n = provider.nextInt(10)
            assert(n in 0 until 10) { "nextInt(10) returned $n" }
        }
    }
}
