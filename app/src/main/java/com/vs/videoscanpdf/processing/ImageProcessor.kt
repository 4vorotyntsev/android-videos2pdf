package com.vs.videoscanpdf.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.vs.videoscanpdf.data.session.PageFilter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Image processor for "readable notes" enhancement.
 * 
 * Default processing (P0.5 - Readable notes):
 * - Page outline detection
 * - Perspective correction
 * - Contrast normalization ("Document" style)
 * - Mild denoise
 * - Mild sharpen
 * - Keep color (no harsh B&W thresholding by default)
 * - No OCR
 */
@Singleton
class ImageProcessor @Inject constructor() {
    
    companion object {
        // Contrast normalization parameters
        private const val CONTRAST_FACTOR = 1.2f
        private const val BRIGHTNESS_OFFSET = 10
        
        // Sharpening kernel (simplified)
        private const val SHARPEN_AMOUNT = 0.3f
        
        // Denoise strength (simplified)
        private const val DENOISE_RADIUS = 1
    }
    
    /**
     * Apply document enhancement to make notes readable.
     * This is the default "Document" filter.
     */
    fun applyDocumentEnhancement(bitmap: Bitmap): Bitmap {
        var result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // Step 1: Normalize contrast
        result = normalizeContrast(result)
        
        // Step 2: Mild denoise (optional, simplified)
        // Note: Full implementation would use OpenCV or similar
        
        // Step 3: Mild sharpen
        result = applySharpen(result)
        
        return result
    }
    
    /**
     * Apply filter based on selected type.
     */
    fun applyFilter(bitmap: Bitmap, filter: PageFilter): Bitmap {
        return when (filter) {
            PageFilter.DOCUMENT -> applyDocumentEnhancement(bitmap)
            PageFilter.ORIGINAL -> bitmap.copy(Bitmap.Config.ARGB_8888, true)
            PageFilter.BLACK_WHITE -> applyBlackAndWhite(bitmap)
        }
    }
    
    /**
     * Normalize contrast for better readability.
     */
    private fun normalizeContrast(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Find min/max luminance for auto-levels
        var minLum = 255
        var maxLum = 0
        
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            minLum = min(minLum, lum)
            maxLum = max(maxLum, lum)
        }
        
        // Apply contrast stretch
        val range = maxLum - minLum
        if (range > 0) {
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val a = (pixel shr 24) and 0xFF
                var r = (pixel shr 16) and 0xFF
                var g = (pixel shr 8) and 0xFF
                var b = pixel and 0xFF
                
                // Stretch each channel
                r = ((r - minLum) * 255 / range).coerceIn(0, 255)
                g = ((g - minLum) * 255 / range).coerceIn(0, 255)
                b = ((b - minLum) * 255 / range).coerceIn(0, 255)
                
                // Apply mild contrast boost
                r = ((r - 128) * CONTRAST_FACTOR + 128 + BRIGHTNESS_OFFSET).toInt().coerceIn(0, 255)
                g = ((g - 128) * CONTRAST_FACTOR + 128 + BRIGHTNESS_OFFSET).toInt().coerceIn(0, 255)
                b = ((b - 128) * CONTRAST_FACTOR + 128 + BRIGHTNESS_OFFSET).toInt().coerceIn(0, 255)
                
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Apply mild sharpening using ColorMatrix.
     */
    private fun applySharpen(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(result)
        val paint = Paint()
        
        // Sharpen via contrast increase (simplified approach)
        val cm = ColorMatrix()
        val contrast = 1 + SHARPEN_AMOUNT
        val brightness = -(128 * SHARPEN_AMOUNT)
        cm.set(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Convert to black and white with high contrast.
     */
    private fun applyBlackAndWhite(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(result)
        val paint = Paint()
        
        // Grayscale with high contrast
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        
        // Add contrast
        val contrast = 1.5f
        val brightness = -40f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastMatrix)
        
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Rotate bitmap by degrees.
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees.toFloat())
        
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }
    
    /**
     * Scale bitmap for PDF output based on quality preset.
     */
    fun scaleBitmapForPdf(bitmap: Bitmap, quality: Int): Bitmap {
        val scale = when {
            quality >= 90 -> 1.0f  // Print quality
            quality >= 70 -> 0.8f // Balanced
            else -> 0.6f // Email-friendly
        }
        
        // Scale up for print resolution (approximately 150 DPI)
        val targetWidth = (bitmap.width * scale * 3).toInt()
        val targetHeight = (bitmap.height * scale * 3).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
    
    /**
     * Detect page outline (simplified - real implementation needs OpenCV).
     * Returns crop rect if detected, null otherwise.
     */
    fun detectPageOutline(bitmap: Bitmap): android.graphics.RectF? {
        // Simplified implementation - return null (no crop)
        // Full implementation would use:
        // 1. Convert to grayscale
        // 2. Apply Canny edge detection
        // 3. Find contours
        // 4. Find largest quadrilateral
        // 5. Return bounding rect
        return null
    }
    
    /**
     * Apply perspective correction (simplified - real implementation needs OpenCV).
     */
    fun applyPerspectiveCorrection(
        bitmap: Bitmap,
        sourcePoints: List<android.graphics.PointF>
    ): Bitmap {
        // Simplified implementation - return original
        // Full implementation would use:
        // 1. Calculate perspective transform matrix
        // 2. Apply warpPerspective
        return bitmap
    }
}

