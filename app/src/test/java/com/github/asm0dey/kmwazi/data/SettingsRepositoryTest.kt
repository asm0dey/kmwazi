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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.github.asm0dey.kmwazi.domain.Mode
import com.github.asm0dey.kmwazi.ui.Palettes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tmpFolder.newFolder(), "test.preferences_pb") }
        )
        repository = SettingsRepository(testDataStore)
    }

    @Test
    fun `savePalette should update paletteFlow`() = testScope.runTest {
        repository.savePalette(Palettes.Pastel)
        val palette = repository.paletteFlow().first()
        assertEquals(Palettes.Pastel, palette)
    }

    @Test
    fun `saveMode ChooseOne should update modeFlow`() = testScope.runTest {
        repository.saveMode(Mode.ChooseOne)
        val mode = repository.modeFlow().first()
        assertEquals(Mode.ChooseOne, mode)
    }

    @Test
    fun `saveMode SplitIntoGroups should update modeFlow with groupSize`() = testScope.runTest {
        val expectedSize = 4
        repository.saveMode(Mode.SplitIntoGroups(expectedSize))
        val mode = repository.modeFlow().first()
        assertEquals(Mode.SplitIntoGroups(expectedSize), mode)
    }

    @Test
    fun `saveDecisionTimeoutSeconds should update decisionTimeoutSecondsFlow`() = testScope.runTest {
        val expectedTimeout = 7
        repository.saveDecisionTimeoutSeconds(expectedTimeout)
        val timeout = repository.decisionTimeoutSecondsFlow().first()
        assertEquals(expectedTimeout, timeout)
    }

    @Test
    fun `decisionTimeoutSeconds should be clamped`() = testScope.runTest {
        repository.saveDecisionTimeoutSeconds(15) // Max is 10
        val timeout = repository.decisionTimeoutSecondsFlow().first()
        assertEquals(10, timeout)

        repository.saveDecisionTimeoutSeconds(0) // Min is 1
        val timeout2 = repository.decisionTimeoutSecondsFlow().first()
        assertEquals(1, timeout2)
    }
}
