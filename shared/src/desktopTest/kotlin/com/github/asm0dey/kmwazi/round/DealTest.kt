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
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import kotlin.random.Random

class DealTest :
    FunSpec({
        val fingerIds = Arb.set(Arb.long(), 1..30).map { it.toList() }

        test("groups contain every finger exactly once and all but the last group are full") {
            checkAll(fingerIds, Arb.int(Mode.Groups.SIZES), Arb.long()) { ids, size, seed ->
                val groups = Deal(Random(seed)).groups(ids, size)
                groups.flatten() shouldContainExactlyInAnyOrder ids
                groups.dropLast(1).forEach { it.size shouldBe size }
                groups.last().size shouldBeInRange 1..size
            }
        }

        test("fewer fingers than the group size make one group with everyone") {
            Deal(Random(1)).groups(listOf(1L, 2L), 4).single() shouldContainExactlyInAnyOrder listOf(1L, 2L)
        }

        test("order is a permutation of the fingers") {
            checkAll(fingerIds, Arb.long()) { ids, seed ->
                Deal(Random(seed)).order(ids) shouldContainExactlyInAnyOrder ids
            }
        }

        test("choose one returns one of the fingers") {
            checkAll(fingerIds, Arb.long()) { ids, seed ->
                Deal(Random(seed)).chooseOne(ids) shouldBeIn ids
            }
        }

        test("deal follows the mode") {
            val deal = Deal(Random(1))
            deal.deal(Mode.ChooseOne, listOf(5L)) shouldBe Result.One(5L)
            deal.deal(Mode.Groups(2), listOf(5L)) shouldBe Result.Groups(listOf(listOf(5L)))
            deal.deal(Mode.Order, listOf(5L)) shouldBe Result.Order(listOf(5L))
        }
    })
