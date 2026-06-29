package com.caladapt.data.repository

import com.caladapt.data.db.dao.WeightLogDao
import com.caladapt.data.db.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepository @Inject constructor(
    private val dao: WeightLogDao
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getAllWeightsFlow(): Flow<List<WeightLogEntity>> = dao.getAllFlow()

    fun getLatestWeightFlow(): Flow<WeightLogEntity?> = dao.getLatestFlow()

    suspend fun getLatestWeight(): WeightLogEntity? = dao.getLatest()

    suspend fun getAllWeights(): List<WeightLogEntity> = dao.getAll()

    suspend fun getWeightByDate(date: LocalDate): WeightLogEntity? =
        dao.getByDate(date.format(dateFormatter))

    suspend fun getWeightsFromDate(fromDate: LocalDate): List<WeightLogEntity> =
        dao.getFromDate(fromDate.format(dateFormatter))

    suspend fun getWeightsBetween(start: LocalDate, end: LocalDate): List<WeightLogEntity> =
        dao.getBetweenDates(start.format(dateFormatter), end.format(dateFormatter))

    suspend fun getLastNWeights(n: Int): List<WeightLogEntity> = dao.getLastN(n)

    suspend fun getWeightCount(): Int = dao.getCount()

    /**
     * Insert or update a weight entry for the given date.
     * The EMA weight is computed externally (by EMACalculator) and passed in.
     */
    suspend fun logWeight(
        date: LocalDate,
        weightKg: Float,
        emaWeight: Float,
        loggedAt: String,
        isWaterRetention: Boolean = false
    ): Long {
        val dateStr = date.format(dateFormatter)
        val existing = dao.getByDate(dateStr)
        val entity = WeightLogEntity(
            id = existing?.id ?: 0,
            date = dateStr,
            weightKg = weightKg,
            emaWeight = emaWeight,
            loggedAt = loggedAt,
            isCycleWaterRetention = isWaterRetention
        )
        return if (existing != null) {
            dao.update(entity)
            existing.id.toLong()
        } else {
            dao.insert(entity)
        }
    }

    suspend fun updateAll(entries: List<WeightLogEntity>) {
        dao.insertAll(entries)
    }

    suspend fun toggleWaterRetention(date: LocalDate, isActive: Boolean) {
        dao.updateWaterRetention(date.format(dateFormatter), isActive)
    }

    suspend fun deleteWeight(id: Int) = dao.deleteById(id)

    suspend fun clearAll() = dao.deleteAll()
}
