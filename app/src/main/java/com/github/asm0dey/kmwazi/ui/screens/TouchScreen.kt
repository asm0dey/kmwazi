package com.github.asm0dey.kmwazi.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.asm0dey.kmwazi.di.ServiceLocator.settingsRepository
import com.github.asm0dey.kmwazi.domain.Mode
import com.github.asm0dey.kmwazi.domain.Result
import com.github.asm0dey.kmwazi.ui.PaletteRepository
import com.github.asm0dey.kmwazi.ui.gestures.trackMultiTouch
import com.github.asm0dey.kmwazi.viewmodel.TouchViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.hypot
import android.graphics.Paint as AndroidPaint

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
            haptic.performHapticFeedback(androidx.compose.ui.input.pointer.PointerType.Touch.let { androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress })
            showOverlay.value = true
            resultProgress.snapTo(0f)
            resultProgress.animateTo(1f, tween(800))
        } else {
            showOverlay.value = false
            resultProgress.snapTo(0f)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(inputLocked) {
                if (!inputLocked) {
                    trackMultiTouch(
                        points = points,
                        onChanged = { vm.updateActive(it) },
                        onFingerAdded = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    )
                } else {
                    // When locked, still listen for a long-press anywhere to reset
                    awaitEachGesture {
                        val down = awaitPointerEvent().changes.firstOrNull()
                        if (down != null) {
                            val start = System.currentTimeMillis()
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.changedToUp() }) break
                                if (System.currentTimeMillis() - start > TouchViewModel.LONG_PRESS_RESET_MS) {
                                    vm.reset()
                                    points.clear()
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    break
                                }
                            }
                        }
                    }
                }
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

        Canvas(modifier = Modifier.fillMaxSize()) {
            val toDraw = if (inputLocked) (snapshot ?: emptyMap()) else points
            // Cleanup colors for removed fingers
            val activeIds = toDraw.keys.toSet()
            val removed = fingerColors.keys - activeIds
            removed.forEach { fingerColors.remove(it) }
            // Reset color rotation if no fingers
            if (activeIds.isEmpty()) nextColorIndexState.intValue = 0
            var count = 0

            // Determine per-mode rendering
            val winnerId = (result as? Result.One)?.winnerId
            val orderMap: Map<Long, Int>? = (result as? Result.Order)?.order?.withIndex()?.associate { it.value to (it.index + 1) }
            val groupsMap: Map<Long, Int>? =
                (result as? Result.Groups)?.groups?.withIndex()?.flatMap { (gi, g) -> g.map { it to gi } }
                    ?.toMap()
            val groupColors = palette.colors

            toDraw.forEach { (id, pos) ->
                if (count < 10) {
                    val color =
                        when {
                            !inputLocked -> {
                                // Assign per-finger color from current palette, rotating when palette is exhausted
                                val existing = fingerColors[id]
                                if (existing != null) {
                                    existing
                                } else {
                                    val idx = nextColorIndexState.intValue % palette.colors.size.coerceAtLeast(1)
                                    val c = palette.colors.getOrElse(idx) { Color(0xFF00E5FF) }
                                    fingerColors[id] = c
                                    nextColorIndexState.intValue += 1
                                    c
                                }
                            }
                            groupsMap != null -> groupColors[groupsMap[id]!! % groupColors.size]
                            winnerId != null && id == winnerId -> fingerColors[id] ?: Color(0xFF4CAF50)
                            winnerId != null -> Color(0xFF444444)
                            else -> Color(0xFF4CAF50)
                        }
                    val currentRadius = 110f * pulseFactor
                    drawCircle(
                        color = color,
                        radius = currentRadius,
                        center = pos,
                    )
                    // If order mode, draw the number label inside the circle (centered)
                    val num = orderMap?.get(id)
                    if (num != null) {
                        val paint =
                            AndroidPaint().apply {
                                isAntiAlias = true
                                this.color = Color.White.toArgb()
                                textSize = currentRadius * 0.6f
                                textAlign = AndroidPaint.Align.CENTER
                            }
                        val baselineY = pos.y - (paint.descent() + paint.ascent()) / 2f
                        drawContext.canvas.nativeCanvas.drawText(
                            num.toString(),
                            pos.x,
                            baselineY,
                            paint,
                        )
                    }
                    count++
                }
            }
        }

        // Results overlay animations
        if (showOverlay.value && result != null) {
            val winnerIdOverlay = (result as? Result.One)?.winnerId
            val winnerPos = winnerIdOverlay?.let { id -> snapshot?.get(id) }
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (result) {
                    is Result.One -> {
                        val center = winnerPos ?: Offset(this.size.width / 2f, this.size.height / 2f)
                        val maxRadius = hypot(this.size.width.toDouble(), this.size.height.toDouble()).toFloat()
                        val r = maxRadius * resultProgress.value
                        val color = fingerColors[winnerIdOverlay] ?: palette.colors.firstOrNull() ?: Color(0xFF4CAF50)
                        drawCircle(color = color.copy(alpha = 0.5f), radius = r, center = center)
                    }
                    is Result.Order -> {
                        val firstId = result.order.firstOrNull()
                        val center =
                            firstId?.let { fid ->
                                snapshot?.get(fid)
                            } ?: Offset(this.size.width / 2f, this.size.height / 2f)
                        val maxRadius = hypot(this.size.width.toDouble(), this.size.height.toDouble()).toFloat()
                        val r = maxRadius * resultProgress.value
                        val color =
                            firstId?.let { fid ->
                                fingerColors[fid]
                            } ?: palette.colors.firstOrNull() ?: Color(0xFF2196F3)
                        drawCircle(color = color.copy(alpha = 0.5f), radius = r, center = center)
                    }
                    is Result.Groups -> {
                        val h = this.size.height * resultProgress.value
                        val color = palette.colors.firstOrNull() ?: Color(0xFF2196F3)
                        drawRect(color = color.copy(alpha = 0.5f), size = androidx.compose.ui.geometry.Size(this.size.width, h))
                    }
                }
            }
        }
        // Back control as a cross icon in bottom-right
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .background(Color(0x33000000), shape = CircleShape)
                    .clickable { onBack() }
                    .padding(12.dp),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}
