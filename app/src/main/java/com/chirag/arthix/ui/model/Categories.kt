package com.chirag.arthix.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class Category(
    val label: String,
    val icon: ImageVector,
    val glyph: String // used on the overlapping avatar badge
)

val expenseCategories = listOf(
    Category("Food", Icons.Filled.Restaurant, "🍔"),
    Category("Travel", Icons.Filled.Flight, "✈️"),
    Category("Shopping", Icons.Filled.ShoppingBag, "🛍️"),
    Category("Bills", Icons.Filled.ReceiptLong, "🧾"),
    Category("Groceries", Icons.Filled.ShoppingCart, "🛒"),
)

val incomeCategories = listOf(
    Category("Salary", Icons.Filled.Payments, "💼"),
    Category("Refund", Icons.Filled.Replay, "↩️"),
    Category("Gift", Icons.Filled.CardGiftcard, "🎁"),
    Category("Interest", Icons.Filled.TrendingUp, "📈"),
    Category("Other", Icons.Filled.MoreHoriz, "➕"),
)
