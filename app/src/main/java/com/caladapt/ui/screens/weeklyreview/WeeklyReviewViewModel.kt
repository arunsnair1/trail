package com.caladapt.ui.screens.weeklyreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caladapt.domain.usecase.EvaluateWeeklyProgressUseCase
import com.caladapt.domain.usecase.WeeklyReview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyReviewState(
    val review: WeeklyReview? = null,
    val isLoading: Boolean = true,
    val currentStep: Int = 0 // 0 = Recap, 1 = Metabolism Update, 2 = New Plan
)

@HiltViewModel
class WeeklyReviewViewModel @Inject constructor(
    private val evaluateWeeklyProgressUseCase: EvaluateWeeklyProgressUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WeeklyReviewState())
    val state = _state.asStateFlow()

    init {
        runReview()
    }

    private fun runReview() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val reviewResult = evaluateWeeklyProgressUseCase()
            _state.update { 
                it.copy(
                    review = reviewResult,
                    isLoading = false
                ) 
            }
        }
    }

    fun nextStep() {
        if (_state.value.currentStep < 2) {
            _state.update { it.copy(currentStep = it.currentStep + 1) }
        }
    }

    fun previousStep() {
        if (_state.value.currentStep > 0) {
            _state.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }
}
