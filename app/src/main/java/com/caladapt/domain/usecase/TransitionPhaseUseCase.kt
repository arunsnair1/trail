package com.caladapt.domain.usecase

import com.caladapt.data.db.entity.PhaseConfigEntity
import com.caladapt.data.repository.PhaseRepository
import com.caladapt.data.repository.UserProfileRepository
import com.caladapt.data.repository.WeightRepository
import com.caladapt.domain.algorithm.PhaseManager
import com.caladapt.domain.model.Goal
import com.caladapt.domain.model.Phase
import com.caladapt.domain.model.Sex
import javax.inject.Inject

/**
 * Use case for transitioning between phases.
 * Handles the lifecycle: DISCOVERY → GOAL → MAINTENANCE
 */
class TransitionPhaseUseCase @Inject constructor(
    private val phaseRepository: PhaseRepository,
    private val weightRepository: WeightRepository,
    private val userProfileRepository: UserProfileRepository,
    private val phaseManager: PhaseManager
) {
    /**
     * Start the initial Discovery phase (called from onboarding).
     */
    suspend fun startDiscovery(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        isMale: Boolean
    ): PhaseConfigEntity {
        val phase = phaseManager.createDiscoveryPhase(weightKg, heightCm, age, isMale)
        phaseRepository.startNewPhase(phase)
        return phase
    }

    /**
     * Transition from Discovery to Goal phase.
     *
     * @param confirmedTDEE The empirically confirmed maintenance TDEE
     * @param goal The user's chosen goal
     * @param targetWeightKg Optional target weight
     */
    suspend fun startGoalPhase(
        confirmedTDEE: Float,
        goal: Goal,
        targetWeightKg: Float? = null
    ): PhaseConfigEntity {
        val profile = userProfileRepository.getProfileOnce()
            ?: throw IllegalStateException("No user profile found")
        val latestWeight = weightRepository.getLatestWeight()
            ?: throw IllegalStateException("No weight data found")

        val isMale = profile.sex == Sex.MALE.name

        val phase = phaseManager.createGoalPhase(
            confirmedTDEE = confirmedTDEE,
            goal = goal,
            currentWeightKg = latestWeight.emaWeight,
            targetWeightKg = targetWeightKg,
            heightCm = profile.heightCm,
            age = profile.age,
            isMale = isMale
        )
        phaseRepository.startNewPhase(phase)
        return phase
    }

    /**
     * Transition to Maintenance phase (after reaching goal or by choice).
     */
    suspend fun startMaintenance(currentTDEE: Float): PhaseConfigEntity {
        val latestWeight = weightRepository.getLatestWeight()
            ?: throw IllegalStateException("No weight data found")

        val phase = phaseManager.createMaintenancePhase(currentTDEE, latestWeight.emaWeight)
        phaseRepository.startNewPhase(phase)
        return phase
    }

    /**
     * Restart Discovery (e.g., after a long break where maintenance may have changed).
     */
    suspend fun restartDiscovery(): PhaseConfigEntity {
        val profile = userProfileRepository.getProfileOnce()
            ?: throw IllegalStateException("No user profile found")
        val latestWeight = weightRepository.getLatestWeight()
            ?: throw IllegalStateException("No weight data found")

        val isMale = profile.sex == Sex.MALE.name

        val phase = phaseManager.createDiscoveryPhase(
            weightKg = latestWeight.emaWeight,
            heightCm = profile.heightCm,
            age = profile.age,
            isMale = isMale
        )
        phaseRepository.startNewPhase(phase)
        return phase
    }
}
