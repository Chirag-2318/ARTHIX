package com.chirag.arthix.ui.screen.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.entity.ReportEntity
import com.chirag.arthix.data.repository.ReportRepository
import com.chirag.arthix.report.ReportGenerator
import com.chirag.arthix.report.model.ReportPeriod
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.chirag.arthix.report.model.ComputedReportData
import com.chirag.arthix.report.model.ReportPeriodType
import com.chirag.arthix.report.model.CategoryTrend

/**
 * UI model for the report screen — maps 1:1 from ReportEntity (PRD §7).
 */
data class ReportUiModel(
    val periodLabel: String = "",
    val categoryBreakdown: Map<String, Long> = emptyMap(),
    val netFlowPaise: Long = 0,
    val totalInflowPaise: Long = 0,
    val totalOutflowPaise: Long = 0,
    val prevNetFlowPaise: Long = 0,
    val suggestions: List<String> = emptyList(),
    val trendingCategories: List<CategoryTrend> = emptyList(),
    val projectedTotalPaise: Long = 0,
    val projectedSavingsPaise: Long = 0,
    val uncategorizedTotalPaise: Long = 0,
    val noPriorData: Boolean = true,
)

data class ReportUiState(
    val report: ReportUiModel? = null,
    val selectedPeriodType: ReportPeriodType = ReportPeriodType.WEEKLY,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel for [ReportScreen] (PRD §7, FR-7).
 *
 * Connects to [ReportRepository] and triggers on-device report generation
 * via [ReportGenerator].
 */
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val reportGenerator: ReportGenerator,
    private val gson: Gson,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState(isLoading = true))
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        generateReportForPeriod(ReportPeriodType.WEEKLY)
    }

    fun selectPeriodType(type: ReportPeriodType) {
        _uiState.update { it.copy(selectedPeriodType = type) }

        generateReportForPeriod(type)
    }

    private fun generateReportForPeriod(type: ReportPeriodType) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // Generate and save to history, but we also compute it to keep transient fields
                val computedData = reportGenerator.computeOnly(type)
                val reportEntity = reportGenerator.generateAndSaveReport(type)

                _uiState.update {
                    it.copy(
                        report = mapComputedToUiModel(computedData, reportEntity),
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to generate report: ${e.message}",
                    )
                }
            }
        }
    }

    private fun mapComputedToUiModel(data: ComputedReportData, entity: com.chirag.arthix.data.entity.ReportEntity): ReportUiModel {
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val label = data.period.label

        val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
        val suggestions: List<String> = try {
            gson.fromJson(entity.suggestionsJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return ReportUiModel(
            periodLabel = label,
            categoryBreakdown = data.categoryBreakdown,
            netFlowPaise = data.netFlowPaise,
            totalInflowPaise = data.totalInflowPaise,
            totalOutflowPaise = data.totalOutflowPaise,
            prevNetFlowPaise = data.prevNetFlowPaise,
            suggestions = suggestions,
            trendingCategories = data.trendingCategories,
            projectedTotalPaise = data.projectedTotalPaise,
            projectedSavingsPaise = data.projectedSavingsPaise,
            uncategorizedTotalPaise = data.uncategorizedTotalPaise,
            noPriorData = data.noPriorData,
        )
    }
}
