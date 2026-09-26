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

package com.github.asm0dey.kmwazi.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.github.asm0dey.kmwazi.Palettes
import com.github.asm0dey.kmwazi.labelColor
import com.github.asm0dey.kmwazi.round.Draw
import com.github.asm0dey.kmwazi.round.Event
import com.github.asm0dey.kmwazi.round.Mode
import com.github.asm0dey.kmwazi.round.Result
import com.github.asm0dey.kmwazi.round.RoundState
import com.github.asm0dey.kmwazi.round.reduce
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.random.Random

private val draw = Draw(Random(0))

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showTouch(
    initial: RoundState = RoundState(Mode.ChooseOne),
    keyboardFingers: Boolean = false,
): MutableState<RoundState> {
    val state = mutableStateOf(initial)

    fun on(e: Event) {
        state.value = reduce(state.value, e, draw)
    }
    mainClock.autoAdvance = false
    setContent {
        TouchScreen(
            state = state.value,
            palette = Palettes.Vibrant,
            groupSize = 2,
            onFingers = { on(Event.FingersChanged(it)) },
            onMode = { on(Event.ModeChanged(it)) },
            onReset = { on(Event.Reset) },
            onClose = {},
            keyboardFingers = keyboardFingers,
        )
    }
    mainClock.advanceTimeByFrame()
    return state
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.pixel(at: Offset): Color {
    mainClock.advanceTimeByFrame()
    return onRoot().captureToImage().toPixelMap()[at.x.toInt(), at.y.toInt()]
}

// Labels are antialiased text, so they don't land on the exact finger centre pixel.
// Scan a small square around it and require at least one exact match on the glyph core.
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.hasPixelNear(
    at: Offset,
    expected: Color,
    radius: Int = 12,
): Boolean {
    mainClock.advanceTimeByFrame()
    val map = onRoot().captureToImage().toPixelMap()
    val cx = at.x.toInt()
    val cy = at.y.toInt()
    for (dx in -radius..radius) {
        for (dy in -radius..radius) {
            val x = cx + dx
            val y = cy + dy
            if (x in 0 until map.width && y in 0 until map.height && map[x, y] == expected) return true
        }
    }
    return false
}

@OptIn(ExperimentalTestApi::class)
class TouchScreenTest :
    FunSpec({
        val spots = List(12) { Offset(60f + it * 80f, 300f) }

        test("each finger gets a circle in the next palette colour") {
            runComposeUiTest {
                showTouch()
                onRoot().performTouchInput { spots.take(3).forEachIndexed { i, p -> down(i, p) } }
                spots.take(3).forEachIndexed { i, p -> pixel(p) shouldBe Palettes.Vibrant.color(i) }
            }
        }

        test("more fingers than colours are all drawn and colours wrap") {
            runComposeUiTest {
                val state = showTouch()
                onRoot().performTouchInput { spots.forEachIndexed { i, p -> down(i, p) } }
                state.value.fingers.size shouldBe 12
                pixel(spots[10]) shouldBe Palettes.Vibrant.color(0)
                pixel(spots[11]) shouldBe Palettes.Vibrant.color(1)
            }
        }

        test("a tap on the mode button is not a finger") {
            runComposeUiTest {
                val state = showTouch()
                onNodeWithText("Mode: Choose One").performClick()
                mainClock.advanceTimeByFrame()
                state.value.fingers shouldBe emptyMap()
                state.value.armed shouldBe 0
            }
        }

        test("a pointer that starts on a button never becomes a finger, even with jitter") {
            runComposeUiTest {
                val state = showTouch()
                onRoot().performTouchInput { down(0, spots[0]) }
                mainClock.advanceTimeByFrame()
                val armedAfterCanvasDown = state.value.armed
                val buttonCenter = onNodeWithText("Mode: Choose One").fetchSemanticsNode().boundsInRoot.center
                onRoot().performTouchInput {
                    down(1, buttonCenter)
                    moveBy(1, Offset(2f, 0f))
                    moveBy(0, Offset(2f, 0f))
                }
                mainClock.advanceTimeByFrame()
                state.value.fingers.keys shouldBe setOf(0L)
                state.value.armed shouldBe armedAfterCanvasDown
            }
        }

        test("fingers are gray in groups mode until the result") {
            runComposeUiTest {
                showTouch(RoundState(Mode.Groups(2)))
                onRoot().performTouchInput { down(0, spots[0]) }
                pixel(spots[0]) shouldBe Color.Gray
            }
        }

        test("choose one keeps the winner's colour and grays out the rest") {
            runComposeUiTest {
                val state = showTouch()
                onRoot().performTouchInput { spots.take(3).forEachIndexed { i, p -> down(i, p) } }
                mainClock.advanceTimeByFrame()
                state.value = reduce(state.value, Event.Expired(state.value.armed), draw)
                mainClock.advanceTimeBy(1_200) // overlay: 800 ms grow + 300 ms fade
                val winner = (state.value.outcome!!.result as Result.One).winner
                state.value.outcome!!.snapshot.forEach { (id, finger) ->
                    val expected = if (id == winner) Palettes.Vibrant.color(finger.colorIndex) else Color.DarkGray
                    pixel(Offset(finger.pos.x, finger.pos.y)) shouldBe expected
                }
            }
        }

        test("order result shows a readable position label on every finger") {
            runComposeUiTest {
                val state = showTouch(RoundState(Mode.Order))
                onRoot().performTouchInput { spots.take(3).forEachIndexed { i, p -> down(i, p) } }
                mainClock.advanceTimeByFrame()
                state.value = reduce(state.value, Event.Expired(state.value.armed), draw)
                mainClock.advanceTimeBy(1_200) // overlay: 800 ms grow + 300 ms fade
                val fingers = state.value.outcome!!.snapshot
                fingers.forEach { (_, finger) ->
                    val circleColour = Palettes.Vibrant.color(finger.colorIndex)
                    hasPixelNear(Offset(finger.pos.x, finger.pos.y), labelColor(circleColour)) shouldBe true
                }
            }
        }

        test("groups result shows a readable group-number label on every finger") {
            runComposeUiTest {
                val state = showTouch(RoundState(Mode.Groups(2)))
                onRoot().performTouchInput { spots.take(3).forEachIndexed { i, p -> down(i, p) } }
                mainClock.advanceTimeByFrame()
                state.value = reduce(state.value, Event.Expired(state.value.armed), draw)
                mainClock.advanceTimeBy(1_200) // overlay: 800 ms grow + 300 ms fade
                val result = state.value.outcome!!.result as Result.Groups
                val fingers = state.value.outcome!!.snapshot
                result.groups.forEachIndexed { groupIndex, ids ->
                    val circleColour = Palettes.Vibrant.color(groupIndex)
                    ids.forEach { id ->
                        val finger = fingers.getValue(id)
                        hasPixelNear(Offset(finger.pos.x, finger.pos.y), labelColor(circleColour)) shouldBe true
                    }
                }
            }
        }

        test("leaving composition with fingers down clears them (rotation safety net)") {
            runComposeUiTest {
                val state = mutableStateOf(RoundState(Mode.ChooseOne))
                val visible = mutableStateOf(true)

                fun on(e: Event) {
                    state.value = reduce(state.value, e, draw)
                }
                mainClock.autoAdvance = false
                setContent {
                    if (visible.value) {
                        TouchScreen(
                            state = state.value,
                            palette = Palettes.Vibrant,
                            groupSize = 2,
                            onFingers = { on(Event.FingersChanged(it)) },
                            onMode = { on(Event.ModeChanged(it)) },
                            onReset = { on(Event.Reset) },
                            onClose = {},
                        )
                    }
                }
                mainClock.advanceTimeByFrame()
                onRoot().performTouchInput { down(0, spots[0]) }
                mainClock.advanceTimeByFrame()
                state.value.fingers shouldHaveSize 1

                visible.value = false
                mainClock.advanceTimeByFrame()
                state.value.fingers shouldBe emptyMap()
            }
        }

        test("adding a finger re-arms the countdown") {
            runComposeUiTest {
                val state = showTouch()
                onRoot().performTouchInput { down(0, spots[0]) }
                mainClock.advanceTimeByFrame()
                val armedAfterFirst = state.value.armed
                onRoot().performTouchInput { down(1, spots[1]) }
                mainClock.advanceTimeByFrame()
                state.value.armed shouldBe armedAfterFirst + 1
            }
        }

        test("lifting all fingers then touching again starts a new round") {
            runComposeUiTest {
                val state = showTouch()
                onRoot().performTouchInput {
                    down(0, spots[0])
                    down(1, spots[1])
                }
                mainClock.advanceTimeByFrame()
                state.value = reduce(state.value, Event.Expired(state.value.armed), draw)
                mainClock.advanceTimeBy(1_200)
                onRoot().performTouchInput {
                    up(0)
                    up(1)
                }
                mainClock.advanceTimeByFrame()
                onRoot().performTouchInput { down(2, spots[2]) }
                mainClock.advanceTimeByFrame()
                state.value.outcome shouldBe null
                state.value.fingers shouldHaveSize 1
                state.value.fingers shouldContainKey 2L
            }
        }
        test("held keys become fingers inside the screen and lift on release") {
            runComposeUiTest {
                val state = showTouch(keyboardFingers = true)
                val size = onRoot().fetchSemanticsNode().size
                onRoot().performKeyInput {
                    keyDown(Key.A)
                    keyDown(Key.B)
                }
                mainClock.advanceTimeByFrame()
                state.value.fingers shouldHaveSize 2
                state.value.fingers.values.forEach {
                    (it.pos.x in 0f..size.width.toFloat()) shouldBe true
                    (it.pos.y in 0f..size.height.toFloat()) shouldBe true
                }
                onRoot().performKeyInput { keyUp(Key.A) }
                mainClock.advanceTimeByFrame()
                state.value.fingers shouldHaveSize 1
                onRoot().performKeyInput { keyUp(Key.B) }
                mainClock.advanceTimeByFrame()
                state.value.fingers shouldBe emptyMap()
            }
        }

        test("escape is never a finger") {
            runComposeUiTest {
                val state = showTouch(keyboardFingers = true)
                onRoot().performKeyInput { keyDown(Key.Escape) }
                mainClock.advanceTimeByFrame()
                state.value.fingers shouldBe emptyMap()
            }
        }

        test("keys do nothing unless keyboard fingers are enabled") {
            runComposeUiTest {
                val state = showTouch()
                onRoot().performKeyInput { keyDown(Key.A) }
                mainClock.advanceTimeByFrame()
                state.value.fingers shouldBe emptyMap()
            }
        }

        test("the group size stepper is fully visible in a short window") {
            runDesktopComposeUiTest(width = 800, height = 480) {
                showTouch(RoundState(Mode.Groups(2)))
                onNodeWithText("Mode: Groups (2)").performClick()
                mainClock.advanceTimeBy(2_000)
                onNodeWithText("Group size: 2").assertIsDisplayed()
                onNodeWithText("+").assertIsDisplayed()
            }
        }
    })
