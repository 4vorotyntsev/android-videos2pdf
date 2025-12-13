package com.vs.videoscanpdf.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a single page extracted from a video.
 * Contains source time, transformations (rotation, crop), and filter type.
 */
@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class PageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val index: Int,
    val sourceTimeMs: Long,
    val imagePath: String,
    val rotation: Int = 0,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val filterType: String = FilterType.ORIGINAL.name,
    val perspectiveParams: String? = null // JSON for perspective correction params
)

/**
 * Available filter types for page images.
 */
enum class FilterType {
    ORIGINAL,
    GRAYSCALE,
    BLACK_WHITE,
    ENHANCED
}
