package com.caladapt.domain.model

/**
 * Unit system preference for weight and measurements.
 *
 * METRIC: kg, cm
 * IMPERIAL: lbs, inches
 */
enum class UnitSystem {
    METRIC,
    IMPERIAL;

    /** Convert kg to display unit */
    fun fromKg(kg: Float): Float = when (this) {
        METRIC -> kg
        IMPERIAL -> kg * 2.20462f
    }

    /** Convert display unit to kg (internal storage is always kg) */
    fun toKg(displayWeight: Float): Float = when (this) {
        METRIC -> displayWeight
        IMPERIAL -> displayWeight / 2.20462f
    }

    /** Convert cm to display unit */
    fun fromCm(cm: Float): Float = when (this) {
        METRIC -> cm
        IMPERIAL -> cm / 2.54f
    }

    /** Convert display unit to cm (internal storage is always cm) */
    fun toCm(displayLength: Float): Float = when (this) {
        METRIC -> displayLength
        IMPERIAL -> displayLength * 2.54f
    }

    val weightLabel: String get() = when (this) {
        METRIC -> "kg"
        IMPERIAL -> "lbs"
    }

    val lengthLabel: String get() = when (this) {
        METRIC -> "cm"
        IMPERIAL -> "in"
    }
}
