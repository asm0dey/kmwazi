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

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.asm0dey.kmwazi.round.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

val TIMEOUTS = 1..10

data class Prefs(
    val palette: Palette = Palettes.Vibrant,
    val mode: Mode = Mode.ChooseOne,
    val timeoutSec: Int = 3,
    val groupSize: Int = 2,
)

// Keys and values must stay byte-compatible with 1.3.0 so upgrades keep settings.
private val MODE = stringPreferencesKey("mode")
private val GROUP_SIZE = intPreferencesKey("group_size")
private val PALETTE = stringPreferencesKey("palette_name")
private val TIMEOUT = intPreferencesKey("decision_timeout_sec")

class Settings(
    private val store: DataStore<Preferences>,
) {
    constructor(path: String) : this(
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { path.toPath() },
        ),
    )

    val prefs: Flow<Prefs> = store.data.map { it.toPrefs() }

    suspend fun setPalette(palette: Palette) {
        store.edit { it[PALETTE] = palette.id }
    }

    suspend fun setMode(mode: Mode) {
        store.edit {
            when (mode) {
                Mode.ChooseOne -> it[MODE] = "ChooseOne"
                Mode.Order -> it[MODE] = "DefineOrder"
                is Mode.Groups -> {
                    it[MODE] = "groups"
                    it[GROUP_SIZE] = mode.size
                }
            }
        }
    }

    suspend fun setTimeout(seconds: Int) {
        store.edit { it[TIMEOUT] = seconds.coerceIn(TIMEOUTS) }
    }
}

private fun Preferences.toPrefs(): Prefs {
    val groupSize = (this[GROUP_SIZE] ?: 2).coerceIn(Mode.Groups.SIZES)
    return Prefs(
        palette = Palettes.All.find { it.id == this[PALETTE] } ?: Palettes.Vibrant,
        mode =
            when (this[MODE]) {
                "DefineOrder" -> Mode.Order
                "groups" -> Mode.Groups(groupSize)
                else -> Mode.ChooseOne
            },
        timeoutSec = (this[TIMEOUT] ?: 3).coerceIn(TIMEOUTS),
        groupSize = groupSize,
    )
}
