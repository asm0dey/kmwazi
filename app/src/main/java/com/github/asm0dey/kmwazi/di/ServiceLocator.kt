/*
 * kmwazi
 *
 * Copyright (C) 2025 asm0dey <pavel.finkelshtein+kmwazi@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 */

package com.github.asm0dey.kmwazi.di

import android.content.Context
import com.github.asm0dey.kmwazi.data.SettingsRepository
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
    private var _settingsRepository: SettingsRepository? = null
    private var _resultEngine: ResultEngine? = null

    /**
     * Initializes the ServiceLocator with production dependencies.
     * Should be called once at application startup.
     */
    fun initialize(context: Context) {
        val randomProvider = SecureRandomProvider()
        _resultEngine = ResultEngine(randomProvider)
        _settingsRepository = SettingsRepository(context.applicationContext)
    }

    /**
     * Returns the SettingsRepository instance.
     * @throws IllegalStateException if ServiceLocator hasn't been initialized
     */
    val settingsRepository: SettingsRepository
        get() = _settingsRepository ?: error("ServiceLocator not initialized. Call initialize() first.")

    /**
     * Returns the ResultEngine instance.
     * @throws IllegalStateException if ServiceLocator hasn't been initialized
     */
    val resultEngine: ResultEngine
        get() = _resultEngine ?: error("ServiceLocator not initialized. Call initialize() first.")

}
