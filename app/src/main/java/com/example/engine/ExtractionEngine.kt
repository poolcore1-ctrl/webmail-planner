package com.example.engine

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

data class ExtractedItem(
    val title: String,
    val description: String,
    val type: String, // MEETING, TASK, DEADLINE, FOLLOW_UP, REMINDER
    val date: String, // YYYY-MM-DD
    val time: String? = null, // HH:mm
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val projectLabel: String? = null,
    val assignedPerson: String? = null,
    val meetingLink: String? = null,
    val attachmentStatus: String = "Not Detected" // Attachment Present, Attachment Mentioned, Not Detected
)

data class ExtractionResult(
    val items: List<ExtractedItem>,
    val executiveSummary: String,
    val actionSummary: String,
    val riskLevel: String, // HIGH, MEDIUM, LOW
    val importantDates: List<String>
)

object ExtractionEngine {

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Parses raw text into structured extracted results
    fun extract(text: String): ExtractionResult {
        if (text.isBlank()) {
            return ExtractionResult(
                items = emptyList(),
                executiveSummary = "No content was shared to process.",
                actionSummary = "Please share a valid email text snippet to extract actions.",
                riskLevel = "LOW",
                importantDates = emptyList()
            )
        }

        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val items = mutableListOf<ExtractedItem>()
        val importantDates = mutableSetOf<String>()

        // 1. Detect Projects
        val projectLabel = detectProject(text)

        // 2. Detect People
        val assignedPerson = detectPerson(text)

        // 3. Detect Attachment Indicators
        val attachmentStatus = detectAttachmentStatus(text)

        // 4. Base reference date: default to today (e.g. 2026-06-24)
        val todayStr = DATE_FORMAT.format(Date())
        val defaultDate = todayStr

        // 5. Line-by-line parsing for actions
        for (line in lines) {
            val lowerLine = line.lowercase()

            // Detect dates in this line
            val dateFound = parseDateFromText(line) ?: defaultDate
            importantDates.add(dateFound)

            // Detect time
            val timeFound = parseTimeFromText(line)

            // Category detection
            when {
                // MEETINGS
                lowerLine.contains("meet") || lowerLine.contains("call") || 
                lowerLine.contains("sync") || lowerLine.contains("discuss") || 
                lowerLine.contains("zoom") || lowerLine.contains("teams") -> {
                    
                    val link = detectMeetingLink(line)
                    val priority = if (lowerLine.contains("urgent") || lowerLine.contains("asap") || lowerLine.contains("important")) "HIGH" else "MEDIUM"
                    
                    items.add(
                        ExtractedItem(
                            title = truncateText(line, 40),
                            description = line,
                            type = "MEETING",
                            date = dateFound,
                            time = timeFound,
                            priority = priority,
                            projectLabel = projectLabel,
                            assignedPerson = assignedPerson,
                            meetingLink = link,
                            attachmentStatus = attachmentStatus
                        )
                    )
                }
                
                // DEADLINES
                lowerLine.contains("due") || lowerLine.contains("deadline") || 
                lowerLine.contains("by") && lowerLine.contains("complete") || 
                lowerLine.contains("before") -> {
                    
                    items.add(
                        ExtractedItem(
                            title = "Deadline: " + truncateText(line, 30),
                            description = line,
                            type = "DEADLINE",
                            date = dateFound,
                            time = timeFound,
                            priority = "HIGH",
                            projectLabel = projectLabel,
                            assignedPerson = assignedPerson,
                            attachmentStatus = attachmentStatus
                        )
                    )
                }

                // FOLLOW UPS
                lowerLine.contains("follow up") || lowerLine.contains("follow-up") || 
                lowerLine.contains("check in") || lowerLine.contains("check-in") || 
                lowerLine.contains("update on") -> {
                    
                    items.add(
                        ExtractedItem(
                            title = "Follow up: " + truncateText(line, 30),
                            description = line,
                            type = "FOLLOW_UP",
                            date = dateFound,
                            time = timeFound,
                            priority = "MEDIUM",
                            projectLabel = projectLabel,
                            assignedPerson = assignedPerson,
                            attachmentStatus = attachmentStatus
                        )
                    )
                }

                // TASKS / WORK
                lowerLine.contains("todo") || lowerLine.contains("to do") || 
                lowerLine.contains("please") || lowerLine.contains("need to") || 
                lowerLine.contains("action") || lowerLine.contains("task") || 
                lowerLine.contains("should") -> {
                    
                    val isHigh = lowerLine.contains("urgent") || lowerLine.contains("asap") || lowerLine.contains("must")
                    items.add(
                        ExtractedItem(
                            title = truncateText(line, 40),
                            description = line,
                            type = "TASK",
                            date = dateFound,
                            time = timeFound,
                            priority = if (isHigh) "HIGH" else "MEDIUM",
                            projectLabel = projectLabel,
                            assignedPerson = assignedPerson,
                            attachmentStatus = attachmentStatus
                        )
                    )
                }
            }
        }

        // Default if empty
        if (items.isEmpty()) {
            items.add(
                ExtractedItem(
                    title = "Review shared notes",
                    description = truncateText(text, 150),
                    type = "TASK",
                    date = defaultDate,
                    priority = "LOW",
                    projectLabel = projectLabel,
                    assignedPerson = assignedPerson,
                    attachmentStatus = attachmentStatus
                )
            )
            importantDates.add(defaultDate)
        }

        // Calculate general risk level
        val hasHighPriority = items.any { it.priority == "HIGH" }
        val riskLevel = if (hasHighPriority) "HIGH" else if (items.size > 3) "MEDIUM" else "LOW"

        // Create elegant executive & action summaries
        val execSummary = "Processed shared email text containing ${items.size} detected work items. " +
                "Detected project label: ${projectLabel ?: "General/None"}. " +
                "Identified personnel involved: ${assignedPerson ?: "None specified"}. " +
                "Attachment context: $attachmentStatus."

        val actionSummary = "Please review, update, and approve the mapped meetings, follow-ups, and to-do checklist mapped in your timeline."

        return ExtractionResult(
            items = items,
            executiveSummary = execSummary,
            actionSummary = actionSummary,
            riskLevel = riskLevel,
            importantDates = importantDates.toList().sorted()
        )
    }

