package com.github.asm0dey.kmwazi.di

import android.content.Context
import com.github.asm0dey.kmwazi.data.DataStoreSettingsRepository
import com.github.asm0dey.kmwazi.data.SettingsRepositoryInterface
import com.github.asm0dey.kmwazi.domain.RandomProvider
import com.github.asm0dey.kmwazi.domain.ResultEngine
import com.github.asm0dey.kmwazi.domain.SecureRandomProvider

/**
 * Manual dependency injection container using the Service Locator pattern.
 * Provides singleton instances of dependencies for the application.
 *
 * Must be initialized in Application.onCreate() or MainActivity.onCreate()
 * before accessing any dependencies.
 */
object ServiceLocator {
    private var _settingsRepository: SettingsRepositoryInterface? = null
    private var _randomProvider: RandomProvider? = null
    private var _resultEngine: ResultEngine? = null

    /**
     * Initializes the ServiceLocator with production dependencies.
     * Should be called once at application startup.
     */
    fun initialize(context: Context) {
        _randomProvider = SecureRandomProvider()
        _resultEngine = ResultEngine(_randomProvider!!)
        _settingsRepository = DataStoreSettingsRepository(context.applicationContext)
    }

    /**
     * Returns the SettingsRepository instance.
     * @throws IllegalStateException if ServiceLocator hasn't been initialized
     */
    val settingsRepository: SettingsRepositoryInterface
        get() = _settingsRepository ?: error("ServiceLocator not initialized. Call initialize() first.")

    /**
     * Returns the ResultEngine instance.
     * @throws IllegalStateException if ServiceLocator hasn't been initialized
     */
    val resultEngine: ResultEngine
        get() = _resultEngine ?: error("ServiceLocator not initialized. Call initialize() first.")

    /**
     * Sets test dependencies for testing.
     * Only use this in test code.
     */
    fun setTestDependencies(
        settingsRepository: SettingsRepositoryInterface? = null,
        randomProvider: RandomProvider? = null,
    ) {
        settingsRepository?.let { _settingsRepository = it }
        randomProvider?.let {
            _randomProvider = it
            _resultEngine = ResultEngine(it)
        }
    }

    /**
     * Resets all dependencies.
     * Only use this in test code for cleanup.
     */
    fun reset() {
        _settingsRepository = null
        _randomProvider = null
        _resultEngine = null
    }
}
