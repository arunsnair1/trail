package com.caladapt.domain.usecase

import com.caladapt.data.db.entity.WeightLogEntity
import com.caladapt.data.repository.WeightRepository
import com.caladapt.domain.algorithm.EMACalculator
import com.caladapt.domain.algorithm.EMAResult
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Use case for logging a weight entry.
 * Computes the EMA automatically and persists both raw and smoothed values.
 * Handles the warm-start seeding logic by bulk-updating if exactly 5 entries exist.
 */
class LogWeightUseCase @Inject constructor(
    private val weightRepository: WeightRepository,
    private val emaCalculator: EMACalculator
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Log today's weight. Computes EMA from the previous entry if sufficient data exists.
     *
     * @param weightKg Raw weight in kg
     * @param date Date of the weigh-in (defaults to today)
     * @return The saved WeightLogEntity with computed EMA
     */
    suspend operator fun invoke(
        weightKg: Float,
        date: LocalDate = LocalDate.now(),
        isWaterRetention: Boolean = false
    ): WeightLogEntity {
        val dateStr = date.format(dateFormatter)
        val allWeights = weightRepository.getAllWeights().toMutableList()
        
        // Remove any existing entry for this date so we can re-evaluate
        val existingIndex = allWeights.indexOfFirst { it.date == dateStr }
        val loggedAt = Instant.now().toString()
        
        val newEntry = WeightLogEntity(
            id = if (existingIndex != -1) allWeights[existingIndex].id else 0,
            date = dateStr,
            weightKg = weightKg,
            emaWeight = weightKg, // Temporary, will be overwritten if > 5 entries
            loggedAt = loggedAt,
            isCycleWaterRetention = isWaterRetention
        )

        if (existingIndex != -1) {
            allWeights[existingIndex] = newEntry
        } else {
            allWeights.add(newEntry)
            allWeights.sortBy { it.date }
        }

        // Run through EMA calculator which enforces the 5-day warm start
        when (val result = emaCalculator.recomputeAllEMA(allWeights)) {
            is EMAResult.Success -> {
                // We have enough data, bulk update everything
                weightRepository.updateAll(result.weightsWithEma)
                return result.weightsWithEma.first { it.date == dateStr }
            }
            is EMAResult.InsufficientData -> {
                // Not enough data. Just log the raw weight, EMA remains identical to raw as placeholder.
                weightRepository.logWeight(
                    date = date,
                    weightKg = weightKg,
                    emaWeight = weightKg,
                    loggedAt = loggedAt,
                    isWaterRetention = isWaterRetention
                )
                return newEntry
            }
        }
    }
}
