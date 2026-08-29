package com.chirag.arthix.ui.screen.report

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * UI model for the report screen — maps 1:1 from ReportEntity (PRD §7).
 *
 * Phase 5 populates this via the repository; this phase provides the
 * rendering shell tested against hand-authored fakes.
 */
data class ReportUiModel(
    val periodLabel: String = "",
    val categoryBreakdown: Map<String, Long> = emptyMap(),
    val netFlowPaise: Long = 0,
    val suggestions: List<String> = emptyList(),
    val projectedTotalPaise: Long = 0,
    val projectedSavingsPaise: Long = 0,
    val uncategorizedTotalPaise: Long = 0,
    val noPriorData: Boolean = true,
)

data class ReportUiState(
    val report: ReportUiModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel for [ReportScreen] (PRD §7 — rendering shell only).
 *
 * Phase 5 will supply real data through the ReportRepository.
 * This phase uses a hand-authored fake for layout verification.
 */
@HiltViewModel
class ReportViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReportUiState(
            report = ReportUiModel(
                periodLabel = "No report generated yet",
                noPriorData = true,
            ),
            isLoading = false,
        )
    )
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()
}
