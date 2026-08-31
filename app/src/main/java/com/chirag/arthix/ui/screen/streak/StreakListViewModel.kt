package com.chirag.arthix.ui.screen.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.BudgetStreakEntity
import com.chirag.arthix.data.repository.BudgetStreakRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreakListViewModel @Inject constructor(
    private val repository: BudgetStreakRepository
) : ViewModel() {

    private val _streaks = MutableStateFlow<List<BudgetStreakEntity>>(emptyList())
    val streaks: StateFlow<List<BudgetStreakEntity>> = _streaks.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getActiveStreaks().collectLatest {
                _streaks.value = it
            }
        }
    }
}
