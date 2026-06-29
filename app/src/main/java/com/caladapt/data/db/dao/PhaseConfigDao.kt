package com.caladapt.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caladapt.data.db.entity.PhaseConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhaseConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: PhaseConfigEntity): Long

    @Update
    suspend fun update(config: PhaseConfigEntity)

    /** Get the currently active phase */
    @Query("SELECT * FROM phase_config WHERE is_active = 1 LIMIT 1")
    suspend fun getActivePhase(): PhaseConfigEntity?

    @Query("SELECT * FROM phase_config WHERE is_active = 1 LIMIT 1")
    fun getActivePhaseFlow(): Flow<PhaseConfigEntity?>

    /** Get all phases in chronological order (journey history) */
    @Query("SELECT * FROM phase_config ORDER BY started_at ASC")
    fun getAllPhasesFlow(): Flow<List<PhaseConfigEntity>>

    @Query("SELECT * FROM phase_config ORDER BY started_at ASC")
    suspend fun getAllPhases(): List<PhaseConfigEntity>

    /** Deactivate all phases (used before activating a new one) */
    @Query("UPDATE phase_config SET is_active = 0, ended_at = :endedAt WHERE is_active = 1")
    suspend fun deactivateAll(endedAt: String)

    /** Update calorie and macro targets for the active phase */
    @Query("""
        UPDATE phase_config 
        SET daily_calorie_target = :calories,
            protein_target = :protein,
            carbs_target = :carbs,
            fat_target = :fat,
            tdee_estimate = :tdee
        WHERE is_active = 1
    """)
    suspend fun updateActiveTargets(
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        tdee: Float
    )

    @Query("DELETE FROM phase_config")
    suspend fun deleteAll()
}
