package com.caladapt.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caladapt.data.db.entity.DailySummaryEntity
import com.caladapt.data.db.entity.TDEEHistoryEntity
import com.caladapt.data.db.entity.WeightLogEntity
import com.caladapt.data.repository.CalorieRepository
import com.caladapt.data.repository.TDEERepository
import com.caladapt.data.repository.UserProfileRepository
import com.caladapt.data.repository.WeightRepository
import com.caladapt.domain.model.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// DATE RANGE ENUM
// ═══════════════════════════════════════════════════════════════════════════

enum class DateRange(val label: String, val days: Int?) {
    WEEK_1("1W", 7),
    WEEK_2("2W", 14),
    MONTH_1("1M", 30),
    MONTH_3("3M", 90),
    ALL("All", null)
}

// ═══════════════════════════════════════════════════════════════════════════
// AGGREGATION DATA CLASSES
// ═══════════════════════════════════════════════════════════════════════════

data class WeeklyAverage(
    val weekLabel: String,           // e.g. "Jun 23–29"
    val avgCalories: Int,
    val avgWeight: Float?,           // in kg (null if no weight logs that week)
    val compliance: Float,           // 0..1 (actual / target ratio)
    val weightChange: Float?         // kg delta from previous week's avg weight
)

data class MacroAggregate(
    val avgProtein: Float = 0f,
    val avgCarbs: Float = 0f,
    val avgFat: Float = 0f,
    val totalDays: Int = 0
) {
    val totalMacroGrams: Float get() = avgProtein + avgCarbs + avgFat
    val proteinPct: Float get() = if (totalMacroGrams > 0) avgProtein / totalMacroGrams else 0f
    val carbsPct: Float get() = if (totalMacroGrams > 0) avgCarbs / totalMacroGrams else 0f
    val fatPct: Float get() = if (totalMacroGrams > 0) avgFat / totalMacroGrams else 0f
}

// ═══════════════════════════════════════════════════════════════════════════
// STATE
// ═══════════════════════════════════════════════════════════════════════════

data class AnalyticsState(
    val dateRange: DateRange = DateRange.MONTH_1,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val weightData: List<WeightLogEntity> = emptyList(),
    val calorieData: List<DailySummaryEntity> = emptyList(),
    val tdeeData: List<TDEEHistoryEntity> = emptyList(),
    val weeklyAverages: List<WeeklyAverage> = emptyList(),
    val macroTotals: MacroAggregate = MacroAggregate(),
    val isLoading: Boolean = true
) {
    val hasAnyData: Boolean
        get() = weightData.isNotEmpty() || calorieData.isNotEmpty() || tdeeData.isNotEmpty()
}

