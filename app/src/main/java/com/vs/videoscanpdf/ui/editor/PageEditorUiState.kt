package com.vs.videoscanpdf.ui.editor

import android.graphics.Bitmap
import com.vs.videoscanpdf.data.entities.PageEntity

/**
 * UI state for the Page Editor screen.
 */
data class PageEditorUiState(
    val pages: List<PageWithBitmap> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Page entity with its loaded bitmap.
 */
data class PageWithBitmap(
    val entity: PageEntity,
    val bitmap: Bitmap?
)
