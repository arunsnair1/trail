package com.caladapt.ui.screens.measurements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caladapt.data.db.entity.BodyMeasurementEntity
import com.caladapt.data.repository.BodyMeasurementRepository
import com.caladapt.data.repository.UserProfileRepository
import com.caladapt.domain.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class MeasurementTab { LOG, HISTORY }

/**
 * A single measurement field's display data, including the delta from last measurement.
 */
data class MeasurementDelta(
    val label: String,
    val currentCm: Float?,
    val previousCm: Float?,
    val unitSystem: UnitSystem
) {
    val deltaDisplay: String?
        get() {
            val curr = currentCm ?: return null
            val prev = previousCm ?: return null
            val diff = unitSystem.fromCm(curr) - unitSystem.fromCm(prev)
            if (kotlin.math.abs(diff) < 0.05f) return null
            val sign = if (diff > 0) "+" else ""
            return "$sign${"%.1f".format(diff)} ${unitSystem.lengthLabel}"
        }

    val isPositiveDelta: Boolean
        get() {
            val curr = currentCm ?: return false
            val prev = previousCm ?: return false
            return curr > prev
        }
}

data class MeasurementsState(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val isMale: Boolean = true,
    val heightCm: Float = 170f,
    val currentTab: MeasurementTab = MeasurementTab.LOG,

    // ── Log tab inputs ──
    val chestInput: String = "",
    val waistInput: String = "",
    val hipsInput: String = "",
    val leftBicepInput: String = "",
    val rightBicepInput: String = "",
    val leftThighInput: String = "",
    val rightThighInput: String = "",
    val neckInput: String = "",
    val forearmInput: String = "",
    val calfInput: String = "",
    val bodyFatInput: String = "",
    val notesInput: String = "",

    // ── Computed ──
    val autoBodyFat: Float? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null,

    // ── History tab ──
    val history: List<BodyMeasurementEntity> = emptyList(),
    val lastMeasurement: BodyMeasurementEntity? = null
) {
    val lengthLabel: String get() = unitSystem.lengthLabel

    val hasAnyInput: Boolean
        get() = listOf(
            chestInput, waistInput, hipsInput,
            leftBicepInput, rightBicepInput,
            leftThighInput, rightThighInput,
            neckInput, forearmInput, calfInput
        ).any { it.isNotBlank() }

    /**
     * Build deltas for each measurement field against [lastMeasurement].
     */
    fun deltas(): List<MeasurementDelta> {
        val prev = lastMeasurement
        return listOf(
            MeasurementDelta("Chest", chestInput.toFloatOrNull()?.let { unitSystem.toCm(it) }, prev?.chestCm, unitSystem),
            MeasurementDelta("Waist", waistInput.toFloatOrNull()?.let { unitSystem.toCm(it) }, prev?.waistCm, unitSystem),
            MeasurementDelta("Hips", hipsInput.toFloatOrNull()?.let { unitSystem.toCm(it) }, prev?.hipsCm, unitSystem),
            MeasurementDelta("L. Bicep", leftBicepInput.toFloatOrNull()?.let { unitSystem.toCm(it) }, prev?.leftBicepCm, unitSystem),
            MeasurementDelta("R. Bicep", rightBicepInput.toFloatOrNull()?.let { unitSystem.toCm(it) }, prev?.rightBicepCm, unitSystem),
            MeasurementDelta("L. Thigh", leftThighInput.toFloatOrNull()?.let { unitSystem.toCm(it) }, prev?.leftThighCm, unitSystem),
            MeasurementDelta("R. Thigh", rightThighInput.toFloatOrNull()?.let { unitSystem.toCm(it) }, prev?.rightThighCm, unitSystem),
            MeasurementDelta("Neck", neckInput.toFloatOrNull()?.let { unitSystem.toCm(it) }, prev?.neckCm, unitSystem),
            MeasurementDelta("Forearm", forearmInput.toFloatOrNull()?.let { unitSystem.toCm(it) }, prev?.forearmCm, unitSystem),
            MeasurementDelta("Calf", calfInput.toFloatOrNull()?.let { unitSystem.toCm(it) }, prev?.calfCm, unitSystem)
        )
    }
}

