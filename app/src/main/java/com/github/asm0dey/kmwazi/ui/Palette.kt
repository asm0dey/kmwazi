package com.github.asm0dey.kmwazi.ui

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Simple in-memory palette store for the current session
object PaletteRepository {
    private val _current = MutableStateFlow(Palettes.Vibrant)
    val current: StateFlow<Palette> get() = _current

    fun setPalette(palette: Palette) {
        _current.value = palette
    }
}

// Palette model and predefined palettes
data class Palette(val name: String, val colors: List<Color>)

object Palettes {
    val Vibrant = Palette(
        name = "Vibrant",
        colors = listOf(
            Color(0xffff4d4d), // Red
            Color(0xFFff8052), // Blue
            Color(0xFFffb347), // Green
            Color(0xFFffd900), // Amber
            Color(0xFF80ff00), // Purple
            Color(0xFF00fa9a), // Deep Orange
            Color(0xFF00bfff), // Cyan
            Color(0xFF1f8fff), // Deep Purple
            Color(0xFF8a2ce2), // Yellow
            Color(0xFFff6bb5), // Teal
        )
    )

    val Pastel = Palette(
        name = "Pastel",
        colors = listOf(
            Color(0xffffd1dc), // light red
            Color(0xffffb3ba), // light blue
            Color(0xffffdeb8), // light green
            Color(0xffffffb8), // light yellow
            Color(0xffb8ffc7), // light purple
            Color(0xffb8e0ff), // light orange
            Color(0xffd6bce1), // light cyan
            Color(0xfff9c8cb), // light deep purple
            Color(0xfff7e2d9), // light amber
            Color(0xffc1e1c1), // light teal
        )
    )

    val Lucid = Palette(
        name = "Lucid",
        colors = listOf(
            Color(0xffa3d2ca),
            Color(0xff70a9a1),
            Color(0xff4b7c5a),
            Color(0xfff4d35d),
            Color(0xffee9649),
            Color(0xfff95939),
            Color(0xffd92632),
            Color(0xff9e1031),
            Color(0xff560b0e),
            Color(0xffa5b4ef),
        )
    )

    val Colorblind = Palette(
        name = "Colorblind",
        colors = listOf(
            Color(0xff7d7d28), // Blue
            Color(0xffa5a557), // Vermillion
            Color(0xffbbbb6d), // Green
            Color(0xffcfcf83), // Orange
            Color(0xffe2e2a3), // Reddish purple
            Color(0xfff5f5c3), // Sky blue
            Color(0xffc7c7ab), // Yellow
            Color(0xFF989894), // Grey
            Color(0xff7b7b92), // Grey
            Color(0xff5a5a8a), // Grey
        )
    )

    val All = listOf(Vibrant, Pastel, Lucid, Colorblind)
}
