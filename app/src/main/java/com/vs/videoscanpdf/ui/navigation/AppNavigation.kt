package com.vs.videoscanpdf.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vs.videoscanpdf.ui.export.ExportResultScreen
import com.vs.videoscanpdf.ui.export.ExportSetupScreen
import com.vs.videoscanpdf.ui.home.HomeScreen
import com.vs.videoscanpdf.ui.videoimport.ImportVideoScreen
import com.vs.videoscanpdf.ui.pagepicker.ManualPagePickerScreen
import com.vs.videoscanpdf.ui.editor.SinglePageEditorScreen
import com.vs.videoscanpdf.ui.processing.ProcessingScreen
import com.vs.videoscanpdf.ui.recorder.RecorderScreen
import com.vs.videoscanpdf.ui.review.PageReviewScreen
import com.vs.videoscanpdf.ui.settings.SettingsScreen
import com.vs.videoscanpdf.ui.splash.SplashScreen
import com.vs.videoscanpdf.ui.onboarding.OnboardingScreen
import com.vs.videoscanpdf.ui.trim.TrimVideoScreen

/**
 * Main navigation host for the app.
 * 
 * Navigation flow:
 * 1. Splash -> Onboarding (first run) or Main
 * 2. Main has bottom nav: Home | Exports | Help
 * 3. Scanning flow (Manual Selection Edition):
 *    Recorder/Import -> Trim (optional) -> ManualPagePicker -> Processing -> ExportSetup -> ExportResult
 * 
 * Mode B behavior: Any back navigation during scanning flow discards the session.
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
        // ===== Initial Flow =====
        
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
        
        // ===== Main Scaffold with Bottom Navigation =====
        
        composable(Screen.Main.route) {
            MainScaffold(
                onNavigateToRecorder = { sessionId ->
                    navController.navigate(Screen.Recorder.createRoute(sessionId))
                },
                onNavigateToImport = { sessionId ->
                    navController.navigate(Screen.ImportVideo.createRoute(sessionId))
                },
                onNavigateToTrim = { sessionId ->
                    navController.navigate(Screen.TrimVideo.createRoute(sessionId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToExportResult = { sessionId ->
                    navController.navigate(Screen.ExportResult.createRoute(sessionId))
                }
            )
        }
        
        // ===== Scanning Flow =====
        
        // Recorder screen
        composable(
            route = Screen.Recorder.route,
            arguments = listOf(
                navArgument(Screen.SESSION_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Screen.SESSION_ID_ARG) ?: return@composable
            RecorderScreen(
                sessionId = sessionId,
                onRecordingComplete = {
                    // After recording, go to trim
                    navController.navigate(Screen.TrimVideo.createRoute(sessionId)) {
                        popUpTo(Screen.Recorder.route) { inclusive = true }
                    }
                },
                onBack = { 
                    // Mode B: discard session on back
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                }
            )
        }
        
        // Import video screen
        composable(
            route = Screen.ImportVideo.route,
            arguments = listOf(
                navArgument(Screen.SESSION_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Screen.SESSION_ID_ARG) ?: return@composable
            ImportVideoScreen(
                sessionId = sessionId,
                onVideoSelected = {
                    // After import validation, go to trim
                    navController.navigate(Screen.TrimVideo.createRoute(sessionId)) {
                        popUpTo(Screen.ImportVideo.route) { inclusive = true }
                    }
                },
                onBack = {
                    // Mode B: discard session on back
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                }
            )
        }
        
        // Trim video screen
        composable(
            route = Screen.TrimVideo.route,
            arguments = listOf(
                navArgument(Screen.SESSION_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Screen.SESSION_ID_ARG) ?: return@composable
            TrimVideoScreen(
                sessionId = sessionId,
                onContinue = {
                    // Navigate to Manual Page Picker (Manual Selection Edition)
                    navController.navigate(Screen.ManualPagePicker.createRoute(sessionId)) {
                        popUpTo(Screen.TrimVideo.route) { inclusive = true }
                    }
                },
                onSkip = {
                    // Skip trim, go directly to manual page picker
                    navController.navigate(Screen.ManualPagePicker.createRoute(sessionId)) {
                        popUpTo(Screen.TrimVideo.route) { inclusive = true }
                    }
                },
                onBack = {
                    // Mode B: discard session on back
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                }
            )
        }
        
        // Manual page picker screen (Manual Selection Edition)
        composable(
            route = Screen.ManualPagePicker.route,
            arguments = listOf(
                navArgument(Screen.SESSION_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Screen.SESSION_ID_ARG) ?: return@composable
            ManualPagePickerScreen(
                sessionId = sessionId,
                onContinue = {
                    // Go directly to processing (enhances picked pages)
                    navController.navigate(Screen.Processing.createRoute(sessionId)) {
                        popUpTo(Screen.ManualPagePicker.route) { inclusive = true }
                    }
                },
                onReviewPages = {
                    // Go to optional page review for reordering/editing
                    navController.navigate(Screen.PageReview.createRoute(sessionId))
                },
                onBack = {
                    // Mode B: discard session on back
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                }
            )
        }
        
        // Processing screen
        composable(
            route = Screen.Processing.route,
            arguments = listOf(
                navArgument(Screen.SESSION_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Screen.SESSION_ID_ARG) ?: return@composable
            ProcessingScreen(
                sessionId = sessionId,
                onComplete = {
                    navController.navigate(Screen.ExportSetup.createRoute(sessionId)) {
                        popUpTo(Screen.Processing.route) { inclusive = true }
                    }
                },
                onCancel = {
                    // Mode B: discard session on cancel
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                }
            )
        }
        
        // Page review screen (optional - for reordering, editing, deleting pages)
        composable(
            route = Screen.PageReview.route,
            arguments = listOf(
                navArgument(Screen.SESSION_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Screen.SESSION_ID_ARG) ?: return@composable
            PageReviewScreen(
                sessionId = sessionId,
                onContinue = {
                    // Go to processing to enhance the selected pages
                    navController.navigate(Screen.Processing.createRoute(sessionId)) {
                        popUpTo(Screen.PageReview.route) { inclusive = true }
                    }
                },
                onEditPage = { pageId ->
                    navController.navigate(Screen.SinglePageEditor.createRoute(sessionId, pageId))
                },
                onBack = {
                    // Go back to manual page picker
                    navController.popBackStack()
                }
            )
        }
        
        // Single page editor
        composable(
            route = Screen.SinglePageEditor.route,
            arguments = listOf(
                navArgument(Screen.SESSION_ID_ARG) { type = NavType.StringType },
                navArgument(Screen.PAGE_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Screen.SESSION_ID_ARG) ?: return@composable
            val pageId = backStackEntry.arguments?.getString(Screen.PAGE_ID_ARG) ?: return@composable
            SinglePageEditorScreen(
                sessionId = sessionId,
                pageId = pageId,
                onSave = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Export setup screen
        composable(
            route = Screen.ExportSetup.route,
            arguments = listOf(
                navArgument(Screen.SESSION_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Screen.SESSION_ID_ARG) ?: return@composable
            ExportSetupScreen(
                sessionId = sessionId,
                onExportComplete = {
                    navController.navigate(Screen.ExportResult.createRoute(sessionId)) {
                        popUpTo(Screen.ExportSetup.route) { inclusive = true }
                    }
                },
                onBack = {
                    // Go back to previous screen
                    navController.popBackStack()
                }
            )
        }
        
        // Export result screen
        composable(
            route = Screen.ExportResult.route,
            arguments = listOf(
                navArgument(Screen.SESSION_ID_ARG) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Screen.SESSION_ID_ARG) ?: return@composable
            ExportResultScreen(
                sessionId = sessionId,
                onCreateAnother = {
                    // Complete session and go home
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                },
                onDone = {
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                }
            )
        }
        
        // ===== Settings =====
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
