package com.caladapt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Individual calorie/macro log entry.
 * Multiple entries per day (one per meal/snack).
 */
@Entity(tableName = "calorie_log")
data class CalorieLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Date in yyyy-MM-dd format */
    val date: String,

    /** Optional label: "Breakfast", "Lunch", "Dinner", "Snack", or custom */
    @ColumnInfo(name = "time_of_day")
    val timeOfDay: String = "",

    /** Calories for this entry */
    val calories: Int,

    /** Protein in grams (optional, 0 if not tracked) */
    @ColumnInfo(name = "protein_g")
    val proteinG: Float = 0f,

    /** Carbohydrates in grams (optional, 0 if not tracked) */
    @ColumnInfo(name = "carbs_g")
    val carbsG: Float = 0f,

    /** Fat in grams (optional, 0 if not tracked) */
    @ColumnInfo(name = "fat_g")
    val fatG: Float = 0f,

    /** Optional note for this entry */
    val note: String = "",

    /** ISO-8601 timestamp of when logged */
    @ColumnInfo(name = "logged_at")
    val loggedAt: String
)
