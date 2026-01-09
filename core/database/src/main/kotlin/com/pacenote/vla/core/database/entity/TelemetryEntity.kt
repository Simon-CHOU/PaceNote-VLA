package com.pacenote.vla.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pacenote.vla.core.domain.model.TelemetryData

/**
 * Room entity for persisting telemetry data
 */
@Entity(tableName = "telemetry_logs")
data class TelemetryEntity(
    @PrimaryKey
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speedMps: Float,
    val bearing: Float,
    val accelerationX: Float,
    val accelerationY: Float,
    val accelerationZ: Float,
    val longitudinalG: Float,
    val lateralG: Float,
    val yawRate: Float
) {
    companion object {
        fun fromDomain(data: TelemetryData): TelemetryEntity = TelemetryEntity(
            timestamp = data.timestamp,
            latitude = data.latitude,
            longitude = data.longitude,
            altitude = data.altitude,
            speedMps = data.speedMps,
            bearing = data.bearing,
            accelerationX = data.accelerationX,
            accelerationY = data.accelerationY,
            accelerationZ = data.accelerationZ,
            longitudinalG = data.longitudinalG,
            lateralG = data.lateralG,
            yawRate = data.yawRate
        )
    }

    fun toDomain(): TelemetryData = TelemetryData(
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        speedMps = speedMps,
        bearing = bearing,
        accelerationX = accelerationX,
        accelerationY = accelerationY,
        accelerationZ = accelerationZ,
        longitudinalG = longitudinalG,
        lateralG = lateralG,
        yawRate = yawRate
    )
}
