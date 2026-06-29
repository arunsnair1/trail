package com.caladapt.data.repository

import com.caladapt.data.db.dao.CalorieLogDao
import com.caladapt.data.db.dao.DailySummaryDao
import com.caladapt.data.db.dao.MacroTotals
import com.caladapt.data.db.entity.CalorieLogEntity
import com.caladapt.data.db.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalorieRepository @Inject constructor(
    private val calorieLogDao: CalorieLogDao,
    private val dailySummaryDao: DailySummaryDao
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // --- Calorie Log CRUD ---

    fun getLogsForDateFlow(date: LocalDate): Flow<List<CalorieLogEntity>> =
        calorieLogDao.getByDateFlow(date.format(dateFormatter))

    suspend fun getLogsForDate(date: LocalDate): List<CalorieLogEntity> =
        calorieLogDao.getByDate(date.format(dateFormatter))

    fun getAllLogsFlow(): Flow<List<CalorieLogEntity>> = calorieLogDao.getAllFlow()

    /**
     * Add a calorie log entry and update the daily summary.
     */
    suspend fun logCalories(entry: CalorieLogEntity): Long {
        val id = calorieLogDao.insert(entry)
        refreshDailySummary(entry.date)
        return id
    }

    suspend fun updateEntry(entry: CalorieLogEntity) {
        calorieLogDao.update(entry)
        refreshDailySummary(entry.date)
    }

    suspend fun deleteEntry(entry: CalorieLogEntity) {
        calorieLogDao.delete(entry)
        refreshDailySummary(entry.date)
    }

    suspend fun deleteEntryById(id: Int, date: String) {
        calorieLogDao.deleteById(id)
        refreshDailySummary(date)
    }

    // --- Daily Summary ---

    fun getDailySummaryFlow(date: LocalDate): Flow<DailySummaryEntity?> =
        dailySummaryDao.getByDateFlow(date.format(dateFormatter))

    suspend fun getDailySummary(date: LocalDate): DailySummaryEntity? =
        dailySummaryDao.getByDate(date.format(dateFormatter))

    fun getAllSummariesFlow(): Flow<List<DailySummaryEntity>> = dailySummaryDao.getAllFlow()

    suspend fun getSummariesBetween(
        start: LocalDate,
        end: LocalDate
    ): List<DailySummaryEntity> =
        dailySummaryDao.getBetweenDates(
            start.format(dateFormatter),
            end.format(dateFormatter)
        )

    // --- Aggregation Queries ---

    suspend fun getTotalCaloriesForDate(date: LocalDate): Int =
        calorieLogDao.getTotalCaloriesForDate(date.format(dateFormatter))

    suspend fun getMacroTotalsForDate(date: LocalDate): MacroTotals =
        calorieLogDao.getMacroTotalsForDate(date.format(dateFormatter))

    suspend fun getAverageDailyCalories(start: LocalDate, end: LocalDate): Float =
        calorieLogDao.getAverageDailyCalories(
            start.format(dateFormatter),
            end.format(dateFormatter)
        )

    suspend fun getAverageCompliance(start: LocalDate, end: LocalDate): Float =
        dailySummaryDao.getAverageCompliance(
            start.format(dateFormatter),
            end.format(dateFormatter)
        )

    /**
     * Update the daily summary targets (called when phase targets change).
     */
    suspend fun updateDailySummaryTargets(
        date: LocalDate,
        calorieTarget: Int,
        proteinTarget: Float,
        carbsTarget: Float,
        fatTarget: Float
    ) {
        val dateStr = date.format(dateFormatter)
        val existing = dailySummaryDao.getByDate(dateStr)
        if (existing != null) {
            dailySummaryDao.update(
                existing.copy(
                    calorieTarget = calorieTarget,
                    proteinTarget = proteinTarget,
                    carbsTarget = carbsTarget,
                    fatTarget = fatTarget
                )
            )
        } else {
            dailySummaryDao.insertOrReplace(
                DailySummaryEntity(
                    date = dateStr,
                    calorieTarget = calorieTarget,
                    proteinTarget = proteinTarget,
                    carbsTarget = carbsTarget,
                    fatTarget = fatTarget
                )
            )
        }
    }

    // --- Internal ---

    /**
     * Recompute and persist the daily summary from individual calorie_log entries.
     */
    private suspend fun refreshDailySummary(dateStr: String) {
        val totalCals = calorieLogDao.getTotalCaloriesForDate(dateStr)
        val macros = calorieLogDao.getMacroTotalsForDate(dateStr)
        val existing = dailySummaryDao.getByDate(dateStr)

        val summary = DailySummaryEntity(
            id = existing?.id ?: 0,
            date = dateStr,
            totalCalories = totalCals,
            totalProtein = macros.totalProtein,
            totalCarbs = macros.totalCarbs,
            totalFat = macros.totalFat,
            // Preserve existing targets
            calorieTarget = existing?.calorieTarget ?: 0,
            proteinTarget = existing?.proteinTarget ?: 0f,
            carbsTarget = existing?.carbsTarget ?: 0f,
            fatTarget = existing?.fatTarget ?: 0f
        )

        dailySummaryDao.insertOrReplace(summary)
    }

    suspend fun clearAll() {
        calorieLogDao.deleteAll()
        dailySummaryDao.deleteAll()
    }
}
