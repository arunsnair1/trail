package com.caladapt.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.caladapt.data.db.dao.BodyMeasurementDao
import com.caladapt.data.db.dao.CalorieLogDao
import com.caladapt.data.db.dao.DailySummaryDao
import com.caladapt.data.db.dao.PhaseConfigDao
import com.caladapt.data.db.dao.TDEEHistoryDao
import com.caladapt.data.db.dao.UserProfileDao
import com.caladapt.data.db.dao.WeightLogDao
import com.caladapt.data.db.entity.BodyMeasurementEntity
import com.caladapt.data.db.entity.CalorieLogEntity
import com.caladapt.data.db.entity.DailySummaryEntity
import com.caladapt.data.db.entity.PhaseConfigEntity
import com.caladapt.data.db.entity.TDEEHistoryEntity
import com.caladapt.data.db.entity.UserProfileEntity
import com.caladapt.data.db.entity.WeightLogEntity

@Database(
    entities = [
        UserProfileEntity::class,
        WeightLogEntity::class,
        CalorieLogEntity::class,
        DailySummaryEntity::class,
        TDEEHistoryEntity::class,
        BodyMeasurementEntity::class,
        PhaseConfigEntity::class,
    ],
    version = 2,
    exportSchema = true
)
abstract class CalAdaptDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun calorieLogDao(): CalorieLogDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun tdeeHistoryDao(): TDEEHistoryDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun phaseConfigDao(): PhaseConfigDao

    companion object {
        const val DATABASE_NAME = "caladapt_db"
        
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add new fields to user_profile
                db.execSQL("ALTER TABLE user_profile ADD COLUMN is_discovery_phase INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN discovery_start_date INTEGER NOT NULL DEFAULT 0")
                
                // Add new field to weight_log
                db.execSQL("ALTER TABLE weight_log ADD COLUMN is_cycle_water_retention INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
