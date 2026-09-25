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
import com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf
import com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec

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
            noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAPackage("java.security..")
                .check(production)
        }
    })
