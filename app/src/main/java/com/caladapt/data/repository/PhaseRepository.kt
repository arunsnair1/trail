package com.caladapt.data.repository

import com.caladapt.data.db.dao.PhaseConfigDao
import com.caladapt.data.db.entity.PhaseConfigEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhaseRepository @Inject constructor(
    private val dao: PhaseConfigDao
) {
    fun getActivePhaseFlow(): Flow<PhaseConfigEntity?> = dao.getActivePhaseFlow()

    suspend fun getActivePhase(): PhaseConfigEntity? = dao.getActivePhase()

    fun getAllPhasesFlow(): Flow<List<PhaseConfigEntity>> = dao.getAllPhasesFlow()

    suspend fun getAllPhases(): List<PhaseConfigEntity> = dao.getAllPhases()

    /**
     * Start a new phase. Deactivates any currently active phase first.
     */
    suspend fun startNewPhase(phase: PhaseConfigEntity): Long {
        dao.deactivateAll(Instant.now().toString())
        return dao.insert(phase.copy(isActive = true))
    }

    /**
     * Update the calorie/macro targets for the currently active phase.
     */
    suspend fun updateActiveTargets(
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        tdee: Float
    ) {
        dao.updateActiveTargets(calories, protein, carbs, fat, tdee)
    }

    /**
     * End the current phase (mark inactive with end timestamp).
     */
    suspend fun endActivePhase() {
        dao.deactivateAll(Instant.now().toString())
    }

    suspend fun updatePhase(phase: PhaseConfigEntity) = dao.update(phase)

    suspend fun clearAll() = dao.deleteAll()
}
