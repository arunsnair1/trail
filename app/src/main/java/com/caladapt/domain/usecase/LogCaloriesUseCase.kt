package com.caladapt.domain.usecase

import com.caladapt.data.db.entity.CalorieLogEntity
import com.caladapt.data.repository.CalorieRepository
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case for logging a calorie/macro entry.
 * Automatically refreshes the daily summary after each log.
 */
class LogCaloriesUseCase @Inject constructor(
    private val calorieRepository: CalorieRepository
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Log a calorie entry for a meal/snack.
     *
     * @param calories Calorie count
     * @param proteinG Protein in grams (optional)
     * @param carbsG Carbs in grams (optional)
     * @param fatG Fat in grams (optional)
     * @param timeOfDay Label like "Breakfast", "Lunch", "Dinner", "Snack"
     * @param note Optional note
     * @param date Date of the entry (defaults to today)
     * @return ID of the created entry
     */
    suspend operator fun invoke(
        calories: Int,
        proteinG: Float = 0f,
        carbsG: Float = 0f,
        fatG: Float = 0f,
        timeOfDay: String = "",
        note: String = "",
        date: LocalDate = LocalDate.now()
    ): Long {
        val entry = CalorieLogEntity(
            date = date.format(dateFormatter),
            timeOfDay = timeOfDay,
            calories = calories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            note = note,
            loggedAt = Instant.now().toString()
        )

        return calorieRepository.logCalories(entry)
    }
}
