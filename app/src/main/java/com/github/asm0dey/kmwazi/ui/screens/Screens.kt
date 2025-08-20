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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.asm0dey.kmwazi.data.SettingsRepository
import com.github.asm0dey.kmwazi.domain.Mode
import com.github.asm0dey.kmwazi.domain.Result
import com.github.asm0dey.kmwazi.ui.PaletteRepository
import com.github.asm0dey.kmwazi.ui.Palettes
import com.github.asm0dey.kmwazi.ui.navigation.Routes
import com.github.asm0dey.kmwazi.viewmodel.TouchViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.hypot
import android.graphics.Paint as AndroidPaint

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Kmwazi")
        Button(onClick = { onNavigate(Routes.Touch) }) { Icon(Icons.Default.PlayArrow, contentDescription = null); Text(" Start ") }
        Button(onClick = { onNavigate(Routes.Settings) }) { Icon(Icons.Default.Settings, contentDescription = null); Text(" Settings ") }
        Button(onClick = { onNavigate(Routes.Help) }) { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null); Text(" Help ") }
    }
}

@Composable
fun TouchScreen(onBack: () -> Unit) {
    val palette = PaletteRepository.current.collectAsState().value
    val vm: TouchViewModel = viewModel()
    val inputLocked = vm.inputLocked.collectAsState().value
    val remaining = vm.remainingMs.collectAsState().value
    val snapshot = vm.snapshot.collectAsState().value
    val mode = vm.mode.collectAsState().value
    val result = vm.result.collectAsState().value

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Haptic feedback
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val points = remember { mutableStateMapOf<Long, Offset>() }
    // Assign stable colors per active finger using the selected palette
    val fingerColors = remember { mutableStateMapOf<Long, Color>() }
    val nextColorIndexState = remember { mutableIntStateOf(0) }
    val groupSizeState = remember { mutableIntStateOf(2) }

    // Load saved mode and decision timeout on first composition
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val savedMode = SettingsRepository.modeFlow(context).first()
        if (savedMode is Mode.SplitIntoGroups) {
            groupSizeState.intValue = savedMode.groupSize
        }
        vm.setMode(savedMode)

        val timeoutSec = SettingsRepository.decisionTimeoutSecondsFlow(context).first()
        vm.setDecisionTimeoutSeconds(timeoutSec)
    }

    // Result overlay animation controls
    val resultProgress = remember { Animatable(0f) }
    val showOverlay = remember { androidx.compose.runtime.mutableStateOf(false) }

    // Haptic when results are ready / countdown finished
    androidx.compose.runtime.LaunchedEffect(result) {
        if (result != null) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            showOverlay.value = true
            resultProgress.snapTo(0f)
            resultProgress.animateTo(1f, animationSpec = tween(1200))
            kotlinx.coroutines.delay(800)
            showOverlay.value = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (!inputLocked) Modifier.pointerInput(Unit) {
                    trackMultiTouch(
                        points = points,
                        onChanged = { updated -> vm.updateActive(updated) },
                        onFingerAdded = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove) },
                        onFingerRemoved = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) }
                    )
                } else Modifier.pointerInput(result) {
                    // When result is known, allow long-press to reset, but only if a NEW finger is placed after the result is shown
                    if (result != null) {
                        awaitEachGesture {
                            // Wait for a new down event (ignore already pressed pointers)
                            var startTime: Long? = null
                            while (true) {
                                val e = awaitPointerEvent(PointerEventPass.Main)
                                val newDown = e.changes.any { it.changedToDown() }
                                if (startTime == null && newDown) {
                                    startTime = System.currentTimeMillis()
                                }
                                // If we haven't started (no new down yet), continue waiting
                                if (startTime == null) continue
                                // After started, if all pointers are up -> cancel
                                val anyPressed = e.changes.any { it.pressed }
                                if (!anyPressed) break
                                val elapsed = System.currentTimeMillis() - startTime
                                if (elapsed >= TouchViewModel.LONG_PRESS_RESET_MS) {
                                    vm.reset()
                                    points.clear()
                                    break
                                }
                            }
                        }
                    }
                }
            )
    ) {
        // Pulsation: active only while countdown is running
        val isCountingDown = (remaining != null && remaining > 0)
        val infinite = rememberInfiniteTransition(label = "pulsate")
        val pulse by infinite.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseFactor"
        )
        val pulseFactor = if (isCountingDown) pulse else 1f
        // Mode controls are hidden behind a settings (gear) button (moved to bottom-left)
        val modeMenuExpanded = remember { androidx.compose.runtime.mutableStateOf(false) }
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(Color(0x33000000), shape = CircleShape)
                    .padding(8.dp)
                    .clickable { modeMenuExpanded.value = true }
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Modes / Settings", tint = Color.White)
                Text(
                    text = when (mode) {
                        is Mode.ChooseOne -> "Choose One"
                        is Mode.DefineOrder -> "Play Order"
                        is Mode.SplitIntoGroups -> "Groups(${mode.groupSize})"
                    },
                    color = Color.White
                )
            }

            androidx.compose.material3.DropdownMenu(
                expanded = modeMenuExpanded.value,
                onDismissRequest = { modeMenuExpanded.value = false }
            ) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Choose One") },
                    onClick = {
                        vm.setMode(Mode.ChooseOne)
                        scope.launch { SettingsRepository.saveMode(context, Mode.ChooseOne) }
                        vm.reset()
                        points.clear()
                        modeMenuExpanded.value = false
                    },
                    enabled = true
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Play Order") },
                    onClick = {
                        vm.setMode(Mode.DefineOrder)
                        scope.launch { SettingsRepository.saveMode(context, Mode.DefineOrder) }
                        vm.reset()
                        points.clear()
                        modeMenuExpanded.value = false
                    },
                    enabled = true
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Groups") },
                    onClick = {
                        val m = Mode.SplitIntoGroups(groupSizeState.intValue)
                        vm.setMode(m)
                        scope.launch { SettingsRepository.saveMode(context, m) }
                        vm.reset()
                        points.clear()
                        // keep menu open to allow adjusting size if desired
                    },
                    enabled = true
                )

                // Show group size controls ONLY when group mode is selected
                if (mode is Mode.SplitIntoGroups) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { if (groupSizeState.intValue > 1) { groupSizeState.intValue -= 1; val m = Mode.SplitIntoGroups(groupSizeState.intValue); vm.setMode(m); scope.launch { SettingsRepository.saveMode(context, m) }; vm.reset(); points.clear() } }, enabled = true) { Text("-") }
                                Text("Group size: ${groupSizeState.intValue}")
                                Button(onClick = { if (groupSizeState.intValue < 9) { groupSizeState.intValue += 1; val m = Mode.SplitIntoGroups(groupSizeState.intValue); vm.setMode(m); scope.launch { SettingsRepository.saveMode(context, m) }; vm.reset(); points.clear() } }, enabled = true) { Text("+") }
                            }
                        },
                        onClick = { /* no-op */ },
                        enabled = true
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
                    val color = when {
                        !inputLocked -> {
                            // Assign per-finger color from current palette, rotating when palette is exhausted
                            val existing = fingerColors[id]
                            if (existing != null) existing else {
                                val idx = nextColorIndexState.intValue % palette.colors.size.coerceAtLeast(1)
                                val c = palette.colors.getOrElse(idx) { Color(0xFF00E5FF) }
                                fingerColors[id] = c
                                nextColorIndexState.intValue = nextColorIndexState.intValue + 1
                                c
                            }
                        }
                        groupsMap != null -> groupColors[groupsMap[id]!! % groupColors.size]
                        winnerId != null && id == winnerId -> Color(0xFF4CAF50)
                        winnerId != null -> Color(0xFF444444)
                        else -> Color(0xFF4CAF50)
                    }
                    val currentRadius = 110f * pulseFactor
                    drawCircle(
                        color = color,
                        radius = currentRadius,
                        center = pos
                    )
                    // If order mode, draw the number label inside the circle (centered)
                    val num = orderMap?.get(id)
                    if (num != null) {
                        val paint = AndroidPaint().apply {
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
                            paint
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
                        drawCircle(color = color, radius = r, center = center)
                    }
                    is Result.Order -> {
                        val firstId = result.order.firstOrNull()
                        val center = firstId?.let { fid -> snapshot?.get(fid) } ?: Offset(this.size.width / 2f, this.size.height / 2f)
                        val maxRadius = hypot(this.size.width.toDouble(), this.size.height.toDouble()).toFloat()
                        val r = maxRadius * resultProgress.value
                        val color = firstId?.let { fid -> fingerColors[fid] } ?: palette.colors.firstOrNull() ?: Color(0xFF2196F3)
                        drawCircle(color = color, radius = r, center = center)
                    }
                    is Result.Groups -> {
                        val h = this.size.height * resultProgress.value
                        val color = palette.colors.firstOrNull() ?: Color(0xFF2196F3)
                        drawRect(color = color, size = androidx.compose.ui.geometry.Size(this.size.width, h))
                    }
                }
            }
        }
        // Back control as a cross icon in bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .background(Color(0x33000000), shape = CircleShape)
                .clickable { onBack() }
                .padding(12.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}

