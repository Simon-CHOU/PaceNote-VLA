package com.pacenote.vla.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pacenote.vla.core.database.entity.TelemetryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for telemetry operations
 */
@Dao
interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(telemetry: TelemetryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(telemetries: List<TelemetryEntity>)

    @Query("SELECT * FROM telemetry_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getTelemetryInRange(startTime: Long, endTime: Long): Flow<List<TelemetryEntity>>

    @Query("SELECT * FROM telemetry_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTelemetry(limit: Int = 1000): Flow<List<TelemetryEntity>>

    @Query("DELETE FROM telemetry_logs WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldEntries(beforeTimestamp: Long): Int

    @Query("DELETE FROM telemetry_logs")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM telemetry_logs")
    suspend fun count(): Long

    @Query("SELECT MAX(ABS(longitudinalG)) AS maxLongitudinalG, MAX(ABS(lateralG)) AS maxLateralG FROM telemetry_logs")
    suspend fun getMaxGForces(): GForceResult?

    data class GForceResult(
        @ColumnInfo(name = "maxLongitudinalG") val maxLongitudinalG: Float?,
        @ColumnInfo(name = "maxLateralG") val maxLateralG: Float?
    )
}
