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
import com.vs.videoscanpdf.ui.splash.SplashScreen
import com.vs.videoscanpdf.ui.onboarding.OnboardingScreen
import com.vs.videoscanpdf.ui.processing.ProcessingScreen
import com.vs.videoscanpdf.ui.review.PageReviewScreen
import com.vs.videoscanpdf.ui.reorder.ReorderScreen
import com.vs.videoscanpdf.ui.editor.SinglePageEditorScreen

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
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        // Splash screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Onboarding screen
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Main scaffold with bottom navigation
        composable(Screen.Main.route) {
            MainScaffold(
                onNavigateToRecorder = { projectId ->
                    navController.navigate(Screen.Recorder.createRoute(projectId))
                },
                onNavigateToFramePicker = { projectId, videoUri ->
                    val encodedUri = java.net.URLEncoder.encode(videoUri, "UTF-8")
                    navController.navigate(Screen.FramePicker.createRoute(projectId) + "?videoUri=$encodedUri")
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToPageReview = { projectId ->
                    navController.navigate(Screen.PageReview.createRoute(projectId))
                },
                onNavigateToExport = { projectId ->
                    navController.navigate(Screen.Export.createRoute(projectId))
                }
            )
        }
        
        // Home screen (legacy route for direct access)
        composable(Screen.Home.route) {
            HomeScreen(
                onRecordClick = { projectId ->
                    navController.navigate(Screen.Recorder.createRoute(projectId))
                },
                onImportVideo = { projectId, videoUri ->
                    // Navigate to FramePicker with the imported video
                    // The video URI needs to be encoded for navigation
                    val encodedUri = java.net.URLEncoder.encode(videoUri, "UTF-8")
                    navController.navigate(Screen.FramePicker.createRoute(projectId) + "?videoUri=$encodedUri")
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
            route = Screen.FramePicker.route + "?videoUri={videoUri}",
            arguments = listOf(
                navArgument(Screen.PROJECT_ID_ARG) { type = NavType.StringType },
                navArgument("videoUri") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Screen.PROJECT_ID_ARG) ?: return@composable
            val videoUri = backStackEntry.arguments?.getString("videoUri")?.let {
                java.net.URLDecoder.decode(it, "UTF-8")
            }
            FramePickerScreen(
                projectId = projectId,
                importedVideoUri = videoUri,
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
                    navController.popBackStack(Screen.Main.route, inclusive = false)
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
        
        // Processing screen
        composable(
            route = Screen.Processing.route,
            arguments = listOf(
                navArgument(Screen.PROJECT_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Screen.PROJECT_ID_ARG) ?: return@composable
            ProcessingScreen(
                projectId = projectId,
                onComplete = {
                    navController.navigate(Screen.Export.createRoute(projectId)) {
                        popUpTo(Screen.Processing.route) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }
        
        // Page Review screen
        composable(
            route = Screen.PageReview.route,
            arguments = listOf(
                navArgument(Screen.PROJECT_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Screen.PROJECT_ID_ARG) ?: return@composable
            PageReviewScreen(
                projectId = projectId,
                onContinue = {
                    navController.navigate(Screen.Reorder.createRoute(projectId))
                },
                onEditPage = { pageId ->
                    navController.navigate(Screen.SinglePageEditor.createRoute(projectId, pageId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Reorder screen
        composable(
            route = Screen.Reorder.route,
            arguments = listOf(
                navArgument(Screen.PROJECT_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Screen.PROJECT_ID_ARG) ?: return@composable
            ReorderScreen(
                projectId = projectId,
                onExport = {
                    navController.navigate(Screen.Processing.createRoute(projectId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Single Page Editor screen
        composable(
            route = Screen.SinglePageEditor.route,
            arguments = listOf(
                navArgument(Screen.PROJECT_ID_ARG) { type = NavType.StringType },
                navArgument(Screen.PAGE_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Screen.PROJECT_ID_ARG) ?: return@composable
            val pageId = backStackEntry.arguments?.getString(Screen.PAGE_ID_ARG) ?: return@composable
            SinglePageEditorScreen(
                projectId = projectId,
                pageId = pageId,
                onSave = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