// ═══════════════════════════════════════════════════════════════════════════
// VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val weightRepository: WeightRepository,
    private val calorieRepository: CalorieRepository,
    private val tdeeRepository: TDEERepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadProfile()
        loadData()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = userProfileRepository.getProfileOnce() ?: return@launch
            val unitSystem = try {
                UnitSystem.valueOf(profile.unitSystem)
            } catch (_: Exception) {
                UnitSystem.METRIC
            }
            _state.update { it.copy(unitSystem = unitSystem) }
        }
    }

    fun selectDateRange(range: DateRange) {
        _state.update { it.copy(dateRange = range, isLoading = true) }
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val range = _state.value.dateRange
                val endDate = LocalDate.now()
                val startDate = range.days?.let { endDate.minusDays(it.toLong()) }

                // Load weight data
                val weights = if (startDate != null) {
                    weightRepository.getWeightsBetween(startDate, endDate)
                } else {
                    weightRepository.getAllWeights()
                }

                // Load calorie summaries
                val summaries = if (startDate != null) {
                    calorieRepository.getSummariesBetween(startDate, endDate)
                } else {
                    // Get all by using a very early start date
                    calorieRepository.getSummariesBetween(
                        LocalDate.of(2020, 1, 1), endDate
                    )
                }

                // Load TDEE history
                val tdee = if (startDate != null) {
                    tdeeRepository.getTDEEBetween(startDate, endDate)
                } else {
                    tdeeRepository.getAllTDEE()
                }

                // Compute weekly averages
                val weeklyAvgs = computeWeeklyAverages(summaries, weights)

                // Compute macro aggregate
                val macros = computeMacroAggregate(summaries)

                // Apply 90-day bucket downsampling if necessary (Stage 7E)
                val downsampledWeights = downsampleWeights(weights)
                val downsampledSummaries = downsampleSummaries(summaries)
                val downsampledTdee = downsampleTdee(tdee)

                _state.update {
                    it.copy(
                        weightData = downsampledWeights,
                        calorieData = downsampledSummaries,
                        tdeeData = downsampledTdee,
                        weeklyAverages = weeklyAvgs,
                        macroTotals = macros,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun downsampleWeights(data: List<WeightLogEntity>): List<WeightLogEntity> {
        if (data.size <= 90) return data
        val bucketSize = data.size.toDouble() / 90
        val downsampled = mutableListOf<WeightLogEntity>()
        for (i in 0 until 90) {
            val startIdx = (i * bucketSize).toInt()
            val endIdx = ((i + 1) * bucketSize).toInt().coerceAtMost(data.size)
            if (startIdx < endIdx) {
                val sublist = data.subList(startIdx, endIdx)
                val avgRaw = sublist.map { it.weightKg }.average().toFloat()
                val avgEma = sublist.map { it.emaWeight }.average().toFloat()
                downsampled.add(
                    WeightLogEntity(
                        date = sublist.last().date,
                        weightKg = avgRaw,
                        emaWeight = avgEma,
                        loggedAt = sublist.last().loggedAt,
                        isCycleWaterRetention = sublist.any { it.isCycleWaterRetention }
                    )
                )
            }
        }
        return downsampled
    }

    private fun downsampleSummaries(data: List<DailySummaryEntity>): List<DailySummaryEntity> {
        if (data.size <= 90) return data
        val bucketSize = data.size.toDouble() / 90
        val downsampled = mutableListOf<DailySummaryEntity>()
        for (i in 0 until 90) {
            val startIdx = (i * bucketSize).toInt()
            val endIdx = ((i + 1) * bucketSize).toInt().coerceAtMost(data.size)
            if (startIdx < endIdx) {
                val sublist = data.subList(startIdx, endIdx)
                downsampled.add(
                    DailySummaryEntity(
                        date = sublist.last().date,
                        totalCalories = sublist.map { it.totalCalories }.average().toInt(),
                        totalProtein = sublist.map { it.totalProtein }.average().toFloat(),
                        totalCarbs = sublist.map { it.totalCarbs }.average().toFloat(),
                        totalFat = sublist.map { it.totalFat }.average().toFloat(),
                        calorieTarget = sublist.map { it.calorieTarget }.average().toInt()
                    )
                )
            }
        }
        return downsampled
    }

    private fun downsampleTdee(data: List<TDEEHistoryEntity>): List<TDEEHistoryEntity> {
        if (data.size <= 90) return data
        val bucketSize = data.size.toDouble() / 90
        val downsampled = mutableListOf<TDEEHistoryEntity>()
        for (i in 0 until 90) {
            val startIdx = (i * bucketSize).toInt()
            val endIdx = ((i + 1) * bucketSize).toInt().coerceAtMost(data.size)
            if (startIdx < endIdx) {
                val sublist = data.subList(startIdx, endIdx)
                downsampled.add(
                    TDEEHistoryEntity(
                        date = sublist.last().date,
                        calculatedTDEE = sublist.map { it.calculatedTDEE }.average().toFloat(),
                        emaWeight = sublist.map { it.emaWeight }.average().toFloat(),
                        avgCalories7d = sublist.map { it.avgCalories7d }.average().toFloat(),
                        weightChangeRate = sublist.map { it.weightChangeRate }.average().toFloat(),
                        phase = sublist.last().phase
                    )
                )
            }
        }
        return downsampled
    }

    /**
     * Group daily summaries + weights by ISO week and compute averages.
     */
    private fun computeWeeklyAverages(
        summaries: List<DailySummaryEntity>,
        weights: List<WeightLogEntity>
    ): List<WeeklyAverage> {
        if (summaries.isEmpty()) return emptyList()

        val weekFields = WeekFields.of(Locale.getDefault())

        // Group summaries by year-week
        data class YearWeek(val year: Int, val week: Int)

        val summaryByWeek = summaries.groupBy { summary ->
            val date = LocalDate.parse(summary.date, dateFormatter)
            YearWeek(date.get(weekFields.weekBasedYear()), date.get(weekFields.weekOfWeekBasedYear()))
        }

        // Group weights by year-week
        val weightByWeek = weights.groupBy { w ->
            val date = LocalDate.parse(w.date, dateFormatter)
            YearWeek(date.get(weekFields.weekBasedYear()), date.get(weekFields.weekOfWeekBasedYear()))
        }

        val sortedWeeks = summaryByWeek.keys.sortedWith(compareBy({ it.year }, { it.week }))

        val result = mutableListOf<WeeklyAverage>()
        var prevAvgWeight: Float? = null

        for (yw in sortedWeeks) {
            val weekSummaries = summaryByWeek[yw] ?: continue
            val weekWeights = weightByWeek[yw] ?: emptyList()

            // Compute average calories
            val avgCal = weekSummaries.map { it.totalCalories }.average().toInt()

            // Compute average weight (EMA)
            val avgWeight = if (weekWeights.isNotEmpty()) {
                weekWeights.map { it.emaWeight }.average().toFloat()
            } else null

            // Compliance: avg(actual / target) where target > 0
            val complianceDays = weekSummaries.filter { it.calorieTarget > 0 }
            val compliance = if (complianceDays.isNotEmpty()) {
                complianceDays.map {
                    (it.totalCalories.toFloat() / it.calorieTarget).coerceIn(0f, 2f)
                }.average().toFloat()
            } else 0f

            // Weight change from previous week
            val weightDelta = if (avgWeight != null && prevAvgWeight != null) {
                avgWeight - prevAvgWeight!!
            } else null

            // Build week label (e.g. "Jun 23–29")
            val weekDates = weekSummaries.map { LocalDate.parse(it.date, dateFormatter) }
            val firstDay = weekDates.min()
            val lastDay = weekDates.max()
            val monthFormatter = DateTimeFormatter.ofPattern("MMM d")
            val weekLabel = "${firstDay.format(monthFormatter)}–${lastDay.dayOfMonth}"

            result.add(
                WeeklyAverage(
                    weekLabel = weekLabel,
                    avgCalories = avgCal,
                    avgWeight = avgWeight,
                    compliance = compliance,
                    weightChange = weightDelta
                )
            )

            if (avgWeight != null) prevAvgWeight = avgWeight
        }

        return result
    }

    /**
     * Average daily macros across all summaries with logged data.
     */
    private fun computeMacroAggregate(summaries: List<DailySummaryEntity>): MacroAggregate {
        val daysWithData = summaries.filter {
            it.totalProtein > 0 || it.totalCarbs > 0 || it.totalFat > 0
        }
        if (daysWithData.isEmpty()) return MacroAggregate()

        return MacroAggregate(
            avgProtein = daysWithData.map { it.totalProtein }.average().toFloat(),
            avgCarbs = daysWithData.map { it.totalCarbs }.average().toFloat(),
            avgFat = daysWithData.map { it.totalFat }.average().toFloat(),
            totalDays = daysWithData.size
        )
    }
}
