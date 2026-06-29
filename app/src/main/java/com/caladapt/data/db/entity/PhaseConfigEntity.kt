package com.caladapt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase configuration — tracks the user's journey through
 * Discovery → Goal → Maintenance phases.
 *
 * Only one phase can be active at a time (is_active = true).
 * Previous phases are kept for historical context.
 */
@Entity(tableName = "phase_config")
data class PhaseConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** "DISCOVERY", "GOAL", or "MAINTENANCE" */
    val phase: String,

    /** "CUT", "BULK", "RECOMP", or empty for DISCOVERY/MAINTENANCE */
    val goal: String = "",

    /** Weight at start of this phase (kg) */
    @ColumnInfo(name = "starting_weight")
    val startingWeight: Float,

    /** Target weight for goal phase (kg), null for discovery */
    @ColumnInfo(name = "target_weight")
    val targetWeight: Float? = null,

    /** Current daily calorie target */
    @ColumnInfo(name = "daily_calorie_target")
    val dailyCalorieTarget: Int,

    /** Daily protein target (g) */
    @ColumnInfo(name = "protein_target")
    val proteinTarget: Float,

    /** Daily carbs target (g) */
    @ColumnInfo(name = "carbs_target")
    val carbsTarget: Float,

    /** Daily fat target (g) */
    @ColumnInfo(name = "fat_target")
    val fatTarget: Float,

    /** Current TDEE estimate (kcal/day) */
    @ColumnInfo(name = "tdee_estimate")
    val tdeeEstimate: Float,

    /** ISO-8601 datetime when this phase started */
    @ColumnInfo(name = "started_at")
    val startedAt: String,

    /** ISO-8601 datetime when this phase ended (null if active) */
    @ColumnInfo(name = "ended_at")
    val endedAt: String? = null,

    /** Only one phase is active at a time */
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
