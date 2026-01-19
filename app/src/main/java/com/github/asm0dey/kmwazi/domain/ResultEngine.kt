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

/**
 * Pure domain service for computing game results using the Mode pattern.
 * All randomization is delegated to the injected RandomProvider.
 */
class ResultEngine(private val randomProvider: RandomProvider) {

    /**
     * Chooses one ID randomly from the list.
     * @throws IllegalArgumentException if ids is empty
     */
    fun chooseOne(ids: List<Long>): Long {
        require(ids.isNotEmpty()) { "Cannot choose from empty list" }
        val index = randomProvider.nextInt(ids.size)
        return ids[index]
    }

    /**
     * Splits IDs into groups of the specified size.
     * The last group may be smaller if the total doesn't divide evenly.
     * @throws IllegalArgumentException if ids is empty or groupSize <= 0
     */
    fun splitIntoGroups(ids: List<Long>, groupSize: Int): List<List<Long>> {
        require(ids.isNotEmpty()) { "Cannot split empty list" }
        require(groupSize > 0) { "Group size must be positive" }

        val shuffled = randomProvider.shuffle(ids)
        return shuffled.chunked(groupSize)
    }

    /**
     * Returns a shuffled order of all IDs.
     * @throws IllegalArgumentException if ids is empty
     */
    fun defineOrder(ids: List<Long>): List<Long> {
        require(ids.isNotEmpty()) { "Cannot order empty list" }
        return randomProvider.shuffle(ids)
    }
}
