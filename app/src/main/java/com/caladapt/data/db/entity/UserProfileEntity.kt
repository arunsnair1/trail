package com.caladapt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User profile data collected during onboarding.
 * Height is stored in cm, weight references are in kg internally.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    @ColumnInfo(name = "height_cm")
    val heightCm: Float,

    val age: Int,

    /** "MALE" or "FEMALE" — maps to Sex enum */
    val sex: String,

    /** "METRIC" or "IMPERIAL" — maps to UnitSystem enum */
    @ColumnInfo(name = "unit_system")
    val unitSystem: String,

    @ColumnInfo(name = "created_at")
    val createdAt: String, // ISO-8601 datetime

    /** Added in V2 - Tracks if the user is in the initial discovery phase */
    @ColumnInfo(name = "is_discovery_phase")
    val isDiscoveryPhase: Boolean = true,

    /** Added in V2 - Timestamp (epoch millis) when discovery started */
    @ColumnInfo(name = "discovery_start_date")
    val discoveryStartDate: Long = 0L
)
