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

package com.github.asm0dey.kmwazi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.asm0dey.kmwazi.di.ServiceLocator
import com.github.asm0dey.kmwazi.ui.PaletteRepository
import com.github.asm0dey.kmwazi.ui.screens.HelpScreen
import com.github.asm0dey.kmwazi.ui.screens.HomeScreen
import com.github.asm0dey.kmwazi.ui.screens.SettingsScreen
import com.github.asm0dey.kmwazi.ui.screens.TouchScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Touch : Screen("touch")
    object Settings : Screen("settings")
    object Help : Screen("help")
}

@Composable
fun KmwaziNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val settingsRepository = ServiceLocator.settingsRepository
    val palette by settingsRepository.paletteFlow().collectAsState(initial = com.github.asm0dey.kmwazi.ui.Palettes.Vibrant)

    // Apply saved palette as early as possible
    LaunchedEffect(palette) {
        PaletteRepository.setPalette(palette)
    }

    NavHost(navController = navController, startDestination = Screen.Home.route, modifier = modifier) {
        composable(Screen.Home.route) { HomeScreen(onNavigate = { navController.navigate(it) }) }
        composable(Screen.Touch.route) {
            TouchScreen(onBack = {
                // Always lead to Home; never close the app
                if (!navController.popBackStack(Screen.Home.route, false)) {
                    navController.navigate(Screen.Home.route) {
                        // Remove the current Touch from back stack so Home becomes top
                        popUpTo(Screen.Touch.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = {
                // Always lead to Home; never close the app
                if (!navController.popBackStack(Screen.Home.route, false)) {
                    navController.navigate(Screen.Home.route) {
                        // Remove Settings from back stack
                        popUpTo(Screen.Settings.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            })
        }
        composable(Screen.Help.route) {
            HelpScreen(onBack = {
                // Always lead to Home; never close the app
                if (!navController.popBackStack(Screen.Home.route, false)) {
                    navController.navigate(Screen.Home.route) {
                        // Remove Help from back stack
                        popUpTo(Screen.Help.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            })
        }
    }
}
