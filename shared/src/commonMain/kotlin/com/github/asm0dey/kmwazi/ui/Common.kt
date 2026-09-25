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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.resources.Res
import com.github.asm0dey.kmwazi.resources.close
import com.github.asm0dey.kmwazi.resources.settings_close
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
    ) {
        Icon(
            painterResource(Res.drawable.close),
            contentDescription = stringResource(Res.string.settings_close),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun Stepper(
    value: Int,
    range: IntRange,
    label: String,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    decreaseDescription: String? = null,
    increaseDescription: String? = null,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        StepButton("-", decreaseDescription, enabled = value > range.first) { onChange(value - 1) }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        StepButton("+", increaseDescription, enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(
    symbol: String,
    description: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).semantics { if (description != null) contentDescription = description },
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(symbol, style = MaterialTheme.typography.headlineSmall)
    }
}
