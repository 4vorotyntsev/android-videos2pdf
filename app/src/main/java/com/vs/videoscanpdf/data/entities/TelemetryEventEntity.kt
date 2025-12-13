package com.vs.videoscanpdf.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a telemetry event for analytics.
 * Stored locally and can be exported for diagnostics.
 */
@Entity(tableName = "telemetry_events")
data class TelemetryEventEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String,
    val projectId: String? = null,
    val eventName: String,
    val propsJson: String = "{}" // JSON object with event properties
)
