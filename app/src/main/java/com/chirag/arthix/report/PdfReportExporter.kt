package com.chirag.arthix.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.chirag.arthix.report.model.ComputedReportData
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object PdfReportExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun exportToPdf(context: Context, data: ComputedReportData, suggestions: List<String>): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawReport(canvas, data, suggestions)

        document.finishPage(page)

        val cacheDir = File(context.cacheDir, "reports")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val file = File(cacheDir, "Arthix_Report_${data.period.label.replace(" ", "_")}.pdf")
        
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        document.close()
        fos.close()

        return file
    }

    private fun drawReport(canvas: Canvas, data: ComputedReportData, suggestions: List<String>) {
        val paint = Paint().apply { isAntiAlias = true }
        
        // Background
        canvas.drawColor(Color.parseColor("#FAF7F2"))

        var currentY = 50f
        val marginX = 40f
        val contentWidth = PAGE_WIDTH - (2 * marginX)

        // Title
        paint.color = Color.parseColor("#1A1A1A")
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Arthix Financial Report", marginX, currentY, paint)
        currentY += 30f

        // Period Label
        paint.color = Color.parseColor("#6E6E73")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(data.period.label, marginX, currentY, paint)
        currentY += 40f

        // Hero Cards (Income vs Spend vs Net)
        val cardWidth = (contentWidth - 40f) / 3
        drawSummaryCard(canvas, marginX, currentY, cardWidth, "Income", formatPaise(data.totalInflowPaise), "#8BA888")
        drawSummaryCard(canvas, marginX + cardWidth + 20f, currentY, cardWidth, "Spent", formatPaise(data.totalOutflowPaise), "#E4463A")
        val netColor = if (data.netFlowPaise >= 0) "#8BA888" else "#1A1A1A"
        drawSummaryCard(canvas, marginX + 2 * (cardWidth + 20f), currentY, cardWidth, "Net", formatPaise(data.netFlowPaise), netColor)
        
        currentY += 100f

        // Trending
        if (data.trendingCategories.isNotEmpty()) {
            paint.color = Color.parseColor("#1A1A1A")
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Trending Categories", marginX, currentY, paint)
            currentY += 30f
            
            for (trend in data.trendingCategories) {
                val isUp = trend.amountChangedPaise > 0
                val color = if (isUp) "#E4463A" else "#8BA888"
                val sign = if (isUp) "+" else "-"
                
                paint.color = Color.parseColor("#1A1A1A")
                paint.textSize = 16f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(trend.category.replaceFirstChar { it.uppercase() }, marginX, currentY, paint)
                
                paint.color = Color.parseColor(color)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val trendText = "$sign${kotlin.math.abs(trend.percentageChange)}% (${formatPaise(kotlin.math.abs(trend.amountChangedPaise))})"
                canvas.drawText(trendText, PAGE_WIDTH - marginX - paint.measureText(trendText), currentY, paint)
                currentY += 25f
            }
            currentY += 20f
        }

        // Category Breakdown (Bar Chart)
        if (data.categoryBreakdown.isNotEmpty()) {
            paint.color = Color.parseColor("#1A1A1A")
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Category Breakdown", marginX, currentY, paint)
            currentY += 40f
            
            val maxSpend = data.categoryBreakdown.values.maxOrNull() ?: 1L
            val chartWidth = contentWidth - 150f
            
            data.categoryBreakdown.entries.sortedByDescending { it.value }.take(5).forEach { (cat, amount) ->
                // Label
                paint.color = Color.parseColor("#1A1A1A")
                paint.textSize = 14f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(cat.replaceFirstChar { it.uppercase() }, marginX, currentY, paint)
                
                // Bar
                val barWidth = (amount.toFloat() / maxSpend) * chartWidth
                val rect = RectF(marginX + 100f, currentY - 12f, marginX + 100f + barWidth, currentY + 4f)
                paint.color = Color.parseColor("#E4463A")
                canvas.drawRoundRect(rect, 8f, 8f, paint)
                
                // Amount
                paint.color = Color.parseColor("#6E6E73")
                canvas.drawText(formatPaise(amount), marginX + 100f + barWidth + 10f, currentY, paint)
                
                currentY += 30f
            }
            currentY += 20f
        }

        // Suggestions
        if (suggestions.isNotEmpty()) {
            paint.color = Color.parseColor("#1A1A1A")
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Insights & Suggestions", marginX, currentY, paint)
            currentY += 30f
            
            paint.color = Color.parseColor("#6E6E73")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            
            for (suggestion in suggestions) {
                // simple wrapping roughly
                val words = suggestion.split(" ")
                var line = "• "
                for (word in words) {
                    if (paint.measureText(line + word) > contentWidth) {
                        canvas.drawText(line, marginX, currentY, paint)
                        currentY += 20f
                        line = "  $word "
                    } else {
                        line += "$word "
                    }
                }
                if (line.isNotBlank()) {
                    canvas.drawText(line, marginX, currentY, paint)
                    currentY += 25f
                }
            }
        }
    }

    private fun drawSummaryCard(canvas: Canvas, x: Float, y: Float, width: Float, title: String, amount: String, colorHex: String) {
        val paint = Paint().apply { isAntiAlias = true }
        
        // Card bg
        paint.color = Color.WHITE
        paint.setShadowLayer(4f, 0f, 2f, Color.parseColor("#20000000"))
        val rect = RectF(x, y, x + width, y + 80f)
        canvas.drawRoundRect(rect, 12f, 12f, paint)
        paint.clearShadowLayer()

        // Title
        paint.color = Color.parseColor("#6E6E73")
        paint.textSize = 12f
        canvas.drawText(title, x + 12f, y + 25f, paint)

        // Amount
        paint.color = Color.parseColor(colorHex)
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(amount, x + 12f, y + 60f, paint)
    }

    private fun formatPaise(paise: Long): String {
        val rupees = paise / 100
        return "₹$rupees"
    }
}
