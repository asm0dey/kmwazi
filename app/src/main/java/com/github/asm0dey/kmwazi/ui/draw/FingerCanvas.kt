package com.github.asm0dey.kmwazi.ui.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
    val orderMap by remember(result) {
        derivedStateOf {
            (result as? Result.Order)?.order?.withIndex()?.associate { it.value to (it.index + 1) }
        }
    }
    val groupsMap by remember(result) {
        derivedStateOf {
            (result as? Result.Groups)?.groups?.withIndex()?.flatMap { (gi, g) -> g.map { it to gi } }?.toMap()
        }
    }
    val paletteSize = paletteColors.size.coerceAtLeast(1)

    Canvas(modifier = modifier.fillMaxSize()) {
        var count = 0
        val currentGroupsMap = groupsMap
        val currentOrderMap = orderMap
        points.forEach { (id, pos) ->
            if (count < 10) {
                val color = when {
                    currentGroupsMap != null -> paletteColors[currentGroupsMap[id]!! % paletteSize]
                    winnerId != null && id == winnerId -> fingerColors[id] ?: paletteColors[0]
                    winnerId != null -> Color.DarkGray
                    currentOrderMap != null -> fingerColors[id] ?: paletteColors[id.toInt() % paletteSize]
                    else -> fingerColors[id] ?: paletteColors[id.toInt() % paletteSize]
                }

                val currentRadius = 110f * pulseFactor
                drawCircle(
                    color = color,
                    radius = currentRadius,
                    center = pos,
                )

                // If order mode, draw the number label inside the circle
                val num = currentOrderMap?.get(id)
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
