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

package com.github.asm0dey.kmwazi.viewmodel

import androidx.compose.ui.geometry.Offset
import com.github.asm0dey.kmwazi.FakeRandomProvider
import com.github.asm0dey.kmwazi.domain.ResultEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TouchViewModelTest {

    private val resultEngine = ResultEngine(FakeRandomProvider())

    @Test
    fun `test results stay after all fingers removed`() = runTest {
        val testScope = TestScope(testScheduler)
        val vm = TouchViewModel(resultEngine, testScope)

        // 1. Place fingers
        val fingers = mapOf(1L to Offset(0f, 0f), 2L to Offset(100f, 100f))
        vm.updateActive(fingers)

        // 2. Wait for countdown
        advanceTimeBy(4000) // Default timeout is 3000ms

        assertNotNull(vm.result.value)
        assertTrue(vm.inputLocked.value)

        // 3. Remove one finger - result should stay
        val oneFingerLeft = mapOf(1L to Offset(0f, 0f))
        vm.updateActive(oneFingerLeft)
        assertNotNull(vm.result.value)
        assertTrue(vm.inputLocked.value)

        // 4. Remove all fingers - result should still stay
        vm.updateActive(emptyMap())
        assertNotNull(vm.result.value)
        assertTrue(vm.inputLocked.value)

        // 5. Place new fingers - result should be cleared, new countdown starts
        vm.updateActive(mapOf(3L to Offset(200f, 200f)))
        assertNull(vm.result.value)
        assertEquals(false, vm.inputLocked.value)
        assertNotNull(vm.remainingMs.value)
    }

    @Test
    fun `test countdown restarts only after all fingers were removed and new ones placed`() = runTest {
        val testScope = TestScope(testScheduler)
        val vm = TouchViewModel(resultEngine, testScope)

        // 1. Place fingers and wait for result
        vm.updateActive(mapOf(1L to Offset(0f, 0f)))
        advanceTimeBy(4000)
        assertNotNull(vm.result.value)

        // 2. Keep holding and add another finger - should not restart timer or change result
        vm.updateActive(mapOf(1L to Offset(0f, 0f), 2L to Offset(100f, 100f)))
        advanceTimeBy(2000)
        assertEquals(0L, vm.remainingMs.value)

        // 3. Remove all fingers - result remains
        vm.updateActive(emptyMap())
        assertNotNull(vm.result.value)

        // 4. Place fingers again - should start a new countdown
        vm.updateActive(mapOf(3L to Offset(200f, 200f)))
        assertNull(vm.result.value)
        assertNotNull(vm.remainingMs.value)
    }
}
