package com.vs.videoscanpdf.ui.navigation

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Recorder : Screen("recorder/{projectId}") {
        fun createRoute(projectId: String) = "recorder/$projectId"
    }
    data object FramePicker : Screen("frame_picker/{projectId}") {
        fun createRoute(projectId: String) = "frame_picker/$projectId"
    }
    data object PageEditor : Screen("page_editor/{projectId}") {
        fun createRoute(projectId: String) = "page_editor/$projectId"
    }
    data object Export : Screen("export/{projectId}") {
        fun createRoute(projectId: String) = "export/$projectId"
    }
    data object Settings : Screen("settings")
    
    companion object {
        const val PROJECT_ID_ARG = "projectId"
    }
}
