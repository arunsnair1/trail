package com.caladapt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Body measurement log entry.
 * All measurements stored in cm internally.
 * Nullable fields allow users to log only what they measure.
 */
@Entity(tableName = "body_measurement")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Date in yyyy-MM-dd format */
    val date: String,

    @ColumnInfo(name = "chest_cm")
    val chestCm: Float? = null,

    @ColumnInfo(name = "waist_cm")
    val waistCm: Float? = null,

    @ColumnInfo(name = "hips_cm")
    val hipsCm: Float? = null,

    @ColumnInfo(name = "left_bicep_cm")
    val leftBicepCm: Float? = null,

    @ColumnInfo(name = "right_bicep_cm")
    val rightBicepCm: Float? = null,

    @ColumnInfo(name = "left_thigh_cm")
    val leftThighCm: Float? = null,

    @ColumnInfo(name = "right_thigh_cm")
    val rightThighCm: Float? = null,

    @ColumnInfo(name = "neck_cm")
    val neckCm: Float? = null,

    @ColumnInfo(name = "forearm_cm")
    val forearmCm: Float? = null,

    @ColumnInfo(name = "calf_cm")
    val calfCm: Float? = null,

    /** Body fat percentage — manual entry or calculated via Navy method */
    @ColumnInfo(name = "body_fat_pct")
    val bodyFatPct: Float? = null,

    /** Free-text notes */
    val notes: String = "",

    /** ISO-8601 timestamp */
    @ColumnInfo(name = "logged_at")
    val loggedAt: String
)