private suspend fun PointerInputScope.trackMultiTouch(
    points: MutableMap<Long, Offset>,
    onChanged: (Map<Long, Offset>) -> Unit,
    onFingerAdded: (() -> Unit)? = null,
    onFingerRemoved: (() -> Unit)? = null
) {
    awaitEachGesture {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            event.changes.forEach { change: PointerInputChange ->
                val id = change.id.value
                if (change.changedToDown()) {
                    if (points.size < 10) {
                        points[id] = change.position
                        onFingerAdded?.invoke()
                    }
                } else if (change.changedToUp() || change.isConsumed) {
                    if (points.containsKey(id)) {
                        points.remove(id)
                        onFingerRemoved?.invoke()
                    }
                } else if (change.pressed) {
                    points[id] = change.position
                }
            }
            onChanged(points.toMap())
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val current = PaletteRepository.current.collectAsState().value
    val expandedState = remember { androidx.compose.runtime.mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val timeoutSecState = SettingsRepository
        .decisionTimeoutSecondsFlow(context)
        .collectAsState(initial = 3)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Select color palette", fontWeight = FontWeight.Bold)

            // Current selection preview (fixed-size stripes)
            ColorStripes(colors = current.colors)

            // Simple combobox using Button + DropdownMenu
            Box {
                // Non-button trigger styled as a surface-like clickable area
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clickable { expandedState.value = true }
                ) {
                    // Show compact stripes next to the name to hint it is a selector
                    ColorStripes(colors = current.colors, modifier = Modifier.size(width = 60.dp, height = 24.dp))
                    Text(current.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = expandedState.value,
                    onDismissRequest = { expandedState.value = false }
                ) {
                    Palettes.All.forEach { palette ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ColorStripes(colors = palette.colors, modifier = Modifier.size(width = 60.dp, height = 24.dp))
                                    Text(palette.name, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            onClick = {
                                PaletteRepository.setPalette(palette)
                                scope.launch { SettingsRepository.savePalette(context, palette) }
                                expandedState.value = false
                            }
                        )
                    }
                }
            }

            // Decision timeout setting (1..10 seconds)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Decision timeout:", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                Button(onClick = {
                    val newVal = (timeoutSecState.value - 1).coerceIn(1, 10)
                    scope.launch { SettingsRepository.saveDecisionTimeoutSeconds(context, newVal) }
                }) { Text("-") }
                Text("${timeoutSecState.value}s", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                Button(onClick = {
                    val newVal = (timeoutSecState.value + 1).coerceIn(1, 10)
                    scope.launch { SettingsRepository.saveDecisionTimeoutSeconds(context, newVal) }
                }) { Text("+") }
            }

        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .background(Color(0x33000000), shape = CircleShape)
                .clickable { onBack() }
                .padding(12.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}

@Composable
private fun ColorStripes(colors: List<Color>, modifier: Modifier = Modifier) {
    // Fixed-size preview area: width 160dp, height 16dp, divided into equal rectangular stripes
    Row(
        modifier = modifier
            .size(width = 160.dp, height = 16.dp)
            .background(Color.DarkGray.copy(alpha = 0.2f)),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val shown = colors.ifEmpty { listOf(Color.Gray) }
        val maxShown = 10
        shown.take(maxShown).forEach { c ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(c)
            )
        }
        // If fewer than maxShown, fill the rest with transparent space equally to keep layout stable
        repeat((maxShown - shown.size.coerceAtMost(maxShown))) {
            Box(modifier = Modifier.weight(1f).fillMaxSize())
        }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Place fingers on the screen. App will choose, group, or order after stabilization.")
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .background(Color(0x33000000), shape = CircleShape)
                .clickable { onBack() }
                .padding(12.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}