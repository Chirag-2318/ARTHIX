package com.chirag.arthix.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- Theme Colors ---
val DashBg = Color(0xFF0F0F13)
val DashSurface = Color(0xFF1B1A21)
val DashSurfaceElevated = Color(0xFF232029)
val DashDivider = Color(0xFF2A2830)
val DashHeatEmpty = Color(0xFF29262F)
val DashGold = Color(0xFFF4C24C)
val DashOnGold = Color(0xFF241A02)
val DashTextPrimary = Color(0xFFF5F5F7)
val DashTextSecondary = Color(0xFFA7A5B0)
val DashTextMuted = Color(0xFF6E6C78)

// --- Models ---
data class HomeUiState(
    val userInitials: String = "",
    val dateLabel: String = "",
    val searchQuery: String = "",
    val categories: List<CategorySpendingUi> = emptyList()
)

data class CategorySpendingUi(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val goalLabel: String,
    val daysLogged: Int,
    val percentOfBudgetUsed: Int,
    val amountSpentRupees: Int,
    val heatmap: WeekHeatmap
)

data class WeekHeatmap(val weeks: List<List<Float?>>)

// --- Data Layer (Mock) ---
interface CategorySpendingRepository {
    fun observeCategorySummaries(): Flow<List<CategorySpendingUi>>
}

class MockCategoryRepository : CategorySpendingRepository {
    override fun observeCategorySummaries(): Flow<List<CategorySpendingUi>> {
        return MutableStateFlow(sampleCategories())
    }
}

// --- ViewModel ---
class DashboardViewModel(
    private val repository: CategorySpendingRepository = MockCategoryRepository(),
    userInitials: String = "C"
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(userInitials = userInitials, dateLabel = todayLabel())
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCategorySummaries().collectLatest { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun todayLabel(): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM")
        return java.time.LocalDate.now().format(formatter)
    }
}

// --- Screen ---
@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    onLogExpense: (categoryId: String) -> Unit,
    onAddNote: (categoryId: String) -> Unit,
    onAttachReceipt: (categoryId: String, prefill: com.chirag.arthix.ui.screen.manual.ManualEntryPrefill) -> Unit,
    onViewAllCategories: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onShareReport: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val amount = data?.getStringExtra(com.chirag.arthix.ocr.ReceiptCaptureActivity.EXTRA_PREFILL_AMOUNT)
            val payee = data?.getStringExtra(com.chirag.arthix.ocr.ReceiptCaptureActivity.EXTRA_PREFILL_PAYEE)
            onAttachReceipt("", com.chirag.arthix.ui.screen.manual.ManualEntryPrefill(amount = amount, payee = payee))
        }
    }

    DashboardScreen(
        state = state,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onLogExpense = onLogExpense,
        onAddNote = onAddNote,
        onAttachReceipt = { 
            cameraLauncher.launch(com.chirag.arthix.ocr.ReceiptCaptureActivity.createIntent(context))
        },
        onViewAllCategories = onViewAllCategories,
        onOpenCalendar = {
            android.widget.Toast.makeText(context, "Calendar coming soon!", android.widget.Toast.LENGTH_SHORT).show()
            onOpenCalendar()
        },
        onOpenSettings = onOpenSettings,
        onShareReport = onShareReport
    )
}

@Composable
fun DashboardScreen(
    state: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onLogExpense: (categoryId: String) -> Unit,
    onAddNote: (categoryId: String) -> Unit,
    onAttachReceipt: (categoryId: String) -> Unit,
    onViewAllCategories: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onShareReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Note: We removed Scaffold bottomBar here so ArthixApp handles the global bottom nav.
    // The screen uses its own dark background (DashBg).
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DashBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp), // Extra padding for bottom nav
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HomeHeader(
                    initials = state.userInitials,
                    dateLabel = state.dateLabel,
                    onCalendarClick = onOpenCalendar,
                    onSettingsClick = onOpenSettings,
                    onShareClick = onShareReport
                )
            }
            item {
                TransactionSearchBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchQueryChange
                )
            }
            item {
                CategorySectionHeader(
                    title = "Your categories (${state.categories.size})",
                    onViewAllClick = onViewAllCategories
                )
            }
            items(state.categories, key = { it.id }) { category ->
                CategorySpendingCard(
                    ui = category,
                    onLogExpense = { onLogExpense(category.id) },
                    onAddNote = { onAddNote(category.id) },
                    onAttachReceipt = { onAttachReceipt(category.id) }
                )
            }
        }
    }
}

