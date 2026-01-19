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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.R
import com.github.asm0dey.kmwazi.ui.navigation.Screen

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.home_title),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        Button(
            onClick = { onNavigate(Screen.Touch.route) },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.home_start))
            Text(" ${stringResource(R.string.home_start)} ", style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = { onNavigate(Screen.Settings.route) },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_settings))
            Text(" ${stringResource(R.string.home_settings)} ", style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = { onNavigate(Screen.Help.route) },
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.home_help))
            Text(" ${stringResource(R.string.home_help)} ", style = MaterialTheme.typography.labelLarge)
        }
    }
}
