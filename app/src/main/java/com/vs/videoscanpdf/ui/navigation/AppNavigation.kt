package com.vs.videoscanpdf.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vs.videoscanpdf.ui.export.ExportScreen
import com.vs.videoscanpdf.ui.home.HomeScreen
import com.vs.videoscanpdf.ui.editor.PageEditorScreen
import com.vs.videoscanpdf.ui.picker.FramePickerScreen
import com.vs.videoscanpdf.ui.recorder.RecorderScreen
import com.vs.videoscanpdf.ui.settings.SettingsScreen

/**
 * Main navigation host for the app.
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // Home screen
        composable(Screen.Home.route) {
            HomeScreen(
                onRecordClick = { projectId ->
                    navController.navigate(Screen.Recorder.createRoute(projectId))
                },
                onImportClick = { projectId ->
                    navController.navigate(Screen.FramePicker.createRoute(projectId))
                },
                onProjectClick = { projectId ->
                    navController.navigate(Screen.FramePicker.createRoute(projectId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        // Recorder screen
        composable(
            route = Screen.Recorder.route,
            arguments = listOf(
                navArgument(Screen.PROJECT_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Screen.PROJECT_ID_ARG) ?: return@composable
            RecorderScreen(
                projectId = projectId,
                onRecordingComplete = {
                    navController.navigate(Screen.FramePicker.createRoute(projectId)) {
                        popUpTo(Screen.Recorder.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Frame Picker screen
        composable(
            route = Screen.FramePicker.route,
            arguments = listOf(
                navArgument(Screen.PROJECT_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Screen.PROJECT_ID_ARG) ?: return@composable
            FramePickerScreen(
                projectId = projectId,
                onContinue = {
                    navController.navigate(Screen.PageEditor.createRoute(projectId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Page Editor screen
        composable(
            route = Screen.PageEditor.route,
            arguments = listOf(
                navArgument(Screen.PROJECT_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Screen.PROJECT_ID_ARG) ?: return@composable
            PageEditorScreen(
                projectId = projectId,
                onExport = {
                    navController.navigate(Screen.Export.createRoute(projectId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Export screen
        composable(
            route = Screen.Export.route,
            arguments = listOf(
                navArgument(Screen.PROJECT_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Screen.PROJECT_ID_ARG) ?: return@composable
            ExportScreen(
                projectId = projectId,
                onExportComplete = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Settings screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
