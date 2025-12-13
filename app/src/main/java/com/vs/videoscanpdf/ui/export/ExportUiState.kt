package com.vs.videoscanpdf.ui.export

/**
 * UI state for the Export screen.
 */
data class ExportUiState(
    val pageSize: PageSize = PageSize.A4,
    val quality: ExportQuality = ExportQuality.STANDARD,
    val pageCount: Int = 0,
    val useGrayscale: Boolean = false,
    val pdfFileName: String = "scan",
    val isEditingFileName: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportedPdfPath: String? = null,
    val error: String? = null
)

/**
 * Page size options.
 */
enum class PageSize(val displayName: String, val widthPt: Int, val heightPt: Int) {
    AUTO("Auto", 0, 0),
    A4("A4", 595, 842),
    LETTER("Letter", 612, 792)
}

/**
 * Export quality options.
 */
enum class ExportQuality(val displayName: String, val jpegQuality: Int) {
    STANDARD("Standard", 80),
    HIGH("High", 95)
}
