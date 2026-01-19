package com.github.asm0dey.kmwazi.ui.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import com.github.asm0dey.kmwazi.domain.Result
import android.graphics.Paint as AndroidPaint

@Composable
fun FingerCanvas(
    points: Map<Long, Offset>,
    fingerColors: Map<Long, Color>,
    result: Result?,
    inputLocked: Boolean,
    pulseFactor: Float,
    paletteColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    // Reuse paint instances to avoid recreation
    val textPaint = remember {
        AndroidPaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textAlign = AndroidPaint.Align.CENTER
        }
    }

    val winnerId = (result as? Result.One)?.winnerId
    val orderMap = remember(result) {
        (result as? Result.Order)?.order?.withIndex()?.associate { it.value to (it.index + 1) }
    }
    val groupsMap = remember(result) {
        (result as? Result.Groups)?.groups?.withIndex()?.flatMap { (gi, g) -> g.map { it to gi } }?.toMap()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        var count = 0
        points.forEach { (id, pos) ->
            if (count < 10) {
                val color = when {
                    groupsMap != null -> paletteColors[groupsMap[id]!! % paletteColors.size]
                    !inputLocked -> fingerColors[id] ?: Color.Gray
                    winnerId != null && id == winnerId -> fingerColors[id] ?: Color.Green
                    winnerId != null -> Color.DarkGray
                    else -> Color.Green
                }

                val currentRadius = 110f * pulseFactor
                drawCircle(
                    color = color,
                    radius = currentRadius,
                    center = pos,
                )

                // If order mode, draw the number label inside the circle
                val num = orderMap?.get(id)
                if (num != null) {
                    textPaint.textSize = currentRadius * 0.6f
                    val baselineY = pos.y - (textPaint.descent() + textPaint.ascent()) / 2f
                    drawContext.canvas.nativeCanvas.drawText(
                        num.toString(),
                        pos.x,
                        baselineY,
                        textPaint,
                    )
                }
                count++
            }
        }
    }
}
