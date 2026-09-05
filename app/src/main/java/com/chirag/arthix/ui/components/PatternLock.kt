package com.chirag.arthix.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.chirag.arthix.ui.theme.ArthixTheme
import kotlin.math.abs

@Composable
fun PatternLock(
    modifier: Modifier = Modifier,
    onPatternComplete: (List<Int>) -> Unit
) {
    val colors = ArthixTheme.colors
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentDragPosition by remember { mutableStateOf<Offset?>(null) }

    // Colors
    val activeColor = colors.primary
    val defaultColor = colors.surfaceContainerHighest

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
            .aspectRatio(1f)
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val stepX = widthPx / 3f
        val stepY = heightPx / 3f
        val offsetX = stepX / 2f
        val offsetY = stepY / 2f

        val nodePositions = remember(widthPx, heightPx) {
            val positions = mutableListOf<Offset>()
            for (row in 0..2) {
                for (col in 0..2) {
                    positions.add(Offset(offsetX + col * stepX, offsetY + row * stepY))
                }
            }
            positions
        }

        val touchRadius = stepX * 0.45f

        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(nodePositions) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedNodes.clear()
                            currentDragPosition = offset

                            // Check if we started on a node
                            val nodeIndex = nodePositions.indexOfFirst { nodeOffset ->
                                (offset - nodeOffset).getDistance() < touchRadius
                            }
                            if (nodeIndex != -1) {
                                selectedNodes.add(nodeIndex)
                            }
                        },
                        onDrag = { change, _ ->
                            currentDragPosition = change.position

                            // Check if we dragged over a new node
                            val nodeIndex = nodePositions.indexOfFirst { nodeOffset ->
                                (change.position - nodeOffset).getDistance() < touchRadius
                            }
                            if (nodeIndex != -1 && !selectedNodes.contains(nodeIndex)) {
                                // Check for skipped middle nodes (e.g. dragging from 0 to 2 skips 1)
                                if (selectedNodes.isNotEmpty()) {
                                    val lastNode = selectedNodes.last()
                                    val midNode = getMiddleNode(lastNode, nodeIndex)
                                    if (midNode != -1 && !selectedNodes.contains(midNode)) {
                                        selectedNodes.add(midNode)
                                    }
                                }
                                selectedNodes.add(nodeIndex)
                            }
                        },
                        onDragEnd = {
                            currentDragPosition = null
                            if (selectedNodes.isNotEmpty()) {
                                onPatternComplete(selectedNodes.toList())
                            }
                        },
                        onDragCancel = {
                            currentDragPosition = null
                            selectedNodes.clear()
                        }
                    )
                }
        ) {
            // Draw lines between selected nodes
            if (selectedNodes.size > 1) {
                for (i in 0 until selectedNodes.size - 1) {
                    val start = nodePositions[selectedNodes[i]]
                    val end = nodePositions[selectedNodes[i + 1]]
                    drawLine(
                        color = activeColor,
                        start = start,
                        end = end,
                        strokeWidth = 10f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw line from last selected node to current finger position
            if (selectedNodes.isNotEmpty() && currentDragPosition != null) {
                val start = nodePositions[selectedNodes.last()]
                drawLine(
                    color = activeColor.copy(alpha = 0.5f),
                    start = start,
                    end = currentDragPosition!!,
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
            }

            // Draw all nodes
            nodePositions.forEachIndexed { index, position ->
                val isSelected = selectedNodes.contains(index)
                drawCircle(
                    color = if (isSelected) activeColor else defaultColor,
                    radius = if (isSelected) 22f else 16f,
                    center = position
                )

                // Inner dot for selected nodes
                if (isSelected) {
                    drawCircle(
                        color = Color.White,
                        radius = 8f,
                        center = position
                    )
                }
            }
        }
    }
}

// Helper to find the node skipped between two nodes (if any)
// 0 1 2
// 3 4 5
// 6 7 8
private fun getMiddleNode(start: Int, end: Int): Int {
    val startRow = start / 3
    val startCol = start % 3
    val endRow = end / 3
    val endCol = end % 3

    val diffRow = abs(startRow - endRow)
    val diffCol = abs(startCol - endCol)

    if ((diffRow == 0 || diffRow == 2) && (diffCol == 0 || diffCol == 2)) {
        val midRow = (startRow + endRow) / 2
        val midCol = (startCol + endCol) / 2
        return midRow * 3 + midCol
    }
    return -1
}
