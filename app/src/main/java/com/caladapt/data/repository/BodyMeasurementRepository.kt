package com.caladapt.data.repository

import com.caladapt.data.db.dao.BodyMeasurementDao
import com.caladapt.data.db.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyMeasurementRepository @Inject constructor(
    private val dao: BodyMeasurementDao
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getAllMeasurementsFlow(): Flow<List<BodyMeasurementEntity>> = dao.getAllFlow()

    suspend fun getAllMeasurements(): List<BodyMeasurementEntity> = dao.getAll()

    suspend fun getLatestMeasurement(): BodyMeasurementEntity? = dao.getLatest()

    suspend fun getMeasurementByDate(date: LocalDate): BodyMeasurementEntity? =
        dao.getByDate(date.format(dateFormatter))

    suspend fun getMeasurementsBetween(
        start: LocalDate,
        end: LocalDate
    ): List<BodyMeasurementEntity> =
        dao.getBetweenDates(start.format(dateFormatter), end.format(dateFormatter))

    /** Get the two most recent measurements for before/after comparison */
    suspend fun getLastTwoMeasurements(): List<BodyMeasurementEntity> = dao.getLastTwo()

    suspend fun getMeasurementCount(): Int = dao.getCount()

    suspend fun logMeasurement(measurement: BodyMeasurementEntity): Long =
        dao.insert(measurement)

    suspend fun updateMeasurement(measurement: BodyMeasurementEntity) =
        dao.update(measurement)

    suspend fun deleteMeasurement(measurement: BodyMeasurementEntity) =
        dao.delete(measurement)

    suspend fun deleteMeasurementById(id: Int) = dao.deleteById(id)

    suspend fun clearAll() = dao.deleteAll()

    /**
     * Calculate body fat % using the U.S. Navy method.
     * Requires waist, neck, and height (+ hips for female).
     *
     * Male:   BF% = 86.010 × log10(waist − neck) − 70.041 × log10(height) + 36.76
     * Female: BF% = 163.205 × log10(waist + hips − neck) − 97.684 × log10(height) − 78.387
     *
     * All measurements in cm.
     */
    fun calculateNavyBodyFat(
        isMale: Boolean,
        waistCm: Float,
        neckCm: Float,
        heightCm: Float,
        hipsCm: Float? = null
    ): Float? {
        return try {
            if (isMale) {
                val value = 86.010f * Math.log10((waistCm - neckCm).toDouble()).toFloat() -
                        70.041f * Math.log10(heightCm.toDouble()).toFloat() + 36.76f
                value.coerceIn(2f, 60f)
            } else {
                val hips = hipsCm ?: return null
                val value = 163.205f * Math.log10((waistCm + hips - neckCm).toDouble()).toFloat() -
                        97.684f * Math.log10(heightCm.toDouble()).toFloat() - 78.387f
                value.coerceIn(2f, 60f)
            }
        } catch (e: Exception) {
            null
        }
    }
}
