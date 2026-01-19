package com.github.asm0dey.kmwazi.data

import com.github.asm0dey.kmwazi.domain.Mode
import com.github.asm0dey.kmwazi.ui.Palette
import kotlinx.coroutines.flow.Flow

/**
 * Interface for settings persistence, abstracted to allow testing with fakes.
 */
interface SettingsRepositoryInterface {
    /**
     * Flow of the current palette selection.
     */
    fun paletteFlow(): Flow<Palette>

    /**
     * Flow of the current mode selection.
     */
    fun modeFlow(): Flow<Mode>

    /**
     * Flow of the decision timeout in seconds (1-10).
     */
    fun decisionTimeoutSecondsFlow(): Flow<Int>

    /**
     * Saves the palette selection.
     */
    suspend fun savePalette(palette: Palette)

    /**
     * Saves the mode selection.
     */
    suspend fun saveMode(mode: Mode)

    /**
     * Saves the decision timeout in seconds.
     */
    suspend fun saveDecisionTimeoutSeconds(seconds: Int)
}
