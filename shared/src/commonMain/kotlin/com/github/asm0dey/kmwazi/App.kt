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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.github.asm0dey.kmwazi.round.Event
import com.github.asm0dey.kmwazi.ui.HelpScreen
import com.github.asm0dey.kmwazi.ui.HomeScreen
import com.github.asm0dey.kmwazi.ui.SettingsScreen
import com.github.asm0dey.kmwazi.ui.TouchScreen
import kotlinx.coroutines.launch

enum class Screen { Home, Touch, Settings, Help }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App(
    settings: Settings,
    vm: RoundViewModel,
) {
    val prefs by settings.prefs.collectAsState(Prefs())
    val scope = rememberCoroutineScope()
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    // Leaving the touch screen ends the round, like 1.3.0's per-visit ViewModel did.
    fun go(to: Screen) {
        if (screen == Screen.Touch) vm.send(Event.Reset)
        screen = to
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            // Android back / desktop Esc: everything returns to Home; Home lets the system handle it.
            BackHandler(enabled = screen != Screen.Home) { go(Screen.Home) }
            when (screen) {
                Screen.Home ->
                    HomeScreen(
                        onStart = { go(Screen.Touch) },
                        onSettings = { go(Screen.Settings) },
                        onHelp = { go(Screen.Help) },
                    )
                Screen.Touch -> {
                    val state by vm.state.collectAsState()
                    TouchScreen(
                        state = state,
                        palette = prefs.palette,
                        groupSize = prefs.groupSize,
                        onFingers = { vm.send(Event.FingersChanged(it)) },
                        onMode = vm::setMode,
                        onReset = { vm.send(Event.Reset) },
                        onClose = { go(Screen.Home) },
                    )
                }
                Screen.Settings ->
                    SettingsScreen(
                        prefs = prefs,
                        onPalette = { scope.launch { settings.setPalette(it) } },
                        onTimeout = { scope.launch { settings.setTimeout(it) } },
                        onClose = { go(Screen.Home) },
                    )
                Screen.Help -> HelpScreen(onClose = { go(Screen.Home) })
            }
        }
    }
}
