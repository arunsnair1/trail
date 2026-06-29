package com.caladapt.domain.algorithm

import com.caladapt.data.db.entity.WeightLogEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of EMA calculations.
 */
sealed class EMAResult {
    data class Success(val weightsWithEma: List<WeightLogEntity>) : EMAResult()
    object InsufficientData : EMAResult()
}

/**
 * Exponential Moving Average calculator for weight trend smoothing.
 *
 * EMA_today = (Weight_today × α) + (EMA_yesterday × (1 − α))
 *
 * α = 0.1 gives a ~19-day effective window, aggressively filtering
 * daily noise from water, sodium, and digestion fluctuations.
 *
 * Handles missing days via linear interpolation to maintain
 * continuous trend even when user skips weigh-ins.
 */
@Singleton
class EMACalculator @Inject constructor() {

    companion object {
        /** Smoothing factor. 0.1 ≈ 19-day window. Lower = smoother. */
        const val ALPHA = 0.1f
        
        /** Very slow smoothing factor (0.05 ≈ 39-day window) for high water retention days. */
        const val ALPHA_SLOW = 0.05f
        
        /** Minimum entries required for a warm-start seed */
        const val MIN_ENTRIES_FOR_SEED = 5

        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    /**
     * Compute the EMA value for a new weight entry given the previous EMA.
     *
     * @param newWeight Today's raw weight in kg
     * @param previousEMA Yesterday's EMA value (or the seeded value)
     * @param daysSinceLast Number of days since the last weigh-in (1 = consecutive days)
     * @param customAlpha Optional custom alpha (e.g., for cycle water retention days)
     * @return The new EMA value
     */
    fun computeEMA(
        newWeight: Float,
        previousEMA: Float,
        daysSinceLast: Int = 1,
        customAlpha: Float = ALPHA
    ): Float {
        if (daysSinceLast <= 0) return newWeight

        // For consecutive days, standard EMA
        if (daysSinceLast == 1) {
            return (newWeight * customAlpha) + (previousEMA * (1 - customAlpha))
        }

        // For gaps > 1 day: apply EMA stepwise with interpolated weights
        // This simulates what the EMA would have been if the user had
        // weighed in every day with linearly interpolated values
        var ema = previousEMA
        val dailyChange = (newWeight - previousEMA) / daysSinceLast

        for (day in 1..daysSinceLast) {
            val interpolatedWeight = previousEMA + (dailyChange * day)
            // On interpolated days, we use the standard ALPHA
            ema = (interpolatedWeight * ALPHA) + (ema * (1 - ALPHA))
        }

        return ema
    }

    /**
     * Recompute all EMA values for the entire weight log history.
     * Uses warm-start seeding: requires at least 5 entries. The first
     * EMA is seeded using the simple average of the first 5 raw weights.
     * This prevents cold-start volatility where the first few EMA points
     * just wildly track the raw weight before smoothing out.
     *
     * @param weights All weight entries sorted by date ascending
     * @return EMAResult containing updated logs or InsufficientData
     */
    fun recomputeAllEMA(weights: List<WeightLogEntity>): EMAResult {
        if (weights.size < MIN_ENTRIES_FOR_SEED) {
            return EMAResult.InsufficientData
        }

        val result = mutableListOf<WeightLogEntity>()

        // Warm-start seed: average of first 5 raw weights
        val seedAverage = weights.take(MIN_ENTRIES_FOR_SEED).map { it.weightKg }.average().toFloat()
        
        // The first 5 entries will all just carry the seed average as their EMA 
        // to establish a stable baseline.
        for (i in 0 until MIN_ENTRIES_FOR_SEED) {
            result.add(weights[i].copy(emaWeight = seedAverage))
        }

        // Proceed with standard EMA formula from entry 6 onwards
        for (i in MIN_ENTRIES_FOR_SEED until weights.size) {
            val current = weights[i]
            val previous = result[i - 1]

            val daysSinceLast = daysBetween(previous.date, current.date)
            // Stage 7C: Use ALPHA_SLOW (0.05) if user toggled High Water Retention
            val alphaToUse = if (current.isCycleWaterRetention) ALPHA_SLOW else ALPHA
            val ema = computeEMA(current.weightKg, previous.emaWeight, daysSinceLast, alphaToUse)

            result.add(current.copy(emaWeight = ema))
        }

        return EMAResult.Success(result)
    }

    /**
     * Calculate the weight trend rate (kg per week) from EMA data.
     *
     * @param weights Weight entries sorted by date ascending
     * @param daysWindow Number of recent days to consider (default 14)
     * @return Rate in kg/week. Negative = losing, positive = gaining.
     *         Returns null if insufficient data.
     */
    fun calculateWeightTrendRate(
        weights: List<WeightLogEntity>,
        daysWindow: Int = 14
    ): Float? {
        if (weights.size < MIN_ENTRIES_FOR_SEED) return null

        val sorted = weights.sortedBy { it.date }
        val latest = sorted.last()
        val latestDate = LocalDate.parse(latest.date, dateFormatter)
        val windowStart = latestDate.minusDays(daysWindow.toLong())
        val windowStartStr = windowStart.format(dateFormatter)

        // Find the entry closest to the window start
        val entriesInWindow = sorted.filter { it.date >= windowStartStr }
        if (entriesInWindow.size < 2) return null

        val firstInWindow = entriesInWindow.first()
        val lastInWindow = entriesInWindow.last()

        val daysBetween = daysBetween(firstInWindow.date, lastInWindow.date)
        if (daysBetween < 3) return null // Need at least 3 days for meaningful trend

        val emaChange = lastInWindow.emaWeight - firstInWindow.emaWeight
        val ratePerDay = emaChange / daysBetween

        return ratePerDay * 7 // Convert to per-week rate
    }

    private fun daysBetween(dateStr1: String, dateStr2: String): Int {
        val d1 = LocalDate.parse(dateStr1, dateFormatter)
        val d2 = LocalDate.parse(dateStr2, dateFormatter)
        return ChronoUnit.DAYS.between(d1, d2).toInt().coerceAtLeast(1)
    }
}
