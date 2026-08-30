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

/**
 * UI model for the report screen — maps 1:1 from ReportEntity (PRD §7).
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
        observeReports()
        generateReportNow()
    }

    private fun observeReports() {
        reportRepository.observeAll()
            .onEach { reports ->
                if (reports.isNotEmpty()) {
                    val latest = reports.first()
                    _uiState.update {
                        it.copy(
                            report = mapToUiModel(latest),
                            isLoading = false,
                            error = null,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun generateReportNow() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val report = reportGenerator.generateAndSaveReport(ReportPeriod.currentWeek())
                _uiState.update {
                    it.copy(
                        report = mapToUiModel(report),
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

    private fun mapToUiModel(entity: ReportEntity): ReportUiModel {
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val label = "${dateFormat.format(Date(entity.periodStart))} – ${dateFormat.format(Date(entity.periodEnd))}"

        val mapType = object : TypeToken<Map<String, Long>>() {}.type
        val categoryBreakdown: Map<String, Long> = try {
            gson.fromJson(entity.categoryBreakdownJson, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        val listType = object : TypeToken<List<String>>() {}.type
        val suggestions: List<String> = try {
            gson.fromJson(entity.suggestionsJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return ReportUiModel(
            periodLabel = label,
            categoryBreakdown = categoryBreakdown,
            netFlowPaise = entity.netFlowPaise,
            suggestions = suggestions,
            projectedTotalPaise = entity.projectedTotalPaise,
            projectedSavingsPaise = entity.projectedSavingsPaise,
            uncategorizedTotalPaise = entity.uncategorizedTotalPaise,
            noPriorData = entity.netFlowPaise == 0L || suggestions.any { it.contains("first week", ignoreCase = true) },
        )
    }
}
