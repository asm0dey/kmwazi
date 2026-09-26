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

import androidx.compose.runtime.internal.StabilityInferred
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo
import com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf
import com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import kotlin.random.Random

class ArchitectureTest :
    FunSpec({
        // Production classes of :shared only (desktop main output); test classes live under .../desktop/test/.
        val production =
            ClassFileImporter()
                .withImportOption { !it.contains("/test/") }
                .importPackages("com.github.asm0dey.kmwazi")

        test("round is pure Kotlin: no Android, Compose, coroutines or other app packages") {
            // Only the exact StabilityInferred class is allowed, not the whole
            // androidx.compose.runtime.internal package: the Kotlin Compose compiler plugin
            // (applied module-wide for stability inference) stamps @StabilityInferred onto every
            // class in :shared, including round's plain data classes that never reference Compose
            // in source. That one class is a compiler artifact, not a real dependency. The rest of
            // that package (ComposableLambda, AtomicInt, LiveLiteralInfo, ...) is real Compose
            // runtime machinery and must still be rejected.
            classes()
                .that()
                .resideInAPackage("..kmwazi.round..")
                .should()
                .onlyDependOnClassesThat(
                    resideInAnyPackage(
                        "..kmwazi.round..",
                        "kotlin..",
                        "java.lang..",
                        "java.util..",
                        "org.jetbrains.annotations..",
                    ).or(equivalentTo(StabilityInferred::class.java)),
                ).check(production)
        }

        test("ui is stateless: no settings storage or view model") {
            noClasses()
                .that()
                .resideInAPackage("..kmwazi.ui..")
                .should()
                .dependOnClassesThat(
                    belongToAnyOf(
                        Settings::class.java,
                        RoundViewModel::class.java,
                    ).or(resideInAPackage("androidx.datastore..")),
                ).check(production)
        }

        test("randomness comes only from entry points") {
            noGlobalRandomness.check(production)
        }

        test("the randomness rule catches every global source") {
            listOf(
                UsesDefault::class,
                UsesCompanionAsValue::class,
                UsesUnseededShuffled::class,
                UsesUnseededShuffle::class,
                UsesSeededRandom::class,
                UsesMathRandom::class,
                UsesThreadLocalRandom::class,
                UsesCollectionsShuffle::class,
            ).forEach { fixture ->
                withClue(fixture.simpleName) {
                    shouldThrow<AssertionError> {
                        noGlobalRandomness.check(ClassFileImporter().importClasses(fixture.java))
                    }
                }
            }
        }
    })

private val unseededRandomCall =
    object : DescribedPredicate<JavaMethodCall>("an unseeded shuffle, Random(seed) or Math.random") {
        override fun test(call: JavaMethodCall): Boolean {
            val owner = call.target.owner.name
            val name = call.target.name
            val takesRandom = call.target.rawParameterTypes.any { it.isAssignableTo(Random::class.java) }
            return (owner.startsWith("kotlin.collections.") && name in SHUFFLES && !takesRandom) ||
                (owner == "java.util.Collections" && name == "shuffle" && call.target.rawParameterTypes.size == 1) ||
                (owner == "kotlin.random.RandomKt" && name == "Random") ||
                (owner == "java.lang.Math" && name == "random")
        }
    }

private val SHUFFLES = setOf("shuffle", "shuffled")

// ADR 0001: the only randomness in :shared is the Random passed in from the platform shell.
// ponytail: covers the stdlib/JDK entry points we know of; add a fixture + clause if a new one turns up.
private val noGlobalRandomness: ArchRule =
    noClasses()
        .should()
        .dependOnClassesThat(
            resideInAPackage("java.security..")
                .or(equivalentTo(Random.Default::class.java))
                .or(assignableTo(java.util.Random::class.java)),
        ).orShould()
        .accessField(Random::class.java, "Default")
        .orShould()
        .callMethodWhere(unseededRandomCall)

// Each fixture reaches randomness without the injected Random; the rule must reject all of them.
private object UsesDefault {
    fun f() = Random.nextFloat()
}

private object UsesCompanionAsValue {
    fun f(): Random = Random
}

private object UsesUnseededShuffled {
    fun f() = listOf(1, 2).shuffled()
}

private object UsesUnseededShuffle {
    fun f() = mutableListOf(1, 2).shuffle()
}

private object UsesSeededRandom {
    fun f() = Random(1)
}

private object UsesMathRandom {
    fun f() = Math.random()
}

private object UsesThreadLocalRandom {
    fun f() =
        java.util.concurrent.ThreadLocalRandom
            .current()
            .nextInt()
}

private object UsesCollectionsShuffle {
    fun f() = java.util.Collections.shuffle(mutableListOf(1, 2))
}
