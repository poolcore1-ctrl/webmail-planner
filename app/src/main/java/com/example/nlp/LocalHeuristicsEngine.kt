package com.example.nlp

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ExtractedTask(
    val title: String,
    val description: String,
    val type: String, // MEETING, DEADLINE, TASK
    val mappedDate: String, // YYYY-MM-DD
    val priority: String // HIGH, MEDIUM, LOW
)

object LocalHeuristicsEngine {

    private val actionKeywords = listOf("review", "complete", "finish", "submit", "prepare", "send", "update", "schedule", "call")
    private val meetingKeywords = listOf("meeting", "sync", "catch up", "1:1", "standup", "discuss")
    private val urgentKeywords = listOf("urgent", "asap", "immediately", "blocker", "critical")
    private val deadlineKeywords = listOf("deadline", "due by", "eod", "end of day", "by tomorrow")

    fun extractFromSnippet(sender: String, subject: String, bodySnippet: String): ExtractedTask? {
        val fullText = "$subject $bodySnippet".lowercase(Locale.ROOT)
        
        var isActionable = false
        var type = "TASK"
        var priority = "MEDIUM"

        // Determine if actionable
        if (actionKeywords.any { fullText.contains(it) } || meetingKeywords.any { fullText.contains(it) } || deadlineKeywords.any { fullText.contains(it) }) {
            isActionable = true
        }

        if (!isActionable) return null // If it's just an FYI email, skip it

        // Determine Type
        if (meetingKeywords.any { fullText.contains(it) }) {
            type = "MEETING"
        } else if (deadlineKeywords.any { fullText.contains(it) }) {
            type = "DEADLINE"
        }

        // Determine Priority
        if (urgentKeywords.any { fullText.contains(it) }) {
            priority = "HIGH"
        } else if (type == "DEADLINE") {
            priority = "HIGH"
        }

        // Smart Date Parsing (Very Basic Heuristic)
        val mappedDate = parseRelativeDate(fullText)

        return ExtractedTask(
            title = "[\${type}] $subject",
            description = "From: $sender\nSnippet: $bodySnippet",
            type = type,
            mappedDate = mappedDate,
            priority = priority
        )
    }

    private fun parseRelativeDate(text: String): String {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        if (text.contains("tomorrow")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        } else if (text.contains("next week")) {
            cal.add(Calendar.DAY_OF_YEAR, 7)
        } else if (text.contains("friday")) {
            val currentDay = cal.get(Calendar.DAY_OF_WEEK)
            val targetDay = Calendar.FRIDAY
            var diff = targetDay - currentDay
            if (diff <= 0) diff += 7
            cal.add(Calendar.DAY_OF_YEAR, diff)
        } else if (text.contains("monday")) {
            val currentDay = cal.get(Calendar.DAY_OF_WEEK)
            val targetDay = Calendar.MONDAY
            var diff = targetDay - currentDay
            if (diff <= 0) diff += 7
            cal.add(Calendar.DAY_OF_YEAR, diff)
        }
        
        return sdf.format(cal.time)
    }
}
