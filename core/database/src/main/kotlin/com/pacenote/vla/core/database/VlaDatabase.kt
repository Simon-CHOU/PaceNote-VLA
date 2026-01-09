package com.pacenote.vla.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pacenote.vla.core.database.dao.SessionDao
import com.pacenote.vla.core.database.dao.TelemetryDao
import com.pacenote.vla.core.database.entity.SessionEntity
import com.pacenote.vla.core.database.entity.TelemetryEntity

/**
 * Main Room database for PaceNote VLA
 */
@Database(
    entities = [
        TelemetryEntity::class,
        SessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class VlaDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
    abstract fun sessionDao(): SessionDao
}
