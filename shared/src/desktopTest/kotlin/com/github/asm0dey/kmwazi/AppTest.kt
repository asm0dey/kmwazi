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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.random.Random

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class AppTest :
    FunSpec({
        beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
        afterTest { Dispatchers.resetMain() }

        test("home leads to every screen and close returns home") {
            runComposeUiTest {
                val settings = Settings(MemoryStore())
                mainClock.autoAdvance = false
                setContent { App(settings, RoundViewModel(settings, Random(0))) }
                mainClock.advanceTimeBy(100)

                onNodeWithText("Start").performClick()
                mainClock.advanceTimeBy(100)
                onNodeWithText("Mode: Choose One").assertExists()
                onNodeWithContentDescription("Close").performClick()
                mainClock.advanceTimeBy(100)

                onNodeWithText("Settings").performClick()
                mainClock.advanceTimeBy(100)
                onNodeWithText("Select color palette").assertExists()
                onNodeWithText("3s").assertExists()
                onNodeWithContentDescription("Increase timeout").performClick()
                mainClock.advanceTimeBy(100)
                onNodeWithText("4s").assertExists()
                onNodeWithContentDescription("Close").performClick()
                mainClock.advanceTimeBy(100)

                onNodeWithText("Help").performClick()
                mainClock.advanceTimeBy(100)
                onNodeWithText(
                    "Place fingers on the screen and keep them down. After the timeout the app will choose, group, or order.",
                ).assertExists()
                onNodeWithContentDescription("Close").performClick()
                mainClock.advanceTimeBy(100)
                onNodeWithText("Start").assertExists()
            }
        }

        test("on desktop, held keys become fingers after Start") {
            runComposeUiTest {
                val settings = Settings(MemoryStore())
                val vm = RoundViewModel(settings, Random(0))
                mainClock.autoAdvance = false
                setContent { App(settings, vm, keyboardFingers = true) }
                mainClock.advanceTimeBy(100)

                onNodeWithText("Start").performClick()
                mainClock.advanceTimeBy(100)
                onRoot().performKeyInput {
                    keyDown(Key.A)
                    keyDown(Key.B)
                }
                mainClock.advanceTimeBy(100)
                vm.state.value.fingers.size shouldBe 2
            }
        }
    })
