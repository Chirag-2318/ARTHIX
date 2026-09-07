package com.chirag.arthix.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.chirag.arthix.data.entity.MoneyLogEntity
import com.chirag.arthix.data.model.MoneyLogDateMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MoneyLogPdfExporter {
    suspend fun exportToPdf(context: Context, entries: List<MoneyLogEntity>) = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size 595x842
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()
        var yPosition = 50f
        val xMargin = 50f

        val sortedEntries = entries.sortedBy { it.createdAt }

        // Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 24f
        canvas.drawText("Money Log Report", xMargin, yPosition, paint)
        yPosition += 30f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 14f
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
        canvas.drawText("Generated on: ${sdf.format(Date())}", xMargin, yPosition, paint)
        yPosition += 40f

        // Summary
        val total = entries.sumOf { it.amount }
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        canvas.drawText("Summary", xMargin, yPosition, paint)
        yPosition += 25f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 14f
        canvas.drawText("Total Amount: ₹${"%.2f".format(total)}", xMargin, yPosition, paint)
        yPosition += 25f

        val categoryCounts = entries.groupBy { it.category }.mapValues { it.value.size }
        val categoryTotals = entries.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }

        canvas.drawText("Category Breakdown:", xMargin, yPosition, paint)
        yPosition += 20f
        
        for ((cat, count) in categoryCounts) {
            val catTotal = categoryTotals[cat] ?: 0.0
            canvas.drawText("- ${cat.label}: ₹${"%.2f".format(catTotal)} ($count entries)", xMargin + 20f, yPosition, paint)
            yPosition += 20f
        }
        yPosition += 30f

        // Detail Table Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("#", xMargin, yPosition, paint)
        canvas.drawText("Date", xMargin + 30f, yPosition, paint)
        canvas.drawText("Category", xMargin + 150f, yPosition, paint)
        canvas.drawText("Description", xMargin + 280f, yPosition, paint)
        canvas.drawText("Amount", xMargin + 460f, yPosition, paint)
        yPosition += 15f
        canvas.drawLine(xMargin, yPosition - 5f, pageInfo.pageWidth - xMargin, yPosition - 5f, paint)
        yPosition += 15f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateSdf = SimpleDateFormat("dd MMM yyyy", Locale.US)

        for ((index, entry) in sortedEntries.withIndex()) {
            if (yPosition > pageInfo.pageHeight - 50f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val dateStr = when (entry.dateMode) {
                MoneyLogDateMode.EXACT -> entry.exactDateMillis?.let { dateSdf.format(Date(it)) } ?: "Unknown"
                MoneyLogDateMode.RANGE -> {
                    val s = entry.rangeStartMillis?.let { dateSdf.format(Date(it)) } ?: "?"
                    val e = entry.rangeEndMillis?.let { dateSdf.format(Date(it)) } ?: "?"
                    "$s - $e"
                }
                MoneyLogDateMode.APPROXIMATE -> entry.approximateMonthYear ?: "Approx(Unknown)"
                MoneyLogDateMode.UNKNOWN -> "Date not specified"
            }
            
            var catStr = entry.category.label
            if (entry.category.name == "OTHER" && entry.customCategoryLabel != null) {
                catStr = entry.customCategoryLabel
            }
            if (catStr.length > 15) catStr = catStr.take(12) + "..."

            canvas.drawText("${index + 1}", xMargin, yPosition, paint)
            canvas.drawText(dateStr, xMargin + 30f, yPosition, paint)
            canvas.drawText(catStr, xMargin + 150f, yPosition, paint)
            val desc = if (entry.description.length > 25) entry.description.take(22) + "..." else entry.description
            canvas.drawText(desc, xMargin + 280f, yPosition, paint)
            canvas.drawText("₹${"%.2f".format(entry.amount)}", xMargin + 460f, yPosition, paint)
            yPosition += 20f
        }

        document.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val file = File(reportsDir, "MoneyLogReport_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()

        withContext(Dispatchers.Main) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Open Money Log Report"))
        }
    }
}
