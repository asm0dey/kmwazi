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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.asm0dey.kmwazi.domain.Mode
import com.github.asm0dey.kmwazi.ui.Palette
import com.github.asm0dey.kmwazi.ui.Palettes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * DataStore-based implementation of SettingsRepositoryInterface.
 * Stores user preferences for palette, mode, and timeout settings.
 */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepositoryInterface {
    constructor(context: Context) : this(context.dataStore)

    private val KEY_MODE = stringPreferencesKey("mode")
    private val KEY_GROUP_SIZE = intPreferencesKey("group_size")
    private val KEY_PALETTE = stringPreferencesKey("palette_name")
    private val KEY_DECISION_TIMEOUT_SEC = intPreferencesKey("decision_timeout_sec")

    override fun paletteFlow(): Flow<Palette> =
        dataStore.data.map { prefs ->
            when (prefs[KEY_PALETTE]) {
                Palettes.Pastel.id -> Palettes.Pastel
                Palettes.Colorblind.id -> Palettes.Colorblind
                Palettes.Lucid.id -> Palettes.Lucid
                Palettes.Vibrant.id -> Palettes.Vibrant
                else -> Palettes.Vibrant
            }
        }

    override fun modeFlow(): Flow<Mode> =
        dataStore.data.map { prefs ->
            when (prefs[KEY_MODE]) {
                Mode.ChooseOne.toString() -> Mode.ChooseOne
                Mode.DefineOrder.toString() -> Mode.DefineOrder
                "groups" -> {
                    val gs = (prefs[KEY_GROUP_SIZE] ?: 2).coerceIn(1, 9)
                    Mode.SplitIntoGroups(gs)
                }
                else -> Mode.ChooseOne
            }
        }

    override fun decisionTimeoutSecondsFlow(): Flow<Int> =
        dataStore.data.map { prefs ->
            (prefs[KEY_DECISION_TIMEOUT_SEC] ?: 3).coerceIn(1, 10)
        }

    override suspend fun savePalette(palette: Palette) {
        dataStore.edit { prefs ->
            prefs[KEY_PALETTE] = palette.id
        }
    }

    override suspend fun saveMode(mode: Mode) {
        dataStore.edit { prefs ->
            when (mode) {
                is Mode.ChooseOne -> {
                    prefs[KEY_MODE] = mode.toString()
                }
                is Mode.DefineOrder -> {
                    prefs[KEY_MODE] = mode.toString()
                }
                is Mode.SplitIntoGroups -> {
                    prefs[KEY_MODE] = "groups"
                    prefs[KEY_GROUP_SIZE] = mode.groupSize.coerceIn(1, 9)
                }
            }
        }
    }

    override suspend fun saveDecisionTimeoutSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(1, 10)
        dataStore.edit { prefs ->
            prefs[KEY_DECISION_TIMEOUT_SEC] = clamped
        }
    }
}
