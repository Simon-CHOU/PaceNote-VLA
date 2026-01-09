package com.pacenote.vla.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pacenote.vla.core.domain.model.DriveSession

/**
 * Room entity for drive sessions
 */
@Entity(tableName = "drive_sessions")
data class SessionEntity(
    @PrimaryKey
    val sessionId: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long?,
    val distanceMeters: Long,
    val maxSpeedMps: Float,
    val maxGForce: Float,
    val aiCommentaryCount: Int,
    val reflexAlertCount: Int,
    val recordingPath: String?
) {
    companion object {
        fun fromDomain(session: DriveSession): SessionEntity = SessionEntity(
            sessionId = session.sessionId,
            userId = session.userId,
            startTime = session.startTime,
            endTime = session.endTime,
            distanceMeters = session.distanceMeters,
            maxSpeedMps = session.maxSpeedMps,
            maxGForce = session.maxGForce,
            aiCommentaryCount = session.aiCommentaryCount,
            reflexAlertCount = session.reflexAlertCount,
            recordingPath = session.recordingUrl
        )
    }

    fun toDomain(): DriveSession = DriveSession(
        sessionId = sessionId,
        userId = userId,
        startTime = startTime,
        endTime = endTime,
        distanceMeters = distanceMeters,
        maxSpeedMps = maxSpeedMps,
        maxGForce = maxGForce,
        aiCommentaryCount = aiCommentaryCount,
        reflexAlertCount = reflexAlertCount,
        recordingUrl = recordingPath
    )
}
