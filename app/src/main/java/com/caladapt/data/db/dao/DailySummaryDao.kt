package com.caladapt.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caladapt.data.db.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(summary: DailySummaryEntity): Long

    @Update
    suspend fun update(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summary WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailySummaryEntity?

    @Query("SELECT * FROM daily_summary WHERE date = :date LIMIT 1")
    fun getByDateFlow(date: String): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summary ORDER BY date DESC")
    fun getAllFlow(): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summary ORDER BY date DESC")
    suspend fun getAll(): List<DailySummaryEntity>

    @Query("SELECT * FROM daily_summary WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getBetweenDates(startDate: String, endDate: String): List<DailySummaryEntity>

    /** Average calorie compliance (actual / target) over a date range */
    @Query("""
        SELECT COALESCE(AVG(CAST(total_calories AS FLOAT) / NULLIF(calorie_target, 0)), 0)
        FROM daily_summary
        WHERE date BETWEEN :startDate AND :endDate
        AND calorie_target > 0
    """)
    suspend fun getAverageCompliance(startDate: String, endDate: String): Float

    @Query("DELETE FROM daily_summary")
    suspend fun deleteAll()
}
