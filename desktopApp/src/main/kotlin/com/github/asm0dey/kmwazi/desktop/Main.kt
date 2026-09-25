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

package com.github.asm0dey.kmwazi.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.asm0dey.kmwazi.App
import com.github.asm0dey.kmwazi.RoundViewModel
import com.github.asm0dey.kmwazi.Settings
import java.io.File
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

// Dev/test window only; never packaged or released. Held keys stand in for fingers (no multi-touch on desktop).
fun main() {
    val dir = File(System.getProperty("user.home"), ".kmwazi").apply { mkdirs() }
    val settings = Settings(File(dir, "settings.preferences_pb").absolutePath)
    val vm = RoundViewModel(settings, SecureRandom().asKotlinRandom())
    application {
        Window(onCloseRequest = ::exitApplication, title = "Kmwazi") { App(settings, vm, keyboardFingers = true) }
    }
}
