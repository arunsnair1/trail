package com.caladapt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Aggregated daily summary computed from calorie_log entries.
 * Updated whenever a calorie log entry is added/modified for that date.
 * Also stores the targets for comparison.
 */
@Entity(
    tableName = "daily_summary",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailySummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Date in yyyy-MM-dd format */
    val date: String,

    // Actuals (aggregated from calorie_log)
    @ColumnInfo(name = "total_calories")
    val totalCalories: Int = 0,

    @ColumnInfo(name = "total_protein")
    val totalProtein: Float = 0f,

    @ColumnInfo(name = "total_carbs")
    val totalCarbs: Float = 0f,

    @ColumnInfo(name = "total_fat")
    val totalFat: Float = 0f,

    // Targets (from current phase config)
    @ColumnInfo(name = "calorie_target")
    val calorieTarget: Int = 0,

    @ColumnInfo(name = "protein_target")
    val proteinTarget: Float = 0f,

    @ColumnInfo(name = "carbs_target")
    val carbsTarget: Float = 0f,

    @ColumnInfo(name = "fat_target")
    val fatTarget: Float = 0f
)
