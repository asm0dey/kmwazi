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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.asm0dey.kmwazi.round.Deal
import com.github.asm0dey.kmwazi.round.Event
import com.github.asm0dey.kmwazi.round.Mode
import com.github.asm0dey.kmwazi.round.RoundState
import com.github.asm0dey.kmwazi.round.reduce
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class RoundViewModel(
    private val settings: Settings,
    random: Random,
) : ViewModel() {
    private val deal = Deal(random)
    private val _state = MutableStateFlow(RoundState(Mode.ChooseOne))
    val state: StateFlow<RoundState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val mode = settings.prefs.first().mode
            // Only apply the persisted mode if no round activity has happened yet (armed == 0);
            // otherwise this stale read would clobber a finger/mode event the caller already sent.
            _state.update { if (it.armed == 0) reduce(it, Event.ModeChanged(mode), deal) else it }
        }
        // Every armed value gets its own countdown; collectLatest cancels the previous one.
        viewModelScope.launch {
            _state.map { it.armed }.distinctUntilChanged().collectLatest { armed ->
                delay(settings.prefs.first().timeoutSec * 1000L)
                send(Event.Expired(armed))
            }
        }
    }

    fun send(event: Event) {
        _state.update { reduce(it, event, deal) }
    }

    fun setMode(mode: Mode) {
        send(Event.ModeChanged(mode))
        viewModelScope.launch { settings.setMode(mode) }
    }
}
