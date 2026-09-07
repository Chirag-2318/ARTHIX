package com.chirag.arthix.ui.screen.moneylog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirag.arthix.data.dao.MoneyLogDao
import com.chirag.arthix.data.entity.MoneyLogEntity
import com.chirag.arthix.data.model.MoneyLogCategory
import com.chirag.arthix.data.model.MoneyLogDateMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class ParsedImportRow(
    val lineNum: Int,
    val originalText: String,
    val entity: MoneyLogEntity?,
    val error: String?
)

@HiltViewModel
class MoneyLogViewModel @Inject constructor(
    private val moneyLogDao: MoneyLogDao
) : ViewModel() {

    val entries: StateFlow<List<MoneyLogEntity>> = moneyLogDao.getAllEntriesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun insertEntry(entity: MoneyLogEntity) {
        viewModelScope.launch {
            moneyLogDao.insert(entity)
        }
    }

    fun deleteEntry(entity: MoneyLogEntity) {
        viewModelScope.launch {
            moneyLogDao.delete(entity)
        }
    }

    fun parseBulkImportText(text: String): List<ParsedImportRow> {
        val lines = text.trim().split("\n").filter { it.isNotBlank() }
        val results = mutableListOf<ParsedImportRow>()
        
        val exactFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        
        for ((index, line) in lines.withIndex()) {
            try {
                val parts = line.split("|").map { it.trim() }
                if (parts.size < 4) {
                    results.add(ParsedImportRow(index + 1, line, null, "Expected 4 parts, got ${parts.size}"))
                    continue
                }
                
                val catStr = parts[0]
                val desc = parts[1]
                val amtStr = parts[2]
                val dateStr = parts[3]
                
                val category = try {
                    MoneyLogCategory.valueOf(catStr.uppercase())
                } catch (e: Exception) {
                    results.add(ParsedImportRow(index + 1, line, null, "Invalid category: $catStr"))
                    continue
                }
                
                val amount = try {
                    amtStr.replace(",", "").toDouble()
                } catch (e: Exception) {
                    results.add(ParsedImportRow(index + 1, line, null, "Invalid amount: $amtStr"))
                    continue
                }
                
                var dateMode = MoneyLogDateMode.UNKNOWN
                var exactDateMillis: Long? = null
                var rangeStartMillis: Long? = null
                var rangeEndMillis: Long? = null
                var approxStr: String? = null
                
                if (dateStr.equals("unknown", ignoreCase = true)) {
                    dateMode = MoneyLogDateMode.UNKNOWN
                } else if (dateStr.contains(" to ", ignoreCase = true)) {
                    dateMode = MoneyLogDateMode.RANGE
                    val dates = dateStr.split(" to ", ignoreCase = true).map { it.trim() }
                    if (dates.size == 2) {
                        rangeStartMillis = exactFormat.parse(dates[0])?.time
                        rangeEndMillis = exactFormat.parse(dates[1])?.time
                        if (rangeStartMillis == null || rangeEndMillis == null) {
                            results.add(ParsedImportRow(index + 1, line, null, "Invalid date range format"))
                            continue
                        }
                    } else {
                        results.add(ParsedImportRow(index + 1, line, null, "Invalid date range format"))
                        continue
                    }
                } else if (dateStr.matches(Regex("\\d{2}-\\d{4}"))) { // mm-yyyy
                    dateMode = MoneyLogDateMode.APPROXIMATE
                    approxStr = dateStr
                } else {
                    dateMode = MoneyLogDateMode.EXACT
                    exactDateMillis = exactFormat.parse(dateStr)?.time
                    if (exactDateMillis == null) {
                        results.add(ParsedImportRow(index + 1, line, null, "Invalid exact date format"))
                        continue
                    }
                }
                
                val entity = MoneyLogEntity(
                    category = category,
                    customCategoryLabel = if (category == MoneyLogCategory.OTHER) catStr else null,
                    description = desc,
                    amount = amount,
                    dateMode = dateMode,
                    exactDateMillis = exactDateMillis,
                    rangeStartMillis = rangeStartMillis,
                    rangeEndMillis = rangeEndMillis,
                    approximateMonthYear = approxStr,
                    note = null,
                    createdAt = System.currentTimeMillis()
                )
                results.add(ParsedImportRow(index + 1, line, entity, null))
            } catch (e: Exception) {
                results.add(ParsedImportRow(index + 1, line, null, "Parse error: ${e.message}"))
            }
        }
        
        return results
    }

    fun commitBulkImport(entities: List<MoneyLogEntity>) {
        viewModelScope.launch {
            moneyLogDao.insertAll(entities)
        }
    }
}
