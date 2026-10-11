package com.browxer.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.browxer.feature.browser.BrowserScreen
import com.browxer.feature.library.LibraryScreen
import com.browxer.feature.player.PlayerScreen
import com.browxer.feature.settings.SettingsScreen

@Composable
fun BrowxerNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "browser") {
        composable("browser") {
            BrowserScreen(
                onOpenLibrary = { navController.navigate("library") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("library") {
            LibraryScreen(
                onBack = { navController.popBackStack() },
                onPlay = { id -> navController.navigate("player/" + id) }
            )
        }
        composable(
            route = "player/{downloadId}",
            arguments = listOf(navArgument("downloadId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("downloadId") ?: 0L
            PlayerScreen(downloadId = id, onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
