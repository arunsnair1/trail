package com.caladapt.data.repository

import com.caladapt.data.db.dao.TDEEHistoryDao
import com.caladapt.data.db.entity.TDEEHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TDEERepository @Inject constructor(
    private val dao: TDEEHistoryDao
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getAllTDEEFlow(): Flow<List<TDEEHistoryEntity>> = dao.getAllFlow()

    suspend fun getAllTDEE(): List<TDEEHistoryEntity> = dao.getAll()

    suspend fun getLatestTDEE(): TDEEHistoryEntity? = dao.getLatest()

    suspend fun getLastNEntries(n: Int): List<TDEEHistoryEntity> = dao.getLastN(n)

    suspend fun getTDEEBetween(
        start: LocalDate,
        end: LocalDate
    ): List<TDEEHistoryEntity> =
        dao.getBetweenDates(start.format(dateFormatter), end.format(dateFormatter))

    /**
     * Record a new TDEE calculation.
     */
    suspend fun recordTDEE(
        date: LocalDate,
        tdee: Float,
        emaWeight: Float,
        avgCalories7d: Float,
        weightChangeRate: Float,
        phase: String
    ): Long {
        return dao.insert(
            TDEEHistoryEntity(
                date = date.format(dateFormatter),
                calculatedTDEE = tdee,
                emaWeight = emaWeight,
                avgCalories7d = avgCalories7d,
                weightChangeRate = weightChangeRate,
                phase = phase
            )
        )
    }

    suspend fun clearAll() = dao.deleteAll()
}
