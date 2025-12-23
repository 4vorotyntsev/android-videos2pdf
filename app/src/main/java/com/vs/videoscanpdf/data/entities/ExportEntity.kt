package com.vs.videoscanpdf.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a PDF export from a project.
 * Tracks export history and settings used.
 */
@Entity(
    tableName = "exports",
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
data class ExportEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val pdfPath: String,
    val pageCount: Int,
    val fileSize: Long = 0L,
    val settingsJson: String = "{}" // JSON object with page size, quality, etc.
)
