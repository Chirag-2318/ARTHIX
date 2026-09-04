package com.chirag.arthix.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/** The three real navigation destinations. Voice and Plus are actions, not tabs. */
enum class ArthixDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    ACTIVITY("Activity", Icons.Filled.ListAlt),
    INSIGHTS("Insights", Icons.Filled.BarChart)
}

/** The three destinations reachable from the Plus radial menu. */
enum class PlusOption(val label: String, val icon: ImageVector) {
    ACCOUNT("Account", Icons.Filled.Person),
    STREAKS("Streaks", Icons.Filled.LocalFireDepartment),
    CAMERA("Camera", Icons.Filled.CameraAlt)
}

/** Internal state machine for the Plus menu — shared by tap AND press-hold-drag. */
internal enum class PlusMenuMode { CLOSED, TAP_OPEN, DRAG_OPEN }

val VoiceMicIcon: ImageVector = Icons.Filled.Mic
val PlusIcon: ImageVector = Icons.Filled.Add
