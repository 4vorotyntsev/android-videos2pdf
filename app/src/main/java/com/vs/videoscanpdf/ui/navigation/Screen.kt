package com.vs.videoscanpdf.ui.navigation

/**
 * Navigation routes for the app.
 * 
 * Flow structure:
 * Splash -> Onboarding (first run) -> Main
 * Main contains bottom nav: Home | Exports | Help
 * 
 * Scanning flow (Manual Selection Edition - ephemeral session):
 * Home -> Recorder/Import -> Trim (optional) -> ManualPagePicker -> (optional PageReview) -> Processing -> ExportSetup -> ExportResult
 * 
 * Key change: User manually scrubs video and picks pages (no auto-detection).
 */
sealed class Screen(val route: String) {
    // ===== Initial flow =====
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    
    // ===== Main tabs (bottom navigation) =====
    data object Main : Screen("main")
    data object Home : Screen("home")
    data object ExportsLibrary : Screen("exports_library")  // Renamed from Projects
    data object Help : Screen("help")
    
    // ===== Scanning flow screens =====
    
    // Camera recording
    data object Recorder : Screen("recorder/{sessionId}") {
        fun createRoute(sessionId: String) = "recorder/$sessionId"
    }
    
    // Video import
    data object ImportVideo : Screen("import_video/{sessionId}") {
        fun createRoute(sessionId: String) = "import_video/$sessionId"
    }
    
    // Trim video (new screen)
    data object TrimVideo : Screen("trim_video/{sessionId}") {
        fun createRoute(sessionId: String) = "trim_video/$sessionId"
    }
    
    // Manual page picker (user scrubs and picks pages)
    data object ManualPagePicker : Screen("manual_page_picker/{sessionId}") {
        fun createRoute(sessionId: String) = "manual_page_picker/$sessionId"
    }
    
    // Legacy: Auto page moments detection (deprecated in manual selection edition)
    @Deprecated("Use ManualPagePicker instead - Manual Selection Edition")
    data object AutoMoments : Screen("auto_moments/{sessionId}") {
        fun createRoute(sessionId: String) = "auto_moments/$sessionId"
    }
    
    // Processing
    data object Processing : Screen("processing/{sessionId}") {
        fun createRoute(sessionId: String) = "processing/$sessionId"
    }
    
    // Optional page review
    data object PageReview : Screen("page_review/{sessionId}") {
        fun createRoute(sessionId: String) = "page_review/$sessionId"
    }
    
    // Single page editor
    data object SinglePageEditor : Screen("single_page_editor/{sessionId}/{pageId}") {
        fun createRoute(sessionId: String, pageId: String) = "single_page_editor/$sessionId/$pageId"
    }
    
    // Export setup (reorder, presets, filename)
    data object ExportSetup : Screen("export_setup/{sessionId}") {
        fun createRoute(sessionId: String) = "export_setup/$sessionId"
    }
    
    // Export result (success screen)
    data object ExportResult : Screen("export_result/{sessionId}") {
        fun createRoute(sessionId: String) = "export_result/$sessionId"
    }
    
    // ===== Settings =====
    data object Settings : Screen("settings")
    
    // ===== Legacy routes (kept for migration) =====
    
    @Deprecated("Use AutoMoments instead")
    data object FramePicker : Screen("frame_picker/{projectId}") {
        fun createRoute(projectId: String) = "frame_picker/$projectId"
    }
    
    @Deprecated("Use ExportSetup instead")
    data object PageEditor : Screen("page_editor/{projectId}") {
        fun createRoute(projectId: String) = "page_editor/$projectId"
    }
    
    @Deprecated("Use ExportSetup instead")  
    data object Export : Screen("export/{projectId}") {
        fun createRoute(projectId: String) = "export/$projectId"
    }
    
    @Deprecated("Use ExportsLibrary instead")
    data object Projects : Screen("projects")
    
    @Deprecated("Use PageReview with sessionId instead")
    data object Reorder : Screen("reorder/{projectId}") {
        fun createRoute(projectId: String) = "reorder/$projectId"
    }
    
    companion object {
        const val SESSION_ID_ARG = "sessionId"
        const val PAGE_ID_ARG = "pageId"
        
        // Legacy arg names
        @Deprecated("Use SESSION_ID_ARG")
        const val PROJECT_ID_ARG = "projectId"
    }
}
