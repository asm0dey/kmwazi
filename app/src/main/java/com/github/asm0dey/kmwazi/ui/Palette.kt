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
            Color(0xFFEF5350), // Red
            Color(0xFF42A5F5), // Blue
            Color(0xFF66BB6A), // Green
            Color(0xFFFFCA28), // Amber
            Color(0xFFAB47BC), // Purple
            Color(0xFFFF7043), // Deep Orange
            Color(0xFF26C6DA), // Cyan
            Color(0xFF7E57C2), // Deep Purple
            Color(0xFFFFD54F), // Yellow
            Color(0xFF26A69A), // Teal
        )
    )

    val Pastel = Palette(
        name = "Pastel",
        colors = listOf(
            Color(0xFFFFCDD2), // light red
            Color(0xFFBBDEFB), // light blue
            Color(0xFFC8E6C9), // light green
            Color(0xFFFFF9C4), // light yellow
            Color(0xFFE1BEE7), // light purple
            Color(0xFFFFE0B2), // light orange
            Color(0xFFB2EBF2), // light cyan
            Color(0xFFD1C4E9), // light deep purple
            Color(0xFFFFECB3), // light amber
            Color(0xFFB2DFDB), // light teal
        )
    )

    val Colorblind = Palette(
        name = "Colorblind",
        colors = listOf(
            Color(0xFF0072B2), // Blue
            Color(0xFFD55E00), // Vermillion
            Color(0xFF009E73), // Green
            Color(0xFFE69F00), // Orange
            Color(0xFFCC79A7), // Reddish purple
            Color(0xFF56B4E9), // Sky blue
            Color(0xFFF0E442), // Yellow
            Color(0xFF999999), // Grey
        )
    )

    val All = listOf(Vibrant, Pastel, Colorblind)
}
