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

package com.github.asm0dey.kmwazi.data

import com.github.asm0dey.kmwazi.domain.Mode
import com.github.asm0dey.kmwazi.ui.Palette
import kotlinx.coroutines.flow.Flow

/**
 * Interface for settings persistence, abstracted to allow testing with fakes.
 */
interface SettingsRepositoryInterface {
    /**
     * Flow of the current palette selection.
     */
    fun paletteFlow(): Flow<Palette>

    /**
     * Flow of the current mode selection.
     */
    fun modeFlow(): Flow<Mode>

    /**
     * Flow of the decision timeout in seconds (1-10).
     */
    fun decisionTimeoutSecondsFlow(): Flow<Int>

    /**
     * Saves the palette selection.
     */
    suspend fun savePalette(palette: Palette)

    /**
     * Saves the mode selection.
     */
    suspend fun saveMode(mode: Mode)

    /**
     * Saves the decision timeout in seconds.
     */
    suspend fun saveDecisionTimeoutSeconds(seconds: Int)
}
