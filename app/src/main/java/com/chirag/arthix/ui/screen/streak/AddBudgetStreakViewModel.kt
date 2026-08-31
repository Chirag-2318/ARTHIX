package com.chirag.arthix.ui.screen.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.BudgetStreakEntity
import com.chirag.arthix.data.entity.StreakDailyCapEntity
import com.chirag.arthix.data.repository.BudgetStreakRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class AddBudgetStreakViewModel @Inject constructor(
    private val repository: BudgetStreakRepository
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun createStreak(
        category: String,
        label: String,
        amountPaise: Long,
        daysInPeriod: Int,
        distributionMode: String, // "EQUAL" or "CUSTOM"
        customCaps: List<Long>? = null, // only used if mode == CUSTOM
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // Determine start date (today's epoch day in local time)
                val millis = System.currentTimeMillis()
                val offset = TimeZone.getDefault().getOffset(millis)
                val startDateEpochDay = (millis + offset) / 86400000L
                
                val streakEntity = BudgetStreakEntity(
                    category = category,
                    label = label.ifBlank { null },
                    monthlyAmountPaise = amountPaise,
                    daysInPeriod = daysInPeriod,
                    startDate = startDateEpochDay,
                    distributionMode = distributionMode,
                    createdAt = System.currentTimeMillis()
                )
                
                val caps = mutableListOf<StreakDailyCapEntity>()
                if (distributionMode == "EQUAL") {
                    val baseCap = amountPaise / daysInPeriod
                    val remainder = amountPaise % daysInPeriod
                    for (i in 1..daysInPeriod) {
                        // Spread remainder over the first few days
                        val dailyCap = baseCap + if (i <= remainder) 1 else 0
                        caps.add(StreakDailyCapEntity(streakId = 0, dayIndex = i, capPaise = dailyCap))
                    }
                } else if (distributionMode == "CUSTOM" && customCaps != null) {
                    for (i in 1..daysInPeriod) {
                        val cap = customCaps.getOrNull(i - 1) ?: 0L
                        caps.add(StreakDailyCapEntity(streakId = 0, dayIndex = i, capPaise = cap))
                    }
                }
                
                repository.createStreak(streakEntity, caps)
                
                // Note: repository.createStreak doesn't return the ID right now, 
                // but since we want to navigate to success or list, we can just return 0 
                // and the UI can navigate back to the list screen.
                onSuccess(0L)
            } finally {
                _isSaving.value = false
            }
        }
    }
}
