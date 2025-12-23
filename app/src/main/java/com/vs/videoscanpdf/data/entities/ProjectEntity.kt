package com.vs.videoscanpdf.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Project status enum.
 */
enum class ProjectStatus {
    DRAFT,
    EXPORTED
}

/**
 * Represents a scanning project containing a video and extracted pages.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val videoPath: String? = null,
    val videoDurationMs: Long = 0L,
    val isVideoDeleted: Boolean = false,
    val status: String = ProjectStatus.DRAFT.name,
    val thumbnailPath: String? = null
)
