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

package com.github.asm0dey.kmwazi.round

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.random.Random

private val draw = Draw(Random(0))

private fun RoundState.on(vararg events: Event): RoundState = events.fold(this) { s, e -> reduce(s, e, draw) }

private fun touch(vararg ids: Long) = Event.FingersChanged(ids.associateWith { Point(it * 10f, 0f) })

private fun RoundState.expire() = on(Event.Expired(armed))

class RoundTest :
    FunSpec({
        val start = RoundState(Mode.ChooseOne)

        test("a new finger set arms the countdown") {
            start.on(touch(1)).armed shouldBe 1
            start.on(touch(1), touch(1, 2)).armed shouldBe 2
        }

        test("moving fingers updates positions without re-arming") {
            val moved = start.on(touch(1), Event.FingersChanged(mapOf(1L to Point(5f, 5f))))
            moved.armed shouldBe 1
            moved.fingers.getValue(1L).pos shouldBe Point(5f, 5f)
        }

        test("lifting every finger before a result cancels the countdown and restarts colours") {
            val s = start.on(touch(1, 2), touch())
            s.fingers shouldBe emptyMap()
            s.expire().outcome.shouldBeNull()
            val next = s.on(touch(3)).fingers
            next.getValue(3L).colorIndex shouldBe 0
        }

        test("a matching expiry draws and locks with a snapshot") {
            val s = start.on(touch(1, 2)).expire()
            val outcome = s.outcome.shouldNotBeNull()
            outcome.snapshot.keys shouldBe setOf(1L, 2L)
            (outcome.result as Result.One).winner shouldBeIn listOf(1L, 2L)
        }

        test("a single finger never gets a result") {
            val s = start.on(touch(1))
            s.expire().outcome.shouldBeNull()
        }

        test("a finger lifting before the timeout leaves one finger and no result") {
            val s = start.on(touch(1, 2), touch(1))
            s.expire().outcome.shouldBeNull()
        }

        test("a lone finger after a locked round starts a round that draws nothing") {
            val locked = start.on(touch(1, 2)).expire()
            val s = locked.on(touch(), touch(3)).expire()
            s.outcome.shouldBeNull()
            s.fingers.keys shouldBe setOf(3L)
        }

        test("a stale expiry is ignored") {
            start.on(touch(1), touch(1, 2), Event.Expired(1)).outcome.shouldBeNull()
        }

        test("changing mode during a countdown makes the pending expiry stale") {
            val counting = start.on(touch(1, 2))
            val changed = counting.on(Event.ModeChanged(Mode.Order))
            changed.mode shouldBe Mode.Order
            changed.fingers shouldBe emptyMap()
            changed.on(Event.Expired(counting.armed)).outcome.shouldBeNull()
        }

        test("while locked, finger changes are ignored until everyone lifts") {
            val locked = start.on(touch(1, 2)).expire()
            val s = locked.on(touch(1), touch(1, 3))
            s.outcome shouldBe locked.outcome
            s.armed shouldBe locked.armed
        }

        test("the first touch after everyone lifted starts a new round") {
            val locked = start.on(touch(1, 2)).expire()
            val s = locked.on(touch(), touch(3))
            s.outcome.shouldBeNull()
            s.fingers.keys shouldBe setOf(3L)
            s.fingers.getValue(3L).colorIndex shouldBe 0
            s.armed shouldBe locked.armed + 2
        }

        test("an expiry while locked does not draw again") {
            val locked = start.on(touch(1, 2)).expire()
            locked.expire() shouldBe locked
        }

        test("reset clears the round and keeps the mode") {
            RoundState(Mode.Order).on(touch(1), Event.Reset) shouldBe
                RoundState(Mode.Order, armed = 2)
        }

        test("new fingers take the lowest free colour and keep it") {
            val s = start.on(touch(1), touch(1, 2), touch(2), touch(2, 3))
            s.fingers.mapValues { it.value.colorIndex } shouldBe mapOf(2L to 1, 3L to 0)
        }

        test("tapping a third finger never collides with the two held down") {
            var s = start.on(touch(1, 2))
            for (id in 3L..20L) {
                s = s.on(touch(1, 2, id), touch(1, 2))
            }
            s.on(touch(1, 2, 21)).fingers.mapValues { it.value.colorIndex } shouldBe
                mapOf(1L to 0, 2L to 1, 21L to 2)
        }

        test("colour indices keep counting past the palette size") {
            val s = start.on(touch(*LongArray(12) { it.toLong() }))
            s.fingers.values
                .map { it.colorIndex }
                .sorted() shouldBe (0..11).toList()
        }
    })
