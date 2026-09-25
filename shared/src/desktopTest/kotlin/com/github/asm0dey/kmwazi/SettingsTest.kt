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

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.asm0dey.kmwazi.round.Mode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import java.io.File

private val MODE = stringPreferencesKey("mode")
private val GROUP_SIZE = intPreferencesKey("group_size")
private val PALETTE = stringPreferencesKey("palette_name")
private val TIMEOUT = intPreferencesKey("decision_timeout_sec")

private suspend fun read(vararg stored: Preferences.Pair<*>) =
    Settings(MemoryStore(preferencesOf(*stored))).prefs.first()

class SettingsTest :
    FunSpec({
        test("defaults when nothing is stored") {
            read() shouldBe Prefs(Palettes.Vibrant, Mode.ChooseOne, timeoutSec = 3, groupSize = 2)
        }

        test("reads exactly what 1.3.0 wrote") {
            read(MODE to "DefineOrder", PALETTE to "pastel", TIMEOUT to 7) shouldBe
                Prefs(Palettes.Pastel, Mode.Order, 7, 2)
            read(MODE to "groups", GROUP_SIZE to 4).mode shouldBe Mode.Groups(4)
            read(MODE to "ChooseOne", GROUP_SIZE to 6) shouldBe Prefs(groupSize = 6)
            read(PALETTE to "lucid").palette shouldBe Palettes.Lucid
            read(PALETTE to "colorblind").palette shouldBe Palettes.Colorblind
        }

        test("unknown and out-of-range values fall back or clamp") {
            read(MODE to "groups", GROUP_SIZE to 1, PALETTE to "neon", TIMEOUT to 42) shouldBe
                Prefs(Palettes.Vibrant, Mode.Groups(2), timeoutSec = 10, groupSize = 2)
            read(GROUP_SIZE to 99, TIMEOUT to 0) shouldBe Prefs(timeoutSec = 1, groupSize = 10)
            read(MODE to "weird").mode shouldBe Mode.ChooseOne
        }

        test("round-trips through a real file and keeps group size after leaving groups") {
            val file =
                File.createTempFile("settings", ".preferences_pb").apply {
                    delete()
                    deleteOnExit()
                }
            val settings = Settings(file.absolutePath)
            settings.setPalette(Palettes.Lucid)
            settings.setMode(Mode.Groups(5))
            settings.setTimeout(9)
            settings.prefs.first() shouldBe Prefs(Palettes.Lucid, Mode.Groups(5), 9, 5)
            settings.setMode(Mode.Order)
            settings.prefs.first() shouldBe Prefs(Palettes.Lucid, Mode.Order, 9, 5)
        }

        test("a corrupted file reads as defaults") {
            val file =
                File.createTempFile("corrupt", ".preferences_pb").apply {
                    writeBytes(byteArrayOf(1, 2, 3, 4, 5))
                    deleteOnExit()
                }
            Settings(file.absolutePath).prefs.first() shouldBe Prefs()
        }
    })
