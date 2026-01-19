package com.github.asm0dey.kmwazi.data

import com.github.asm0dey.kmwazi.domain.Mode
import com.github.asm0dey.kmwazi.ui.Palette
import com.github.asm0dey.kmwazi.ui.Palettes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of SettingsRepositoryInterface for testing.
 * Uses in-memory StateFlows instead of DataStore.
 */
class FakeSettingsRepository : SettingsRepositoryInterface {
    private val _palette = MutableStateFlow<Palette>(Palettes.Vibrant)
    private val _mode = MutableStateFlow<Mode>(Mode.ChooseOne)
    private val _timeout = MutableStateFlow(3)

    override fun paletteFlow(): Flow<Palette> = _palette.asStateFlow()

    override fun modeFlow(): Flow<Mode> = _mode.asStateFlow()

    override fun decisionTimeoutSecondsFlow(): Flow<Int> = _timeout.asStateFlow()

    override suspend fun savePalette(palette: Palette) {
        _palette.value = palette
    }

    override suspend fun saveMode(mode: Mode) {
        _mode.value = mode
    }

    override suspend fun saveDecisionTimeoutSeconds(seconds: Int) {
        _timeout.value = seconds.coerceIn(1, 10)
    }

    // Helper for tests to reset state
    fun reset() {
        _palette.value = Palettes.Vibrant
        _mode.value = Mode.ChooseOne
        _timeout.value = 3
    }
}
