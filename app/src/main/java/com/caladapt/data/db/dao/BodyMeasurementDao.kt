package com.caladapt.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caladapt.data.db.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: BodyMeasurementEntity): Long

    @Update
    suspend fun update(measurement: BodyMeasurementEntity)

    @Delete
    suspend fun delete(measurement: BodyMeasurementEntity)

    @Query("SELECT * FROM body_measurement WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurement ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurement ORDER BY date DESC")
    fun getAllFlow(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurement ORDER BY date ASC")
    suspend fun getAll(): List<BodyMeasurementEntity>

    @Query("SELECT * FROM body_measurement WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getBetweenDates(startDate: String, endDate: String): List<BodyMeasurementEntity>

    /** Get the two most recent measurements for before/after comparison */
    @Query("SELECT * FROM body_measurement ORDER BY date DESC LIMIT 2")
    suspend fun getLastTwo(): List<BodyMeasurementEntity>

    @Query("SELECT COUNT(*) FROM body_measurement")
    suspend fun getCount(): Int

    @Query("DELETE FROM body_measurement WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM body_measurement")
    suspend fun deleteAll()
}
