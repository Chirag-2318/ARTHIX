package com.chirag.arthix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.chirag.arthix.ui.navigation.ArthixRoute
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.LabelCaps

/**
 * Arthix 4-tab bottom navigation bar matching the Stitch design.
 *
 * Tabs: Home, Activity, Insights, Account
 * Active state uses secondary-container pill indicator around the icon.
 * Label uses label-caps typography.
 * Surface: surface-dim background with border-hairline top border.
 */

data class BottomNavItem(
    val route: ArthixRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(ArthixRoute.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(ArthixRoute.Activity, "Activity", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    BottomNavItem(ArthixRoute.Split, "Split", Icons.Filled.CallSplit, Icons.Outlined.CallSplit),
    BottomNavItem(ArthixRoute.Insights, "Insights", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    BottomNavItem(ArthixRoute.Account, "Account", Icons.Filled.Person, Icons.Outlined.Person),
)

@Composable
fun ArthixBottomNavBar(
    currentRoute: String?,
    onNavigate: (ArthixRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ArthixTheme.colors

    NavigationBar(
        containerColor = Color(0xFF0B0B0D),
        tonalElevation = 0.dp,
        modifier = modifier.height(72.dp),
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route.route

            NavigationBarItem(
                icon = {
                    if (selected) {
                        Icon(
                            imageVector = item.selectedIcon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp),
                            tint = colors.textPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = item.unselectedIcon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = LabelCaps,
                        color = if (selected) colors.textPrimary else colors.onSecondaryContainer,
                    )
                },
                selected = selected,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.textPrimary,
                    selectedTextColor = colors.textPrimary,
                    unselectedIconColor = colors.onSecondaryContainer,
                    unselectedTextColor = colors.onSecondaryContainer,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
