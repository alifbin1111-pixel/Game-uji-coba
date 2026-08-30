package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.controller.ControllerEditorScreen
import com.example.ui.detail.GameDetailScreen
import com.example.ui.engine.GameRunningScreen
import com.example.ui.library.GameLibraryScreen
import com.example.ui.runtimes.RuntimeManagerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.translation.TranslationSettingsScreen
import com.example.viewmodel.GameBridgeViewModel

object Destinations {
    const val LIBRARY = "library"
    const val GAME_DETAIL = "game_detail/{gameId}"
    const val GAME_RUNNING = "game_running/{gameId}"
    const val RUNTIMES = "runtimes"
    const val TRANSLATION_SETTINGS = "translation_settings"
    const val CONTROLLER_EDITOR = "controller_editor"
    const val SETTINGS = "settings"

    fun gameDetail(gameId: String) = "game_detail/$gameId"
    fun gameRunning(gameId: String) = "game_running/$gameId"
}

@Composable
fun AppNavigation(
    viewModel: GameBridgeViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.LIBRARY
    ) {
        composable(Destinations.LIBRARY) {
            GameLibraryScreen(
                viewModel = viewModel,
                onSelectGame = { gameId ->
                    navController.navigate(Destinations.gameDetail(gameId))
                },
                onLaunchGame = { gameId ->
                    navController.navigate(Destinations.gameRunning(gameId))
                },
                onOpenRuntimes = {
                    navController.navigate(Destinations.RUNTIMES)
                },
                onOpenSettings = {
                    navController.navigate(Destinations.SETTINGS)
                },
                onOpenTranslationSettings = {
                    navController.navigate(Destinations.TRANSLATION_SETTINGS)
                },
                onOpenControllerEditor = {
                    navController.navigate(Destinations.CONTROLLER_EDITOR)
                }
            )
        }

        composable(
            route = Destinations.GAME_DETAIL,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            GameDetailScreen(
                gameId = gameId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLaunchGame = { targetGameId ->
                    navController.navigate(Destinations.gameRunning(targetGameId))
                },
                onOpenControllerEditor = {
                    navController.navigate(Destinations.CONTROLLER_EDITOR)
                }
            )
        }

        composable(
            route = Destinations.GAME_RUNNING,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            GameRunningScreen(
                gameId = gameId,
                viewModel = viewModel,
                onExitGame = {
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.RUNTIMES) {
            RuntimeManagerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.TRANSLATION_SETTINGS) {
            TranslationSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.CONTROLLER_EDITOR) {
            ControllerEditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
