package com.chirag.arthix.ui.screen.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.GoalEntity
import com.chirag.arthix.data.entity.TransactionEntity
import com.chirag.arthix.data.model.GoalStatus
import com.chirag.arthix.data.repository.GoalRepository
import com.chirag.arthix.data.repository.TransactionRepository
import com.chirag.arthix.domain.goal.GeneratedGoalPlan
import com.chirag.arthix.domain.goal.GoalPlanGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalListUiState(
    val activeGoals: List<GoalEntity> = emptyList(),
    val completedGoals: List<GoalEntity> = emptyList(),
    val totalSavedPaise: Long = 0L,
    val totalTargetPaise: Long = 0L,
    val overallProgressPercent: Int = 0,
    val isLoading: Boolean = true,
    val planPreview: GeneratedGoalPlan? = null,
    val newlyCompletedGoal: GoalEntity? = null,
)

@HiltViewModel
class GoalListViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val planGenerator: GoalPlanGenerator,
) : ViewModel() {

    private val _planPreview = MutableStateFlow<GeneratedGoalPlan?>(null)
    val planPreview: StateFlow<GeneratedGoalPlan?> = _planPreview.asStateFlow()

    private val _newlyCompletedGoal = MutableStateFlow<GoalEntity?>(null)
    val newlyCompletedGoal: StateFlow<GoalEntity?> = _newlyCompletedGoal.asStateFlow()

    private var cachedTransactions: List<TransactionEntity> = emptyList()

    val uiState: StateFlow<GoalListUiState> = combine(
        goalRepository.observeAll(),
        transactionRepository.observeHistory(),
        _planPreview,
        _newlyCompletedGoal
    ) { goals, transactions, preview, newlyCompleted ->
        cachedTransactions = transactions

        val active = goals.filter { it.status == GoalStatus.ACTIVE && !it.isCompleted }
        val completed = goals.filter { it.isCompleted }

        val totalSaved = goals.sumOf { it.savedAmountPaise }
        val totalTarget = goals.sumOf { it.targetAmountPaise }
        val overallProgress = if (totalTarget > 0L) {
            ((totalSaved.toDouble() / totalTarget.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0

        GoalListUiState(
            activeGoals = active,
            completedGoals = completed,
            totalSavedPaise = totalSaved,
            totalTargetPaise = totalTarget,
            overallProgressPercent = overallProgress,
            isLoading = false,
            planPreview = preview,
            newlyCompletedGoal = newlyCompleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = GoalListUiState()
    )

    fun updatePlanPreview(targetAmountPaise: Long) {
        if (targetAmountPaise <= 0L) {
            _planPreview.value = null
            return
        }
        viewModelScope.launch {
            val plan = planGenerator.generatePlan(targetAmountPaise, cachedTransactions)
            _planPreview.value = plan
        }
    }

    fun clearPlanPreview() {
        _planPreview.value = null
    }

    fun addSavings(goalId: Long, amountPaise: Long) {
        if (amountPaise <= 0L) return
        viewModelScope.launch {
            val goal = goalRepository.getById(goalId) ?: return@launch
            val newTotal = goal.savedAmountPaise + amountPaise
            val willComplete = newTotal >= goal.targetAmountPaise

            goalRepository.addSavings(goalId, amountPaise)

            if (willComplete && goal.status != GoalStatus.COMPLETED) {
                goalRepository.markCompleted(goalId)
                _newlyCompletedGoal.value = goal.copy(savedAmountPaise = newTotal, status = GoalStatus.COMPLETED)
            }
        }
    }

    fun dismissCelebration() {
        _newlyCompletedGoal.value = null
    }

    fun createGoal(
        title: String,
        targetAmountPaise: Long,
        initialSavedPaise: Long = 0L,
        onSuccess: () -> Unit = {}
    ) {
        if (title.isBlank() || targetAmountPaise <= 0L) return

        viewModelScope.launch {
            val plan = _planPreview.value ?: planGenerator.generatePlan(targetAmountPaise, cachedTransactions)
            val entity = planGenerator.createEntity(
                title = title,
                targetAmountPaise = targetAmountPaise,
                plan = plan,
                initialSavedPaise = initialSavedPaise
            )
            val id = goalRepository.createGoal(entity)
            if (entity.isCompleted) {
                _newlyCompletedGoal.value = entity.copy(id = id)
            }
            _planPreview.value = null
            onSuccess()
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goalId)
        }
    }

    fun markCompleted(goalId: Long) {
        viewModelScope.launch {
            goalRepository.markCompleted(goalId)
            val goal = goalRepository.getById(goalId)
            if (goal != null) {
                _newlyCompletedGoal.value = goal
            }
        }
    }
}
