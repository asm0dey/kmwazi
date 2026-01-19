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

package com.github.asm0dey.kmwazi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.R
import com.github.asm0dey.kmwazi.di.ServiceLocator.settingsRepository
import com.github.asm0dey.kmwazi.ui.PaletteRepository
import com.github.asm0dey.kmwazi.ui.Palettes
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val current = PaletteRepository.current.collectAsState().value
    val expandedState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val timeoutSecState =
        settingsRepository
            .decisionTimeoutSecondsFlow()
            .collectAsState(initial = 3)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.settings_palette_title), fontWeight = FontWeight.Bold)

            // Current selection preview (fixed-size stripes)
            ColorStripes(colors = current.colors)

            // Simple combo box using Button + DropdownMenu
            Box {
                // Non-button trigger styled as a surface-like clickable area
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier =
                        Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(onClickLabel = stringResource(R.string.settings_palette_label)) { expandedState.value = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    // Show compact stripes next to the name to hint it is a selector
                    ColorStripes(colors = current.colors, modifier = Modifier.size(width = 60.dp, height = 24.dp))
                    Text(stringResource(current.nameRes), color = MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(
                    expanded = expandedState.value,
                    onDismissRequest = { expandedState.value = false },
                ) {
                    Palettes.All.forEach { palette ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ColorStripes(
                                        colors = palette.colors,
                                        modifier = Modifier.size(width = 60.dp, height = 24.dp),
                                    )
                                    Text(
                                        stringResource(palette.nameRes),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            },
                            onClick = {
                                PaletteRepository.setPalette(palette)
                                scope.launch { settingsRepository.savePalette(palette) }
                                expandedState.value = false
                            },
                        )
                    }
                }
            }

            // Decision timeout setting (1..10 seconds)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.settings_timeout_label), color = MaterialTheme.colorScheme.onSurface)
                Button(
                    onClick = {
                        val newVal = (timeoutSecState.value - 1).coerceIn(1, 10)
                        scope.launch { settingsRepository.saveDecisionTimeoutSeconds(newVal) }
                    },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    val decreaseDescription = stringResource(R.string.settings_timeout_decrease)
                    Text("-", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { contentDescription = decreaseDescription })
                }
                Text(
                    stringResource(R.string.settings_timeout_value, timeoutSecState.value),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 8.dp).semantics { liveRegion = LiveRegionMode.Polite }
                )
                Button(
                    onClick = {
                        val newVal = (timeoutSecState.value + 1).coerceIn(1, 10)
                        scope.launch { settingsRepository.saveDecisionTimeoutSeconds(newVal) }
                    },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    val increaseDescription = stringResource(R.string.settings_timeout_increase)
                    Text("+", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { contentDescription = increaseDescription })
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), shape = CircleShape)
                    .clickable { onBack() }
                    .padding(12.dp),
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.settings_close), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ColorStripes(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    // Fixed-size preview area: width 160dp, height 16dp, divided into equal rectangular stripes
    Row(
        modifier =
            modifier
                .size(width = 160.dp, height = 16.dp),
    ) {
        colors.forEach { color ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .background(color)
                        .fillMaxSize(),
            )
        }
    }
}