    private fun detectProject(text: String): String? {
        val hashTagPattern = Pattern.compile("#([A-Za-z0-9_-]+)")
        val tagMatcher = hashTagPattern.matcher(text)
        if (tagMatcher.find()) {
            return tagMatcher.group(1)
        }

        val projectPattern = Pattern.compile("(?i)(?:project|proj)\\s+([A-Za-z0-9_-]+)")
        val projMatcher = projectPattern.matcher(text)
        if (projMatcher.find()) {
            return projMatcher.group(1)
        }
        return null
    }

    private fun detectPerson(text: String): String? {
        val withPattern = Pattern.compile("(?i)(?:with|assign to|by|from|sender:)\\s+([A-Z][a-z]+)")
        val matcher = withPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    private fun detectAttachmentStatus(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("attached") || lower.contains("find the attachment") -> "Attachment Present"
            lower.contains("attachment") || lower.contains("pdf") || lower.contains("docx") || lower.contains("xlsx") || lower.contains("csv") -> "Attachment Mentioned"
            else -> "Not Detected"
        }
    }

    private fun detectMeetingLink(line: String): String? {
        val urlPattern = Pattern.compile("https?://\\S*(?:zoom|teams|meet|webex)\\S*", Pattern.CASE_INSENSITIVE)
        val matcher = urlPattern.matcher(line)
        if (matcher.find()) {
            return matcher.group(0)
        }
        return null
    }

    private fun parseDateFromText(text: String): String? {
        // Match standard ISO date: YYYY-MM-DD
        val isoPattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})")
        val isoMatcher = isoPattern.matcher(text)
        if (isoMatcher.find()) {
            return isoMatcher.group(1) + "-" + isoMatcher.group(2) + "-" + isoMatcher.group(3)
        }

        // Match US Date: MM/DD/YYYY or M/D/YYYY or MM-DD-YYYY
        val usPattern = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})")
        val usMatcher = usPattern.matcher(text)
        if (usMatcher.find()) {
            val month = usMatcher.group(1).toInt()
            val day = usMatcher.group(2).toInt()
            val year = usMatcher.group(3)
            return String.format("%s-%02d-%02d", year, month, day)
        }

        // Relative text matches
        val lower = text.lowercase()
        val calendar = Calendar.getInstance()
        if (lower.contains("today")) {
            return DATE_FORMAT.format(calendar.time)
        }
        if (lower.contains("tomorrow")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            return DATE_FORMAT.format(calendar.time)
        }
        if (lower.contains("next week")) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            return DATE_FORMAT.format(calendar.time)
        }

        // Return null if no matches, letting caller use default
        return null
    }

    private fun parseTimeFromText(text: String): String? {
        // Matches HH:MM or H:MM (am/pm optional)
        val timePattern = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(?i)(am|pm)?")
        val matcher = timePattern.matcher(text)
        if (matcher.find()) {
            var hour = matcher.group(1).toInt()
            val minute = matcher.group(2).toInt()
            val amPm = matcher.group(3)
            if (amPm != null) {
                if (amPm.lowercase() == "pm" && hour < 12) hour += 12
                if (amPm.lowercase() == "am" && hour == 12) hour = 0
            }
            return String.format("%02d:%02d", hour, minute)
        }
        return null
    }

    private fun truncateText(text: String, length: Int): String {
        val cleaned = text.replace(Regex("\\s+"), " ").trim()
        return if (cleaned.length > length) cleaned.substring(0, length - 3) + "..." else cleaned
    }
}
