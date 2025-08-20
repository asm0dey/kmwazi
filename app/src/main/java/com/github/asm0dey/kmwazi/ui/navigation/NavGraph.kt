package com.github.asm0dey.kmwazi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.asm0dey.kmwazi.data.SettingsRepository
import com.github.asm0dey.kmwazi.ui.PaletteRepository
import com.github.asm0dey.kmwazi.ui.screens.HelpScreen
import com.github.asm0dey.kmwazi.ui.screens.HomeScreen
import com.github.asm0dey.kmwazi.ui.screens.SettingsScreen
import com.github.asm0dey.kmwazi.ui.screens.TouchScreen

object Routes {
    const val Home = "home"
    const val Touch = "touch"
    const val Settings = "settings"
    const val Help = "help"
}

@Composable
fun KmwaziNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val hasSettings by SettingsRepository.hasAnySettingsFlow(context).collectAsState(initial = false)
    val palette by SettingsRepository.paletteFlow(context).collectAsState(initial = com.github.asm0dey.kmwazi.ui.Palettes.Vibrant)
    val navigated = remember { androidx.compose.runtime.mutableStateOf(false) }

    // Apply saved palette as early as possible
    LaunchedEffect(palette) {
        PaletteRepository.setPalette(palette)
    }

    // If there are saved settings, auto-navigate to Touch once
    LaunchedEffect(hasSettings) {
        if (hasSettings && !navigated.value) {
            navigated.value = true
            navController.navigate(Routes.Touch) {
                popUpTo(Routes.Home) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.Home, modifier = modifier) {
        composable(Routes.Home) { HomeScreen(onNavigate = { navController.navigate(it) }) }
        composable(Routes.Touch) {
            TouchScreen(onBack = {
                // Always lead to Home; never close the app
                if (!navController.popBackStack(Routes.Home, false)) {
                    navController.navigate(Routes.Home) {
                        // Remove the current Touch from back stack so Home becomes top
                        popUpTo(Routes.Touch) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            })
        }
        composable(Routes.Settings) {
            SettingsScreen(onBack = {
                // Always lead to Home; never close the app
                if (!navController.popBackStack(Routes.Home, false)) {
                    navController.navigate(Routes.Home) {
                        // Remove Settings from back stack
                        popUpTo(Routes.Settings) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            })
        }
        composable(Routes.Help) {
            HelpScreen(onBack = {
                // Always lead to Home; never close the app
                if (!navController.popBackStack(Routes.Home, false)) {
                    navController.navigate(Routes.Home) {
                        // Remove Help from back stack
                        popUpTo(Routes.Help) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            })
        }
    }
}