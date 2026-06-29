package com.caladapt.di

import android.content.Context
import androidx.room.Room
import com.caladapt.data.db.CalAdaptDatabase
import com.caladapt.data.db.dao.BodyMeasurementDao
import com.caladapt.data.db.dao.CalorieLogDao
import com.caladapt.data.db.dao.DailySummaryDao
import com.caladapt.data.db.dao.PhaseConfigDao
import com.caladapt.data.db.dao.TDEEHistoryDao
import com.caladapt.data.db.dao.UserProfileDao
import com.caladapt.data.db.dao.WeightLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CalAdaptDatabase {
        return Room.databaseBuilder(
            context,
            CalAdaptDatabase::class.java,
            CalAdaptDatabase.DATABASE_NAME
        )
            .addMigrations(CalAdaptDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideUserProfileDao(db: CalAdaptDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideWeightLogDao(db: CalAdaptDatabase): WeightLogDao = db.weightLogDao()

    @Provides
    fun provideCalorieLogDao(db: CalAdaptDatabase): CalorieLogDao = db.calorieLogDao()

    @Provides
    fun provideDailySummaryDao(db: CalAdaptDatabase): DailySummaryDao = db.dailySummaryDao()

    @Provides
    fun provideTDEEHistoryDao(db: CalAdaptDatabase): TDEEHistoryDao = db.tdeeHistoryDao()

    @Provides
    fun provideBodyMeasurementDao(db: CalAdaptDatabase): BodyMeasurementDao = db.bodyMeasurementDao()

    @Provides
    fun providePhaseConfigDao(db: CalAdaptDatabase): PhaseConfigDao = db.phaseConfigDao()
}
