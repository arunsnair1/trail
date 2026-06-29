package com.caladapt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Daily weight log. One entry per day max (unique date).
 * Weight is always stored in kg. EMA is computed on insert/update.
 */
@Entity(
    tableName = "weight_log",
    indices = [Index(value = ["date"], unique = true)]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Date in yyyy-MM-dd format */
    val date: String,

    /** Raw weight in kg */
    @ColumnInfo(name = "weight_kg")
    val weightKg: Float,

    /** Exponential moving average weight in kg (computed by app) */
    @ColumnInfo(name = "ema_weight")
    val emaWeight: Float,

    /** ISO-8601 timestamp of when the entry was logged */
    @ColumnInfo(name = "logged_at")
    val loggedAt: String,
    
    /** Added in V2 - Tags day as cycle/water retention for EMA alpha adjustment */
    @ColumnInfo(name = "is_cycle_water_retention")
    val isCycleWaterRetention: Boolean = false
)
