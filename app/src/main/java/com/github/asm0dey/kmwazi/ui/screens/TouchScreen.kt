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

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.github.asm0dey.kmwazi.R
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.asm0dey.kmwazi.di.ServiceLocator.settingsRepository
import com.github.asm0dey.kmwazi.domain.Mode
import com.github.asm0dey.kmwazi.ui.Palettes
import com.github.asm0dey.kmwazi.ui.draw.FingerCanvas
import com.github.asm0dey.kmwazi.ui.draw.ResultOverlay
import com.github.asm0dey.kmwazi.ui.gestures.TouchEventListener
import com.github.asm0dey.kmwazi.ui.gestures.trackMultiTouch
import com.github.asm0dey.kmwazi.viewmodel.TouchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchScreen(onBack: () -> Unit) {
    val palette by settingsRepository.paletteFlow().collectAsState(initial = Palettes.Vibrant)
    val vm: TouchViewModel = viewModel()
    val inputLocked = vm.inputLocked.collectAsState().value
    val snapshot = vm.snapshot.collectAsState().value
    val mode = vm.mode.collectAsState().value
    val result = vm.result.collectAsState().value

    val scope = rememberCoroutineScope()

    // Haptic feedback
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val points = remember { mutableStateMapOf<Long, Offset>() }
    // Assign stable colors per active finger using the selected palette
    val fingerColors = remember { mutableStateMapOf<Long, Color>() }
    val nextColorIndexState = remember { mutableIntStateOf(0) }
    val groupSizeState = remember { mutableIntStateOf(2) }

    // Load saved mode and decision timeout on first composition
    val view = LocalView.current
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val savedMode = settingsRepository.modeFlow().first()
        vm.setMode(savedMode)
        if (savedMode is Mode.SplitIntoGroups) {
            groupSizeState.intValue = savedMode.groupSize
        }
        val timeout = settingsRepository.decisionTimeoutSecondsFlow().first()
        vm.setDecisionTimeoutSeconds(timeout)
    }

    // Handle results overlay
    val showOverlay = remember { mutableStateOf(false) }
    val resultProgress = remember { Animatable(0f) }

    LaunchedEffect(result) {
        if (result != null) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            showOverlay.value = true
            resultProgress.snapTo(0f)
            resultProgress.animateTo(1f, tween(800))
            delay(1000)
            showOverlay.value = false
        } else {
            showOverlay.value = false
            resultProgress.snapTo(0f)
        }
    }

    val resultAnnouncement =
        when (result) {
            is com.github.asm0dey.kmwazi.domain.Result.One -> stringResource(R.string.touch_winner_selected)
            is com.github.asm0dey.kmwazi.domain.Result.Groups -> stringResource(R.string.touch_groups_formed)
            is com.github.asm0dey.kmwazi.domain.Result.Order -> stringResource(R.string.touch_order_defined)
            else -> ""
        }

    // Pulsing animation for active points
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseFactor"
    )

    // Logic to manage finger colors
    val toDraw = if (inputLocked && snapshot != null) snapshot else points
    LaunchedEffect(toDraw.keys) {
        val activeIds = toDraw.keys
        val removed = fingerColors.keys - activeIds
        removed.forEach { fingerColors.remove(it) }
        if (points.isEmpty() && !inputLocked) nextColorIndexState.intValue = 0
    }

    LaunchedEffect(points.keys) {
        points.keys.forEach { id ->
            if (!fingerColors.containsKey(id)) {
                if (mode is Mode.SplitIntoGroups) {
                    fingerColors[id] = Color.Gray
                } else {
                    val idx = nextColorIndexState.intValue % palette.colors.size.coerceAtLeast(1)
                    fingerColors[id] = palette.colors.getOrElse(idx) { Color(0xFF00E5FF) }
                    nextColorIndexState.intValue += 1
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = resultAnnouncement
            }
            .pointerInput(Unit) {
                trackMultiTouch(object : TouchEventListener {
                    override fun onFingerDown(id: Long, position: Offset) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        points[id] = position
                        vm.updateActive(points.toMap())
                    }

                    override fun onFingerMove(id: Long, position: Offset) {
                        points[id] = position
                        vm.updateActive(points.toMap())
                    }

                    override fun onFingerUp(id: Long) {
                        points.remove(id)
                        vm.updateActive(points.toMap())
                    }

                    override fun onAllFingersUp() {
                        // All fingers removed, vm.updateActive already called in onFingerUp
                    }

                    override fun onLongPress() {
                        if (vm.inputLocked.value) {
                            vm.reset()
                            points.clear()
                            fingerColors.clear()
                        }
                    }
                })
            }
    ) {
        // Mode Selector (Top Left)
        val sheetState = rememberModalBottomSheetState()
        val showModeSheet = remember { mutableStateOf(false) }

        Box(modifier = Modifier.padding(16.dp)) {
            Button(onClick = { showModeSheet.value = true }) {
                Text(
                    when (mode) {
                        is Mode.ChooseOne -> stringResource(R.string.touch_mode_choose_one)
                        is Mode.SplitIntoGroups -> stringResource(R.string.touch_mode_groups, mode.groupSize)
                        is Mode.DefineOrder -> stringResource(R.string.touch_mode_play_order)
                    }
                )
            }
        }

        if (showModeSheet.value) {
            ModalBottomSheet(
                onDismissRequest = { showModeSheet.value = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        stringResource(R.string.touch_selection_mode_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.touch_choose_one)) },
                        onClick = {
                            vm.setMode(Mode.ChooseOne)
                            scope.launch { settingsRepository.saveMode(Mode.ChooseOne) }
                            vm.reset()
                            points.clear()
                            fingerColors.clear()
                            showModeSheet.value = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.touch_play_order)) },
                        onClick = {
                            vm.setMode(Mode.DefineOrder)
                            scope.launch { settingsRepository.saveMode(Mode.DefineOrder) }
                            vm.reset()
                            points.clear()
                            fingerColors.clear()
                            showModeSheet.value = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.touch_groups)) },
                        onClick = {
                            val m = Mode.SplitIntoGroups(groupSizeState.intValue)
                            vm.setMode(m)
                            scope.launch { settingsRepository.saveMode(m) }
                            vm.reset()
                            points.clear()
                            fingerColors.clear()
                        }
                    )

                    // Show group size controls ONLY when group mode is selected
                    if (mode is Mode.SplitIntoGroups) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (groupSizeState.intValue > 2) {
                                        groupSizeState.intValue -= 1
                                        val m = Mode.SplitIntoGroups(groupSizeState.intValue)
                                        vm.setMode(m)
                                        scope.launch { settingsRepository.saveMode(m) }
                                        vm.reset()
                                        points.clear()
                                        fingerColors.clear()
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("-", style = MaterialTheme.typography.headlineSmall)
                            }
                            Text(
                                stringResource(R.string.touch_group_size, groupSizeState.intValue),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Button(
                                onClick = {
                                    if (groupSizeState.intValue < 10) {
                                        groupSizeState.intValue += 1
                                        val m = Mode.SplitIntoGroups(groupSizeState.intValue)
                                        vm.setMode(m)
                                        scope.launch { settingsRepository.saveMode(m) }
                                        vm.reset()
                                        points.clear()
                                        fingerColors.clear()
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("+", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
            }
        }

        FingerCanvas(
            points = toDraw,
            fingerColors = fingerColors,
            result = result,
            pulseFactor = pulseFactor,
            paletteColors = palette.colors
        )

        ResultOverlay(
            show = showOverlay.value,
            result = result,
            progress = resultProgress,
            snapshot = snapshot,
            fingerColors = fingerColors,
            palette = palette
        )
        // Back control as a cross icon in bottom-right
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (vm.inputLocked.collectAsState().value) {
                Button(
                    onClick = {
                        vm.reset()
                        points.clear()
                        fingerColors.clear()
                    }
                ) {
                    Text(stringResource(R.string.touch_reset))
                }
            }
            Box(
                modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), shape = CircleShape)
                    .clickable { onBack() }
                    .padding(12.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.settings_close), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
