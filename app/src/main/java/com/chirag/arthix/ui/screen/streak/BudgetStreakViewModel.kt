package com.chirag.arthix.ui.screen.streak

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.BudgetStreakEntity
import com.chirag.arthix.data.model.StreakDay
import com.chirag.arthix.data.repository.BudgetStreakRepository
import com.chirag.arthix.ui.navigation.ArthixRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.TimeZone

@HiltViewModel
class BudgetStreakViewModel @Inject constructor(
    private val repository: BudgetStreakRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val streakId: Long = savedStateHandle.get<Long>(ArthixRoute.BudgetStreak.ARG_STREAK_ID) ?: 0L

    private val _streak = MutableStateFlow<BudgetStreakEntity?>(null)
    val streak: StateFlow<BudgetStreakEntity?> = _streak.asStateFlow()

    private val _days = MutableStateFlow<List<StreakDay>>(emptyList())
    val days: StateFlow<List<StreakDay>> = _days.asStateFlow()
    
    private val _daysElapsed = MutableStateFlow(0)
    val daysElapsed: StateFlow<Int> = _daysElapsed.asStateFlow()

    init {
        if (streakId > 0) {
            loadStreak()
            observeDays()
        }
    }

    private fun loadStreak() {
        viewModelScope.launch {
            val loadedStreak = repository.getStreakById(streakId)
            _streak.value = loadedStreak
            
            if (loadedStreak != null) {
                // Compute elapsed days based on start date
                val millis = System.currentTimeMillis()
                val offset = TimeZone.getDefault().getOffset(millis)
                val todayEpochDay = (millis + offset) / 86400000L
                
                val elapsed = (todayEpochDay - loadedStreak.startDate + 1).toInt()
                _daysElapsed.value = elapsed.coerceIn(1, loadedStreak.daysInPeriod)
            }
        }
    }

    private fun observeDays() {
        viewModelScope.launch {
            repository.getStreakDays(streakId).collectLatest { updatedDays ->
                _days.value = updatedDays
            }
        }
    }
    
    fun deactivateStreak() {
        viewModelScope.launch {
            repository.deactivateStreak(streakId)
        }
    }
}
