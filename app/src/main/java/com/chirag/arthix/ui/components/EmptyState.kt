package com.chirag.arthix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chirag.arthix.ui.theme.ArthixTheme
import com.chirag.arthix.ui.theme.Body
import com.chirag.arthix.ui.theme.Title

/**
 * Empty state — icon + headline + subtext + CTA (PRD §5, §6.10).
 *
 * Matches Uber's "You have no upcoming trips" pattern.
 * Used on: empty transaction store, no-report-data, empty history.
 * Never shows a blank screen per PRD §3 principle 5.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    headline: String,
    subtext: String,
    actionButton: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = ArthixTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = headline,
            style = Title,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtext,
            style = Body,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionButton != null) {
            Spacer(Modifier.height(24.dp))
            actionButton()
        }
    }
}
