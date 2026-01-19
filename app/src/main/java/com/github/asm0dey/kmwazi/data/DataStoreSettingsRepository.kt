package com.github.asm0dey.kmwazi.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.asm0dey.kmwazi.domain.Mode
import com.github.asm0dey.kmwazi.ui.Palette
import com.github.asm0dey.kmwazi.ui.Palettes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * DataStore-based implementation of SettingsRepositoryInterface.
 * Stores user preferences for palette, mode, and timeout settings.
 */
class DataStoreSettingsRepository(private val context: Context) : SettingsRepositoryInterface {
    private val KEY_MODE = stringPreferencesKey("mode")
    private val KEY_GROUP_SIZE = intPreferencesKey("group_size")
    private val KEY_PALETTE = stringPreferencesKey("palette_name")
    private val KEY_DECISION_TIMEOUT_SEC = intPreferencesKey("decision_timeout_sec")
    private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")

    override fun paletteFlow(): Flow<Palette> =
        context.dataStore.data.map { prefs ->
            val name = prefs[KEY_PALETTE]
            when (name) {
                Palettes.Pastel.name -> Palettes.Pastel
                Palettes.Colorblind.name -> Palettes.Colorblind
                Palettes.Vibrant.name -> Palettes.Vibrant
                else -> Palettes.Vibrant
            }
        }

    override fun modeFlow(): Flow<Mode> =
        context.dataStore.data.map { prefs ->
            when (prefs[KEY_MODE]) {
                Mode.ChooseOne.toString() -> Mode.ChooseOne
                Mode.DefineOrder.toString() -> Mode.DefineOrder
                "groups" -> {
                    val gs = (prefs[KEY_GROUP_SIZE] ?: 2).coerceIn(1, 9)
                    Mode.SplitIntoGroups(gs)
                }
                else -> Mode.ChooseOne
            }
        }

    override fun decisionTimeoutSecondsFlow(): Flow<Int> =
        context.dataStore.data.map { prefs ->
            (prefs[KEY_DECISION_TIMEOUT_SEC] ?: 3).coerceIn(1, 10)
        }

    override suspend fun savePalette(palette: Palette) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PALETTE] = palette.name
        }
    }

    override suspend fun saveMode(mode: Mode) {
        context.dataStore.edit { prefs ->
            when (mode) {
                is Mode.ChooseOne -> {
                    prefs[KEY_MODE] = mode.toString()
                }
                is Mode.DefineOrder -> {
                    prefs[KEY_MODE] = mode.toString()
                }
                is Mode.SplitIntoGroups -> {
                    prefs[KEY_MODE] = "groups"
                    prefs[KEY_GROUP_SIZE] = mode.groupSize.coerceIn(1, 9)
                }
            }
        }
    }

    override suspend fun saveDecisionTimeoutSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(1, 10)
        context.dataStore.edit { prefs ->
            prefs[KEY_DECISION_TIMEOUT_SEC] = clamped
        }
    }

    // Legacy method for backward compatibility (if needed)
    fun hasAnySettingsFlow(): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs.contains(KEY_MODE) ||
                prefs.contains(KEY_PALETTE) ||
                prefs.contains(KEY_DECISION_TIMEOUT_SEC) ||
                prefs.contains(KEY_DARK_THEME)
        }
}
