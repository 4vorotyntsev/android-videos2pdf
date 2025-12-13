package com.vs.videoscanpdf.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

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
    val isVideoDeleted: Boolean = false
)
