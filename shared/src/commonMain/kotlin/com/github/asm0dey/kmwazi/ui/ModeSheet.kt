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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.resources.Res
import com.github.asm0dey.kmwazi.resources.touch_choose_one
import com.github.asm0dey.kmwazi.resources.touch_group_size
import com.github.asm0dey.kmwazi.resources.touch_groups
import com.github.asm0dey.kmwazi.resources.touch_play_order
import com.github.asm0dey.kmwazi.resources.touch_selection_mode_title
import com.github.asm0dey.kmwazi.round.Mode
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSheet(
    mode: Mode,
    groupSize: Int,
    onMode: (Mode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Text(
                stringResource(Res.string.touch_selection_mode_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.touch_choose_one)) },
                onClick = { onMode(Mode.ChooseOne) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.touch_play_order)) },
                onClick = { onMode(Mode.Order) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.touch_groups)) },
                onClick = { onMode(Mode.Groups(groupSize)) },
            )
            if (mode is Mode.Groups) {
                Stepper(
                    value = mode.size,
                    range = Mode.Groups.SIZES,
                    label = stringResource(Res.string.touch_group_size, mode.size),
                    onChange = { onMode(Mode.Groups(it)) },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
