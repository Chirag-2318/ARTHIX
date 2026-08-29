package com.chirag.arthix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Label

/**
 * Category chip row — horizontal icon-in-circle + label row (PRD §5).
 *
 * Mirrors Uber's "For you" Auto/Trip/Metro/Bus row:
 * icon inside a filled circle, label below, selected = white border.
 *
 * Used by: FR-1 chip UI, FR-4 OCR pre-fill, edit screen category picker.
 */

data class CategoryItem(
    val name: String,
    val icon: ImageVector,
    val tint: Color,
)

val DEFAULT_CATEGORIES = listOf(
    CategoryItem("Food", Icons.Outlined.Fastfood, Color(0xFFFF8A65)),
    CategoryItem("Travel", Icons.Outlined.Flight, Color(0xFF4FC3F7)),
    CategoryItem("Shopping", Icons.Outlined.ShoppingBag, Color(0xFFBA68C8)),
    CategoryItem("Other", Icons.Outlined.MoreHoriz, Color(0xFF90A4AE)),
)

@Composable
fun CategoryChipRow(
    categories: List<CategoryItem> = DEFAULT_CATEGORIES,
    selectedCategory: String? = null,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(categories) { category ->
            val isSelected = selectedCategory == category.name
            CategoryChipItem(
                category = category,
                isSelected = isSelected,
                onClick = { onCategorySelected(category.name) },
            )
        }
    }
}

@Composable
private fun CategoryChipItem(
    category: CategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = ArthixTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        // Icon circle — matches Uber's icon-in-filled-circle pattern
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isSelected) colors.chipBgSelected else colors.chipBg)
                .then(
                    if (isSelected) Modifier.border(2.dp, colors.accent, CircleShape)
                    else Modifier
                ),
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.name,
                tint = if (isSelected) colors.chipTextSelected else category.tint,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = category.name,
            style = Label,
            color = if (isSelected) colors.textPrimary else colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
