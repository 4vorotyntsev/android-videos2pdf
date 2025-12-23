package com.vs.videoscanpdf.ui.navigation

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    // Initial flow
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    
    // Main tabs (bottom navigation)
    data object Main : Screen("main")
    data object Home : Screen("home")
    data object Projects : Screen("projects")
    data object Help : Screen("help")
    
    // Scanning flow
    data object Recorder : Screen("recorder/{projectId}") {
        fun createRoute(projectId: String) = "recorder/$projectId"
    }
    data object FramePicker : Screen("frame_picker/{projectId}") {
        fun createRoute(projectId: String) = "frame_picker/$projectId"
    }
    data object Processing : Screen("processing/{projectId}") {
        fun createRoute(projectId: String) = "processing/$projectId"
    }
    data object PageReview : Screen("page_review/{projectId}") {
        fun createRoute(projectId: String) = "page_review/$projectId"
    }
    data object SinglePageEditor : Screen("single_page_editor/{projectId}/{pageId}") {
        fun createRoute(projectId: String, pageId: String) = "single_page_editor/$projectId/$pageId"
    }
    data object Reorder : Screen("reorder/{projectId}") {
        fun createRoute(projectId: String) = "reorder/$projectId"
    }
    
    // Legacy page editor (list-based)
    data object PageEditor : Screen("page_editor/{projectId}") {
        fun createRoute(projectId: String) = "page_editor/$projectId"
    }
    
    data object Export : Screen("export/{projectId}") {
        fun createRoute(projectId: String) = "export/$projectId"
    }
    data object Settings : Screen("settings")
    
    companion object {
        const val PROJECT_ID_ARG = "projectId"
        const val PAGE_ID_ARG = "pageId"
    }
}
