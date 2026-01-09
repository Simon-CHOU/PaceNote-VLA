package com.pacenote.vla.core.database.di

import android.content.Context
import androidx.room.Room
import com.pacenote.vla.core.database.VlaDatabase
import com.pacenote.vla.core.database.dao.SessionDao
import com.pacenote.vla.core.database.dao.TelemetryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "vla_database"

    @Provides
    @Singleton
    fun provideVlaDatabase(
        @ApplicationContext context: Context
    ): VlaDatabase {
        return Room.databaseBuilder(
            context,
            VlaDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTelemetryDao(database: VlaDatabase): TelemetryDao {
        return database.telemetryDao()
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: VlaDatabase): SessionDao {
        return database.sessionDao()
    }
}