// --- Components ---
@Composable
fun HomeHeader(
    initials: String,
    dateLabel: String,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(width = 2.dp, color = DashGold, shape = CircleShape)
                    .background(DashSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(text = initials, color = DashTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = "Today", color = DashTextMuted, fontSize = 11.sp)
                Text(text = dateLabel, color = DashTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleIconButton(icon = Icons.Filled.CalendarToday, contentDescription = "Spending calendar", onClick = onCalendarClick)
            CircleIconButton(icon = Icons.Filled.Settings, contentDescription = "Settings", onClick = onSettingsClick)
            CircleIconButton(icon = Icons.Filled.Share, contentDescription = "Share report", onClick = onShareClick)
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(DashSurfaceElevated)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = DashTextPrimary, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun TransactionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DashSurface)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = DashTextMuted, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            if (query.isEmpty()) {
                Text(text = "Search transactions or categories", color = DashTextMuted, fontSize = 13.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = DashTextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(DashGold),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CategorySectionHeader(
    title: String,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = DashTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(DashGold)
                .clickable(onClick = onViewAllClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "View all", color = DashOnGold, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(2.dp))
            Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = DashOnGold, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun CategorySpendingCard(
    ui: CategorySpendingUi,
    onLogExpense: () -> Unit,
    onAddNote: () -> Unit,
    onAttachReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNoteInput by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DashSurface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DashSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ui.icon,
                    contentDescription = null,
                    tint = DashGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = ui.name,
                    color = DashTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = ui.goalLabel,
                    color = DashTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBlock(
                value = "${ui.daysLogged} days",
                label = "Logged",
                modifier = Modifier.weight(1f)
            )
            StatBlock(
                value = "${ui.percentOfBudgetUsed}%",
                label = "of budget",
                modifier = Modifier.weight(1f)
            )
            StatBlock(
                value = "\u20b9${ui.amountSpentRupees}",
                label = "Spent",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DashSurfaceElevated)
                .padding(16.dp)
        ) {
            SpendingHeatmapGrid(heatmap = ui.heatmap, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. Modular Action / Note Widgets ---
        Spacer(modifier = Modifier.height(12.dp))

        if (showNoteInput) {
            // Expanded Note Editor Widget (Dark)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DashSurfaceElevated)
                    .padding(16.dp)
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    decorationBox = { innerTextField ->
                        if (noteText.isEmpty()) {
                            Text(
                                "Discuss launch timeline and assign...",
                                color = DashTextMuted,
                                fontSize = 14.sp,
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(DashDivider)
                            .clickable { 
                                showNoteInput = false 
                                noteText = ""
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = DashTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DashSurface)
                            .clickable {
                                showNoteInput = false
                                noteText = ""
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save Note", color = DashTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF9500)) // iOS orange/yellow
                            .clickable {
                                showNoteInput = false
                                noteText = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Save",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // "Meeting notes" style collapsed widget
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DashSurfaceElevated)
                    .clickable { showNoteInput = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Yellow Notepad Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFD60A)), // Yellow
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EditNote,
                        contentDescription = null,
                        tint = DashSurfaceElevated,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Meeting notes",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Discuss launch timeline and assign...",
                        color = DashTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Edit button (circular grey)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DashDivider)
                        .clickable { showNoteInput = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Note",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Actions Row (Log / Receipt)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Log Expense block
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF007AFF)) // iOS Blue
                    .clickable(onClick = onLogExpense)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log expense", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            // Receipt block
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DashSurfaceElevated)
                    .clickable(onClick = onAttachReceipt)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DashDivider),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = DashTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attach receipt", color = DashTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DashSurfaceElevated)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, color = DashTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = DashTextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun ActionPill(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SpendingHeatmapGrid(
    heatmap: WeekHeatmap,
    modifier: Modifier = Modifier,
    cellSize: Dp = 13.dp,
    cellSpacing: Dp = 4.dp
) {
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
            dayLabels.forEach { label ->
                Box(
                    modifier = Modifier.size(cellSize),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = DashTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(cellSpacing)
        ) {
            heatmap.weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
                    week.forEach { intensity ->
                        HeatCell(intensity = intensity, size = cellSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatCell(intensity: Float?, size: Dp) {
    val fill = when {
        intensity == null || intensity <= 0f -> DashHeatEmpty
        intensity < 0.25f -> DashGold.copy(alpha = 0.25f)
        intensity < 0.50f -> DashGold.copy(alpha = 0.45f)
        intensity < 0.75f -> DashGold.copy(alpha = 0.70f)
        else -> DashGold
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
            .background(fill)
    )
}

// --- Preview Data ---
fun sampleCategories(seed: Long = 42L): List<CategorySpendingUi> {
    val random = Random(seed)
    return listOf(
        CategorySpendingUi(
            id = "food",
            name = "Food and dining",
            icon = Icons.Filled.Fastfood,
            goalLabel = "Budget: \u20b94,000 | at most 3 outings a week",
            daysLogged = 4,
            percentOfBudgetUsed = 15,
            amountSpentRupees = 450,
            heatmap = randomHeatmap(random)
        ),
        CategorySpendingUi(
            id = "subscriptions",
            name = "Subscriptions",
            icon = Icons.Filled.Subscriptions,
            goalLabel = "Budget: \u20b9800 | 5 active plans",
            daysLogged = 2,
            percentOfBudgetUsed = 62,
            amountSpentRupees = 499,
            heatmap = randomHeatmap(random)
        )
    )
}

private fun randomHeatmap(random: Random, weekCount: Int = 8): WeekHeatmap {
    val weeks = List(weekCount) {
        List(7) {
            if (random.nextFloat() < 0.55f) null else random.nextFloat()
        }
    }
    return WeekHeatmap(weeks)
}
