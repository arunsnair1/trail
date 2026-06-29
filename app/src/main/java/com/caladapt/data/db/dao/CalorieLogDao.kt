package com.caladapt.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caladapt.data.db.entity.CalorieLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalorieLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CalorieLogEntity): Long

    @Update
    suspend fun update(entry: CalorieLogEntity)

    @Delete
    suspend fun delete(entry: CalorieLogEntity)

    /** All entries for a specific date, ordered by log time */
    @Query("SELECT * FROM calorie_log WHERE date = :date ORDER BY logged_at ASC")
    fun getByDateFlow(date: String): Flow<List<CalorieLogEntity>>

    @Query("SELECT * FROM calorie_log WHERE date = :date ORDER BY logged_at ASC")
    suspend fun getByDate(date: String): List<CalorieLogEntity>

    /** Sum of calories for a specific date */
    @Query("SELECT COALESCE(SUM(calories), 0) FROM calorie_log WHERE date = :date")
    suspend fun getTotalCaloriesForDate(date: String): Int

    /** Sum of macros for a specific date */
    @Query("""
        SELECT COALESCE(SUM(protein_g), 0) as totalProtein,
               COALESCE(SUM(carbs_g), 0) as totalCarbs,
               COALESCE(SUM(fat_g), 0) as totalFat
        FROM calorie_log WHERE date = :date
    """)
    suspend fun getMacroTotalsForDate(date: String): MacroTotals

    /** Average daily calories over a date range */
    @Query("""
        SELECT COALESCE(AVG(daily_total), 0) FROM (
            SELECT SUM(calories) as daily_total
            FROM calorie_log
            WHERE date BETWEEN :startDate AND :endDate
            GROUP BY date
        )
    """)
    suspend fun getAverageDailyCalories(startDate: String, endDate: String): Float

    /** All entries ordered by date desc for history view */
    @Query("SELECT * FROM calorie_log ORDER BY date DESC, logged_at ASC")
    fun getAllFlow(): Flow<List<CalorieLogEntity>>

    /** Get dates that have calorie logs between two dates */
    @Query("SELECT DISTINCT date FROM calorie_log WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getLoggedDates(startDate: String, endDate: String): List<String>

    @Query("SELECT COUNT(*) FROM calorie_log WHERE date = :date")
    suspend fun getCountForDate(date: String): Int

    @Query("DELETE FROM calorie_log WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM calorie_log")
    suspend fun deleteAll()
}

/** Helper data class for macro totals query */
data class MacroTotals(
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float
)
