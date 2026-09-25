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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.Palette
import com.github.asm0dey.kmwazi.resources.Res
import com.github.asm0dey.kmwazi.resources.touch_groups_formed
import com.github.asm0dey.kmwazi.resources.touch_mode_choose_one
import com.github.asm0dey.kmwazi.resources.touch_mode_groups
import com.github.asm0dey.kmwazi.resources.touch_mode_play_order
import com.github.asm0dey.kmwazi.resources.touch_order_defined
import com.github.asm0dey.kmwazi.resources.touch_reset
import com.github.asm0dey.kmwazi.resources.touch_winner_selected
import com.github.asm0dey.kmwazi.round.Mode
import com.github.asm0dey.kmwazi.round.Point
import com.github.asm0dey.kmwazi.round.Result
import com.github.asm0dey.kmwazi.round.RoundState
import org.jetbrains.compose.resources.stringResource

@Composable
fun TouchScreen(
    state: RoundState,
    palette: Palette,
    groupSize: Int,
    onFingers: (Map<Long, Point>) -> Unit,
    onMode: (Mode) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val latestOnFingers by rememberUpdatedState(onFingers)
    val pressed = remember { mutableSetOf<Long>() }
    var sheetOpen by remember { mutableStateOf(false) }

    // The VM survives activity recreation (e.g. rotation) but the in-progress gesture does not:
    // clear fingers so a rotation never leaves a stale, un-liftable finger armed for a result.
    DisposableEffect(Unit) { onDispose { latestOnFingers(emptyMap()) } }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse",
    )
    val grow = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(state.outcome) {
        if (state.outcome == null) {
            fade.snapTo(0f)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            fade.snapTo(1f)
            grow.snapTo(0f)
            grow.animateTo(1f, tween(800))
            fade.animateTo(0f, tween(300))
        }
    }

    val announcement =
        when (state.outcome?.result) {
            is Result.One -> stringResource(Res.string.touch_winner_selected)
            is Result.Groups -> stringResource(Res.string.touch_groups_formed)
            is Result.Order -> stringResource(Res.string.touch_order_defined)
            null -> ""
        }

    Box(
        modifier
            .fillMaxSize()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = announcement
            }.multiTouch { points ->
                if ((points.keys - pressed).isNotEmpty()) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                pressed.clear()
                pressed.addAll(points.keys)
                latestOnFingers(points)
            },
    ) {
        FingerCanvas(state, palette, pulse, grow.value, fade.value)
        Button(onClick = { sheetOpen = true }, modifier = Modifier.padding(16.dp)) {
            Text(
                when (val m = state.mode) {
                    Mode.ChooseOne -> stringResource(Res.string.touch_mode_choose_one)
                    is Mode.Groups -> stringResource(Res.string.touch_mode_groups, m.size)
                    Mode.Order -> stringResource(Res.string.touch_mode_play_order)
                },
            )
        }
        Row(
            Modifier.align(Alignment.BottomEnd).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.outcome != null) Button(onClick = onReset) { Text(stringResource(Res.string.touch_reset)) }
            CloseButton(onClose)
        }
        if (sheetOpen) {
            ModeSheet(
                mode = state.mode,
                groupSize = groupSize,
                onMode = { m ->
                    onMode(m)
                    if (m !is Mode.Groups) sheetOpen = false
                },
                onDismiss = { sheetOpen = false },
            )
        }
    }
}
