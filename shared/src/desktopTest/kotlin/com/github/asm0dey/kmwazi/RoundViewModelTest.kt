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

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.asm0dey.kmwazi.round.Event
import com.github.asm0dey.kmwazi.round.Mode
import com.github.asm0dey.kmwazi.round.Point
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.random.Random

private fun fingers(vararg ids: Long) = Event.FingersChanged(ids.associateWith { Point(it.toFloat(), 0f) })

@OptIn(ExperimentalCoroutinesApi::class)
class RoundViewModelTest :
    FunSpec({
        val main = StandardTestDispatcher()
        beforeTest { Dispatchers.setMain(main) }
        afterTest { Dispatchers.resetMain() }

        test("deals once the stored timeout has passed") {
            runTest(main) {
                val vm =
                    RoundViewModel(
                        Settings(MemoryStore(preferencesOf(intPreferencesKey("decision_timeout_sec") to 5))),
                        Random(0),
                    )
                runCurrent()
                vm.send(fingers(1, 2))
                advanceTimeBy(4_999)
                vm.state.value.outcome
                    .shouldBeNull()
                advanceTimeBy(2)
                vm.state.value.outcome
                    .shouldNotBeNull()
            }
        }

        test("a new finger restarts the countdown") {
            runTest(main) {
                val vm = RoundViewModel(Settings(MemoryStore()), Random(0))
                runCurrent()
                vm.send(fingers(1))
                advanceTimeBy(2_000)
                vm.send(fingers(1, 2))
                advanceTimeBy(2_999)
                vm.state.value.outcome
                    .shouldBeNull()
                advanceTimeBy(2)
                vm.state.value.outcome
                    .shouldNotBeNull()
            }
        }

        test("restores the saved mode and persists mode changes") {
            runTest(main) {
                val settings = Settings(MemoryStore(preferencesOf(stringPreferencesKey("mode") to "DefineOrder")))
                val vm = RoundViewModel(settings, Random(0))
                runCurrent()
                vm.state.value.mode shouldBe Mode.Order
                vm.setMode(Mode.Groups(4))
                runCurrent()
                vm.state.value.mode shouldBe Mode.Groups(4)
                settings.prefs.first().mode shouldBe Mode.Groups(4)
            }
        }

        test("a finger sent before the settings restore completes is not discarded") {
            runTest(main) {
                val vm = RoundViewModel(Settings(MemoryStore()), Random(0))
                vm.send(fingers(1))
                runCurrent()
                vm.state.value.fingers.keys shouldBe setOf(1L)
            }
        }

        test("a mode change sent before the settings restore completes is not reverted") {
            runTest(main) {
                val settings = Settings(MemoryStore(preferencesOf(stringPreferencesKey("mode") to "DefineOrder")))
                val vm = RoundViewModel(settings, Random(0))
                vm.setMode(Mode.Groups(4))
                runCurrent()
                vm.state.value.mode shouldBe Mode.Groups(4)
            }
        }
    })
