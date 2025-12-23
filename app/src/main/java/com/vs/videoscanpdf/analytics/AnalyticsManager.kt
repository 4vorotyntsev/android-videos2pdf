package com.vs.videoscanpdf.analytics

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics event names as specified in the app flow.
 */
object AnalyticsEvent {
    // Home screen events
    const val HOME_VIEW = "home_view"
    const val HOME_START_SCANNING_CLICK = "home_start_scanning_click"
    const val HOME_IMPORT_CLICK = "home_import_click"
    const val HOME_RECENT_OPEN = "home_recent_open"
    
    // Camera events
    const val CAMERA_OPEN = "camera_open"
    const val RECORD_START = "record_start"
    const val RECORD_STOP = "record_stop"
    const val USE_VIDEO = "use_video"
    const val RETAKE = "retake"
    const val DISCARD_CONFIRM = "discard_confirm"
    
    // Import events
    const val IMPORT_VIEW = "import_view"
    const val IMPORT_SELECT = "import_select"
    const val IMPORT_VALIDATE_SUCCESS = "import_validate_success"
    const val IMPORT_VALIDATE_FAIL = "import_validate_fail"
    
    // Trim events
    const val TRIM_VIEW = "trim_view"
    const val TRIM_CHANGE = "trim_change"
    const val TRIM_CONTINUE = "trim_continue"
    const val TRIM_SKIP = "trim_skip"
    
    // Auto moments events
    const val MOMENTS_VIEW = "moments_view"
    const val MOMENTS_CREATE_CLICK = "moments_create_click"
    const val MOMENTS_REVIEW_CLICK = "moments_review_click"
    const val MOMENTS_ADVANCED_TOGGLE = "moments_advanced_toggle"
    
    // Processing events
    const val PROCESSING_START = "processing_start"
    const val PROCESSING_STAGE_COMPLETE = "processing_stage_complete"
    const val PROCESSING_COMPLETE = "processing_complete"
    const val PROCESSING_ERROR = "processing_error"
    const val PROCESSING_CANCEL = "processing_cancel"
    
    // Page review events
    const val REVIEW_OPEN = "review_open"
    const val PAGE_DELETE = "page_delete"
    const val PAGE_REPLACE = "page_replace"
    const val PAGE_EDIT_OPEN = "page_edit_open"
    const val REVIEW_CONTINUE = "review_continue"
    
    // Page editor events
    const val EDITOR_TOOL_USE = "editor_tool_use"
    
    // Export events
    const val EXPORT_SETUP_VIEW = "export_setup_view"
    const val EXPORT_PRESET_SELECTED = "export_preset_selected"
    const val EXPORT_START = "export_start"
    const val EXPORT_SUCCESS = "export_success"
    const val EXPORT_FAILURE = "export_failure"
    
    // Export result events
    const val RESULT_OPEN = "result_open"
    const val RESULT_SHARE = "result_share"
    const val RESULT_VIEW_FILES = "result_view_files"
    const val RESULT_CREATE_ANOTHER = "result_create_another"
    
    // Exports library events
    const val LIBRARY_VIEW = "library_view"
    const val LIBRARY_SEARCH = "library_search"
    const val LIBRARY_OPEN = "library_open"
    const val LIBRARY_DELETE = "library_delete"
    const val LIBRARY_RENAME = "library_rename"
}

/**
 * Analytics manager for tracking app events.
 * 
 * In a production app, this would integrate with:
 * - Firebase Analytics
 * - Amplitude
 * - Mixpanel
 * - Custom analytics backend
 * 
 * For now, this logs events to Logcat for debugging.
 */
@Singleton
class AnalyticsManager @Inject constructor() {
    
    companion object {
        private const val TAG = "Analytics"
    }
    
    /**
     * Track a simple event without parameters.
     */
    fun track(event: String) {
        Log.d(TAG, "Event: $event")
        // In production: firebaseAnalytics.logEvent(event, null)
    }
    
    /**
     * Track an event with a single parameter.
     */
    fun track(event: String, paramName: String, paramValue: String) {
        Log.d(TAG, "Event: $event - $paramName: $paramValue")
        // In production: 
        // val bundle = bundleOf(paramName to paramValue)
        // firebaseAnalytics.logEvent(event, bundle)
    }
    
    /**
     * Track an event with a single numeric parameter.
     */
    fun track(event: String, paramName: String, paramValue: Long) {
        Log.d(TAG, "Event: $event - $paramName: $paramValue")
        // In production:
        // val bundle = bundleOf(paramName to paramValue)
        // firebaseAnalytics.logEvent(event, bundle)
    }
    
    /**
     * Track an event with multiple parameters.
     */
    fun track(event: String, params: Map<String, Any>) {
        Log.d(TAG, "Event: $event - params: $params")
        // In production:
        // val bundle = Bundle().apply {
        //     params.forEach { (key, value) ->
        //         when (value) {
        //             is String -> putString(key, value)
        //             is Long -> putLong(key, value)
        //             is Int -> putInt(key, value)
        //             is Boolean -> putBoolean(key, value)
        //             is Float -> putFloat(key, value)
        //             is Double -> putDouble(key, value)
        //         }
        //     }
        // }
        // firebaseAnalytics.logEvent(event, bundle)
    }
    
    /**
     * Track screen view.
     */
    fun trackScreen(screenName: String) {
        Log.d(TAG, "Screen: $screenName")
        // In production:
        // firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
        //     FirebaseAnalytics.Param.SCREEN_NAME to screenName
        // ))
    }
    
    /**
     * Track timing event (e.g., processing duration).
     */
    fun trackTiming(event: String, durationMs: Long) {
        Log.d(TAG, "Timing: $event - ${durationMs}ms")
        // In production:
        // val bundle = bundleOf("duration_ms" to durationMs)
        // firebaseAnalytics.logEvent(event, bundle)
    }
    
    /**
     * Track error event.
     */
    fun trackError(event: String, errorMessage: String, errorCode: String? = null) {
        Log.e(TAG, "Error: $event - $errorMessage (code: $errorCode)")
        // In production:
        // val bundle = bundleOf(
        //     "error_message" to errorMessage,
        //     "error_code" to (errorCode ?: "unknown")
        // )
        // firebaseAnalytics.logEvent(event, bundle)
    }
}

