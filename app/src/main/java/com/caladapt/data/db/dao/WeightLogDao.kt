package com.caladapt.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caladapt.data.db.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightLogEntity): Long

    @Update
    suspend fun update(entry: WeightLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WeightLogEntity>)

    @Query("UPDATE weight_log SET is_cycle_water_retention = :isActive WHERE date = :date")
    suspend fun updateWaterRetention(date: String, isActive: Boolean)

    @Query("SELECT * FROM weight_log WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): WeightLogEntity?

    @Query("SELECT * FROM weight_log ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): WeightLogEntity?

    @Query("SELECT * FROM weight_log ORDER BY date DESC LIMIT 1")
    fun getLatestFlow(): Flow<WeightLogEntity?>

    @Query("SELECT * FROM weight_log ORDER BY date ASC")
    fun getAllFlow(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log ORDER BY date ASC")
    suspend fun getAll(): List<WeightLogEntity>

    /** Get weight logs for the last N days from a given date */
    @Query("SELECT * FROM weight_log WHERE date >= :fromDate ORDER BY date ASC")
    suspend fun getFromDate(fromDate: String): List<WeightLogEntity>

    /** Get weight logs between two dates inclusive */
    @Query("SELECT * FROM weight_log WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getBetweenDates(startDate: String, endDate: String): List<WeightLogEntity>

    /** Get the last N entries by date descending */
    @Query("SELECT * FROM weight_log ORDER BY date DESC LIMIT :limit")
    suspend fun getLastN(limit: Int): List<WeightLogEntity>

    @Query("SELECT COUNT(*) FROM weight_log")
    suspend fun getCount(): Int

    @Query("DELETE FROM weight_log WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM weight_log")
    suspend fun deleteAll()
}
