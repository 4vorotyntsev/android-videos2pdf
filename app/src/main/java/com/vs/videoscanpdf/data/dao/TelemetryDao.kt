package com.vs.videoscanpdf.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vs.videoscanpdf.data.entities.TelemetryEventEntity

/**
 * Data Access Object for telemetry events.
 */
@Dao
interface TelemetryDao {
    
    @Query("SELECT * FROM telemetry_events ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<TelemetryEventEntity>
    
    @Query("SELECT * FROM telemetry_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getEventsSince(sinceTimestamp: Long): List<TelemetryEventEntity>
    
    @Query("SELECT COUNT(*) FROM telemetry_events")
    suspend fun getEventCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: TelemetryEventEntity)
    
    @Query("DELETE FROM telemetry_events WHERE timestamp < :beforeTimestamp")
    suspend fun deleteEventsBefore(beforeTimestamp: Long)
    
    @Query("DELETE FROM telemetry_events")
    suspend fun deleteAllEvents()
    
    // Keep only the most recent N events
    @Query("""
        DELETE FROM telemetry_events 
        WHERE id NOT IN (
            SELECT id FROM telemetry_events 
            ORDER BY timestamp DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun pruneOldEvents(keepCount: Int = 10000)
}
