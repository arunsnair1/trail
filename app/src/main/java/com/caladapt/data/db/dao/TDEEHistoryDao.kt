package com.caladapt.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.caladapt.data.db.entity.TDEEHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TDEEHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TDEEHistoryEntity): Long

    @Query("SELECT * FROM tdee_history ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): TDEEHistoryEntity?

    @Query("SELECT * FROM tdee_history ORDER BY date ASC")
    fun getAllFlow(): Flow<List<TDEEHistoryEntity>>

    @Query("SELECT * FROM tdee_history ORDER BY date ASC")
    suspend fun getAll(): List<TDEEHistoryEntity>

    @Query("SELECT * FROM tdee_history WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getBetweenDates(startDate: String, endDate: String): List<TDEEHistoryEntity>

    /** Get the most recent N entries */
    @Query("SELECT * FROM tdee_history ORDER BY date DESC LIMIT :limit")
    suspend fun getLastN(limit: Int): List<TDEEHistoryEntity>

    @Query("DELETE FROM tdee_history")
    suspend fun deleteAll()
}
