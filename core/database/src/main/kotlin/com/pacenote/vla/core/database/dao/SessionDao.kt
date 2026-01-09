package com.pacenote.vla.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pacenote.vla.core.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for drive session operations
 */
@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Query("SELECT * FROM drive_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): SessionEntity?

    @Query("SELECT * FROM drive_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getUserSessions(userId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM drive_sessions WHERE userId = :userId AND endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(userId: String): SessionEntity?

    @Query("UPDATE drive_sessions SET endTime = :endTime, distanceMeters = :distance, maxSpeedMps = :maxSpeed, maxGForce = :maxG, aiCommentaryCount = :aiCount, reflexAlertCount = :alertCount WHERE sessionId = :sessionId")
    suspend fun updateSessionEnd(
        sessionId: String,
        endTime: Long,
        distance: Long,
        maxSpeed: Float,
        maxG: Float,
        aiCount: Int,
        alertCount: Int
    ): Int

    @Query("DELETE FROM drive_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String): Int

    @Query("DELETE FROM drive_sessions WHERE userId = :userId AND startTime < :beforeTimestamp")
    suspend fun deleteOldSessions(userId: String, beforeTimestamp: Long): Int
}
