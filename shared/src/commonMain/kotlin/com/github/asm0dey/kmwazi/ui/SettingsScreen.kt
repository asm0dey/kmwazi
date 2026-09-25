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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.Palette
import com.github.asm0dey.kmwazi.Palettes
import com.github.asm0dey.kmwazi.Prefs
import com.github.asm0dey.kmwazi.TIMEOUTS
import com.github.asm0dey.kmwazi.resources.Res
import com.github.asm0dey.kmwazi.resources.settings_palette_label
import com.github.asm0dey.kmwazi.resources.settings_palette_title
import com.github.asm0dey.kmwazi.resources.settings_timeout_decrease
import com.github.asm0dey.kmwazi.resources.settings_timeout_increase
import com.github.asm0dey.kmwazi.resources.settings_timeout_label
import com.github.asm0dey.kmwazi.resources.settings_timeout_value
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    prefs: Prefs,
    onPalette: (Palette) -> Unit,
    onTimeout: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.settings_palette_title), fontWeight = FontWeight.Bold)
            ColorStripes(prefs.palette.colors, Modifier.size(width = 160.dp, height = 16.dp))
            Box {
                PaletteRow(
                    prefs.palette,
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClickLabel = stringResource(Res.string.settings_palette_label)) { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    Palettes.All.forEach { palette ->
                        DropdownMenuItem(
                            text = { PaletteRow(palette) },
                            onClick = {
                                onPalette(palette)
                                expanded = false
                            },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.settings_timeout_label))
                Stepper(
                    value = prefs.timeoutSec,
                    range = TIMEOUTS,
                    label = stringResource(Res.string.settings_timeout_value, prefs.timeoutSec),
                    onChange = onTimeout,
                    decreaseDescription = stringResource(Res.string.settings_timeout_decrease),
                    increaseDescription = stringResource(Res.string.settings_timeout_increase),
                )
            }
        }
        CloseButton(onClose, Modifier.align(Alignment.BottomEnd).padding(24.dp))
    }
}

@Composable
private fun PaletteRow(
    palette: Palette,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorStripes(palette.colors, Modifier.size(width = 60.dp, height = 24.dp))
        Text(stringResource(palette.name), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ColorStripes(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        colors.forEach { Box(Modifier.weight(1f).fillMaxSize().background(it)) }
    }
}
