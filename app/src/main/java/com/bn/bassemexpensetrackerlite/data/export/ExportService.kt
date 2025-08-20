package com.bn.bassemexpensetrackerlite.data.export

import android.content.Context
import android.net.Uri
import com.bn.bassemexpensetrackerlite.domain.model.Expense
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

interface ExportService {
    suspend fun exportToCsv(expenses: List<Expense>): String
    suspend fun exportToPdf(expenses: List<Expense>): ByteArray
}

class ExportServiceImpl : ExportService {
    
    override suspend fun exportToCsv(expenses: List<Expense>): String {
        val output = ByteArrayOutputStream()
        val writer = OutputStreamWriter(output)
        val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT
            .withHeader("Date", "Category", "Type", "Amount", "Currency", "USD Amount", "Receipt"))
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        
        expenses.forEach { expense ->
            csvPrinter.printRecord(
                dateFormat.format(Date(expense.dateEpochMillis)),
                expense.category,
                if (expense.isIncome) "Income" else "Expense",
                String.format(Locale.US, "%.2f", expense.amountOriginal),
                expense.currencyCode,
                String.format(Locale.US, "%.2f", expense.amountUsd),
                expense.receiptUri ?: "No receipt"
            )
        }
        
        csvPrinter.flush()
        csvPrinter.close()
        
        return output.toString()
    }
    
    override suspend fun exportToPdf(expenses: List<Expense>): ByteArray {
        val output = ByteArrayOutputStream()
        val writer = PdfWriter(output)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)
        
        // Add title
        val title = Paragraph("Expense Report")
            .setFontSize(20f)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(title)
        
        // Add summary
        val totalIncome = expenses.filter { it.isIncome }.sumOf { it.amountUsd }
        val totalExpenses = expenses.filter { !it.isIncome }.sumOf { it.amountUsd }
        val balance = totalIncome - totalExpenses
        
        val summary = Paragraph("""
            Summary:
            Total Income: $${String.format(Locale.US, "%.2f", totalIncome)}
            Total Expenses: $${String.format(Locale.US, "%.2f", totalExpenses)}
            Balance: $${String.format(Locale.US, "%.2f", balance)}
        """.trimIndent())
            .setFontSize(12f)
        document.add(summary)
        
        // Add table
        val table = Table(7)
        table.addCell("Date")
        table.addCell("Category")
        table.addCell("Type")
        table.addCell("Amount")
        table.addCell("Currency")
        table.addCell("USD Amount")
        table.addCell("Receipt")
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        expenses.forEach { expense ->
            table.addCell(dateFormat.format(Date(expense.dateEpochMillis)))
            table.addCell(expense.category)
            table.addCell(if (expense.isIncome) "Income" else "Expense")
            table.addCell(String.format(Locale.US, "%.2f", expense.amountOriginal))
            table.addCell(expense.currencyCode)
            table.addCell(String.format(Locale.US, "%.2f", expense.amountUsd))
            table.addCell(expense.receiptUri ?: "No receipt")
        }
        
        document.add(table)
        document.close()
        
        return output.toByteArray()
    }
}
