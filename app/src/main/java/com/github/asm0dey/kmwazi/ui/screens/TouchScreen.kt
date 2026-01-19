package com.github.asm0dey.kmwazi.ui.screens

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.asm0dey.kmwazi.di.ServiceLocator.settingsRepository
import com.github.asm0dey.kmwazi.domain.Mode
import com.github.asm0dey.kmwazi.ui.PaletteRepository
import com.github.asm0dey.kmwazi.ui.draw.FingerCanvas
import com.github.asm0dey.kmwazi.ui.draw.ResultOverlay
import com.github.asm0dey.kmwazi.ui.gestures.TouchEventListener
import com.github.asm0dey.kmwazi.ui.gestures.trackMultiTouch
import com.github.asm0dey.kmwazi.viewmodel.TouchViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun TouchScreen(onBack: () -> Unit) {
    val palette = PaletteRepository.current.collectAsState().value
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
    LaunchedEffect(Unit) {
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

    val resultAnnouncement = remember(result) {
        when (result) {
            is com.github.asm0dey.kmwazi.domain.Result.One -> "Winner selected"
            is com.github.asm0dey.kmwazi.domain.Result.Groups -> "Groups formed"
            is com.github.asm0dey.kmwazi.domain.Result.Order -> "Order defined"
            else -> ""
        }
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
    val activeIds = toDraw.keys.toSet()
    LaunchedEffect(activeIds) {
        val removed = fingerColors.keys - activeIds
        removed.forEach { fingerColors.remove(it) }
        if (points.isEmpty() && !inputLocked) nextColorIndexState.intValue = 0
    }

    LaunchedEffect(points.keys.toSet()) {
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
                        points[id] = position
                        vm.updateActive(points.toMap())
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
        val modeMenuExpanded = remember { mutableStateOf(false) }
        Box(modifier = Modifier.padding(16.dp)) {
            Button(onClick = { modeMenuExpanded.value = true }) {
                Text(
                    when (mode) {
                        is Mode.ChooseOne -> "Mode: Choose One"
                        is Mode.SplitIntoGroups -> "Mode: Groups (${mode.groupSize})"
                        is Mode.DefineOrder -> "Mode: Play Order"
                    }
                )
            }
            DropdownMenu(
                expanded = modeMenuExpanded.value,
                onDismissRequest = { modeMenuExpanded.value = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Choose One") },
                    onClick = {
                        vm.setMode(Mode.ChooseOne)
                        scope.launch { settingsRepository.saveMode(Mode.ChooseOne) }
                        vm.reset()
                        points.clear()
                        fingerColors.clear()
                        modeMenuExpanded.value = false
                    },
                    enabled = true,
                )
                DropdownMenuItem(
                    text = { Text("Play Order") },
                    onClick = {
                        vm.setMode(Mode.DefineOrder)
                        scope.launch { settingsRepository.saveMode(Mode.DefineOrder) }
                        vm.reset()
                        points.clear()
                        fingerColors.clear()
                        modeMenuExpanded.value = false
                    },
                    enabled = true,
                )
                DropdownMenuItem(
                    text = { Text("Groups") },
                    onClick = {
                        val m = Mode.SplitIntoGroups(groupSizeState.intValue)
                        vm.setMode(m)
                        scope.launch { settingsRepository.saveMode(m) }
                        vm.reset()
                        points.clear()
                        fingerColors.clear()
                        // keep menu open to allow adjusting size if desired
                    },
                    enabled = true,
                )

                // Show group size controls ONLY when group mode is selected
                if (mode is Mode.SplitIntoGroups) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(onClick = {
                                    if (groupSizeState.intValue > 1) {
                                        groupSizeState.intValue -= 1
                                        val m = Mode.SplitIntoGroups(groupSizeState.intValue)
                                        vm.setMode(m)
                                        scope.launch { settingsRepository.saveMode(m) }
                                        vm.reset()
                                        points.clear()
                                        fingerColors.clear()
                                    }
                                }, enabled = true) { Text("-") }
                                Text("Group size: ${groupSizeState.intValue}")
                                Button(onClick = {
                                    if (groupSizeState.intValue < 9) {
                                        groupSizeState.intValue += 1
                                        val m = Mode.SplitIntoGroups(groupSizeState.intValue)
                                        vm.setMode(m)
                                        scope.launch { settingsRepository.saveMode(m) }
                                        vm.reset()
                                        points.clear()
                                        fingerColors.clear()
                                    }
                                }, enabled = true) { Text("+") }
                            }
                        },
                        onClick = { /* no-op */ },
                        enabled = true,
                    )
                }
            }
        }

        FingerCanvas(
            points = toDraw,
            fingerColors = fingerColors,
            result = result,
            inputLocked = inputLocked,
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
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), shape = CircleShape)
                    .clickable { onBack() }
                    .padding(12.dp),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
