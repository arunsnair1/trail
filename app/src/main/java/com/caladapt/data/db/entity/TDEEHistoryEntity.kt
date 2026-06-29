package com.caladapt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Historical record of TDEE calculations.
 * A new entry is created each time the TDEE is recalculated (typically weekly).
 * This enables charting how TDEE evolves over time (metabolic adaptation tracking).
 */
@Entity(tableName = "tdee_history")
data class TDEEHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Date of the calculation in yyyy-MM-dd format */
    val date: String,

    /** Calculated TDEE in kcal/day */
    @ColumnInfo(name = "calculated_tdee")
    val calculatedTDEE: Float,

    /** EMA weight at time of calculation (kg) */
    @ColumnInfo(name = "ema_weight")
    val emaWeight: Float,

    /** Average daily calories over the evaluation period */
    @ColumnInfo(name = "avg_calories_7d")
    val avgCalories7d: Float,

    /** Rate of weight change (kg/week) — negative = losing, positive = gaining */
    @ColumnInfo(name = "weight_change_rate")
    val weightChangeRate: Float,

    /** Phase at time of calculation: "DISCOVERY", "GOAL", or "MAINTENANCE" */
    val phase: String
)