@HiltViewModel
class MeasurementsViewModel @Inject constructor(
    private val measurementRepository: BodyMeasurementRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MeasurementsState())
    val state: StateFlow<MeasurementsState> = _state.asStateFlow()

    init {
        loadProfile()
        loadLastMeasurement()
        observeHistory()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = userProfileRepository.getProfileOnce() ?: return@launch
            val unitSystem = try {
                UnitSystem.valueOf(profile.unitSystem)
            } catch (_: Exception) {
                UnitSystem.METRIC
            }
            val isMale = profile.sex.equals("MALE", ignoreCase = true)
            _state.update {
                it.copy(
                    unitSystem = unitSystem,
                    isMale = isMale,
                    heightCm = profile.heightCm
                )
            }
        }
    }

    private fun loadLastMeasurement() {
        viewModelScope.launch {
            val last = measurementRepository.getLatestMeasurement()
            _state.update { it.copy(lastMeasurement = last) }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            measurementRepository.getAllMeasurementsFlow().collect { list ->
                _state.update { it.copy(history = list) }
            }
        }
    }

    // ── Tab switching ──

    fun selectTab(tab: MeasurementTab) {
        _state.update { it.copy(currentTab = tab) }
    }

    // ── Input handlers ──

    fun updateChest(v: String) { if (isValidNumInput(v)) _state.update { it.copy(chestInput = v) } }
    fun updateWaist(v: String) {
        if (isValidNumInput(v)) {
            _state.update { it.copy(waistInput = v) }
            recalcBodyFat()
        }
    }
    fun updateHips(v: String) {
        if (isValidNumInput(v)) {
            _state.update { it.copy(hipsInput = v) }
            recalcBodyFat()
        }
    }
    fun updateLeftBicep(v: String) { if (isValidNumInput(v)) _state.update { it.copy(leftBicepInput = v) } }
    fun updateRightBicep(v: String) { if (isValidNumInput(v)) _state.update { it.copy(rightBicepInput = v) } }
    fun updateLeftThigh(v: String) { if (isValidNumInput(v)) _state.update { it.copy(leftThighInput = v) } }
    fun updateRightThigh(v: String) { if (isValidNumInput(v)) _state.update { it.copy(rightThighInput = v) } }
    fun updateNeck(v: String) {
        if (isValidNumInput(v)) {
            _state.update { it.copy(neckInput = v) }
            recalcBodyFat()
        }
    }
    fun updateForearm(v: String) { if (isValidNumInput(v)) _state.update { it.copy(forearmInput = v) } }
    fun updateCalf(v: String) { if (isValidNumInput(v)) _state.update { it.copy(calfInput = v) } }
    fun updateBodyFat(v: String) { if (isValidNumInput(v)) _state.update { it.copy(bodyFatInput = v) } }
    fun updateNotes(v: String) { _state.update { it.copy(notesInput = v) } }

    private fun isValidNumInput(value: String): Boolean =
        value.isEmpty() || value == "." || value.toFloatOrNull() != null

    // ── Navy body fat auto-calc ──

    private fun recalcBodyFat() {
        val s = _state.value
        val waistCm = s.waistInput.toFloatOrNull()?.let { s.unitSystem.toCm(it) }
        val neckCm = s.neckInput.toFloatOrNull()?.let { s.unitSystem.toCm(it) }
        val hipsCm = s.hipsInput.toFloatOrNull()?.let { s.unitSystem.toCm(it) }

        if (waistCm != null && neckCm != null && waistCm > neckCm) {
            val bf = measurementRepository.calculateNavyBodyFat(
                isMale = s.isMale,
                waistCm = waistCm,
                neckCm = neckCm,
                heightCm = s.heightCm,
                hipsCm = hipsCm
            )
            _state.update { it.copy(autoBodyFat = bf) }
        } else {
            _state.update { it.copy(autoBodyFat = null) }
        }
    }

    // ── Unit toggle ──

    fun toggleUnit() {
        val s = _state.value
        val newUnit = if (s.unitSystem == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC

        fun convert(input: String): String {
            val v = input.toFloatOrNull() ?: return input
            // old unit → cm → new unit
            val cm = s.unitSystem.toCm(v)
            return "%.1f".format(newUnit.fromCm(cm))
        }

        _state.update {
            it.copy(
                unitSystem = newUnit,
                chestInput = convert(it.chestInput),
                waistInput = convert(it.waistInput),
                hipsInput = convert(it.hipsInput),
                leftBicepInput = convert(it.leftBicepInput),
                rightBicepInput = convert(it.rightBicepInput),
                leftThighInput = convert(it.leftThighInput),
                rightThighInput = convert(it.rightThighInput),
                neckInput = convert(it.neckInput),
                forearmInput = convert(it.forearmInput),
                calfInput = convert(it.calfInput)
            )
        }
    }

    // ── Save ──

    fun saveMeasurement() {
        val s = _state.value
        if (!s.hasAnyInput) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            try {
                fun toCmOrNull(input: String): Float? =
                    input.toFloatOrNull()?.let { s.unitSystem.toCm(it) }

                // Use manual body fat if entered, otherwise auto
                val bodyFat = s.bodyFatInput.toFloatOrNull() ?: s.autoBodyFat

                val entity = BodyMeasurementEntity(
                    date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    chestCm = toCmOrNull(s.chestInput),
                    waistCm = toCmOrNull(s.waistInput),
                    hipsCm = toCmOrNull(s.hipsInput),
                    leftBicepCm = toCmOrNull(s.leftBicepInput),
                    rightBicepCm = toCmOrNull(s.rightBicepInput),
                    leftThighCm = toCmOrNull(s.leftThighInput),
                    rightThighCm = toCmOrNull(s.rightThighInput),
                    neckCm = toCmOrNull(s.neckInput),
                    forearmCm = toCmOrNull(s.forearmInput),
                    calfCm = toCmOrNull(s.calfInput),
                    bodyFatPct = bodyFat,
                    notes = s.notesInput.trim(),
                    loggedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )

                measurementRepository.logMeasurement(entity)

                // Reload last measurement for delta comparison
                val latest = measurementRepository.getLatestMeasurement()

                _state.update {
                    it.copy(
                        isSaving = false,
                        savedSuccessfully = true,
                        lastMeasurement = latest,
                        // Clear form
                        chestInput = "", waistInput = "", hipsInput = "",
                        leftBicepInput = "", rightBicepInput = "",
                        leftThighInput = "", rightThighInput = "",
                        neckInput = "", forearmInput = "", calfInput = "",
                        bodyFatInput = "", notesInput = "",
                        autoBodyFat = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, error = "Failed to save: ${e.message}")
                }
            }
        }
    }

    fun dismissSuccess() {
        _state.update { it.copy(savedSuccessfully = false) }
    }

    fun deleteMeasurement(measurement: BodyMeasurementEntity) {
        viewModelScope.launch {
            measurementRepository.deleteMeasurement(measurement)
            loadLastMeasurement()
        }
    }
}
