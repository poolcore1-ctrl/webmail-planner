package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.UserDayAttachment
import com.example.data.UserNote
import com.example.data.WorkItem
import com.example.security.CryptoManager
import java.io.OutputStream

object PdfGenerator {

    fun generateTimelinePdf(
        context: Context,
        titleLabel: String,
        items: List<WorkItem>,
        notes: List<UserNote>,
        attachments: List<UserDayAttachment>,
        nodeCount: Int,
        edgeCount: Int,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()

        // Page size: A4 (595 x 842 points)
        val pageWidth = 595
        val pageHeight = 842

        // --- PAGE 1: COVER PAGE ---
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint()
        
        // Draw deep slate background accent
        paint.color = Color.parseColor("#121824")
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 150f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("WORKMAIL PLANNER", 40f, 75f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("LOCAL & OFFLINE WORK TIMELINE REPORT", 40f, 105f, paint)

        // Content Area Text
        paint.color = Color.parseColor("#222222")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Timeline Summary", 40f, 200f, paint)

        // Metadata block
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var yPos = 240f
        canvas.drawText("Report Range: $titleLabel", 40f, yPos, paint)
        yPos += 24f
        canvas.drawText("Total Timeline Items: ${items.size}", 40f, yPos, paint)
        yPos += 24f
        canvas.drawText("Daily Hand-written Notes: ${notes.size}", 40f, yPos, paint)
        yPos += 24f
        canvas.drawText("Day-level Local Files Linked: ${attachments.size}", 40f, yPos, paint)
        yPos += 24f
        canvas.drawText("Knowledge Graph Nodes / Connections: $nodeCount / $edgeCount", 40f, yPos, paint)

        // Box outlining strict corporate privacy declaration
        paint.color = Color.parseColor("#F5F7FA")
        canvas.drawRect(40f, yPos + 20f, (pageWidth - 40).toFloat(), yPos + 180f, paint)

        paint.color = Color.parseColor("#E15241")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("ORGANIZATIONAL PRIVACY COMPLIANCE NOTICE", 55f, yPos + 50f, paint)

        paint.color = Color.parseColor("#445566")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        canvas.drawText("• This document contains only metadata locally structured on this device.", 55f, yPos + 75f, paint)
        canvas.drawText("• Absolute Privacy Rules: No email body or direct Outlook attachments are stored.", 55f, yPos + 95f, paint)
        canvas.drawText("• Exported offline from secure WorkMail Planner. Check company policy before sharing.", 55f, yPos + 115f, paint)
        canvas.drawText("• Compiled safely without using any cloud-based AI models or external telemetry.", 55f, yPos + 135f, paint)

        // Footer at bottom
        paint.color = Color.parseColor("#888888")
        paint.textSize = 9f
        canvas.drawText("Privacy Boundary: Local Secure Sandbox - No Network Transmissions", 40f, 800f, paint)

        pdfDocument.finishPage(page)

        // --- PAGE 2: TIMELINE DETAILS ---
        if (items.isNotEmpty()) {
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas

            // Page Header
            paint.color = Color.parseColor("#121824")
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 60f, paint)

            paint.color = Color.WHITE
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TIMELINE AGENDA - DETAILED VIEW", 40f, 38f, paint)

            // Draw items list
            paint.color = Color.BLACK
            yPos = 100f
            paint.textSize = 11f

            val limit = 20 // limit to fit beautiful single details page safely
            val subItems = items.take(limit)

            for (item in subItems) {
                // Background card box
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(30f, yPos, (pageWidth - 30).toFloat(), yPos + 32f, paint)

                paint.color = Color.parseColor("#1F2937")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val itemTitle = com.example.security.CryptoManager.decrypt(item.title)
                canvas.drawText("${item.date} | [${item.type}] $itemTitle", 40f, yPos + 18f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.parseColor("#4B5563")
                val itemTime = item.time ?: "Anytime"
                val priority = item.priority
                canvas.drawText("$itemTime | Priority: $priority | Label: ${item.projectLabel ?: "None"}", (pageWidth - 250).toFloat(), yPos + 18f, paint)

                yPos += 38f
            }

            if (items.size > limit) {
                paint.color = Color.parseColor("#999999")
                paint.textSize = 10f
                canvas.drawText("... and ${items.size - limit} more items (omitted to maintain safe visual layout density) ...", 40f, yPos, paint)
            }

            // Footer
            paint.color = Color.parseColor("#888888")
            paint.textSize = 9f
            canvas.drawText("Offline-First Timeline Security Shield • Generated locally via WorkMail Planner", 40f, 800f, paint)

            pdfDocument.finishPage(page)
        }

        // Save PDF to output stream
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}
