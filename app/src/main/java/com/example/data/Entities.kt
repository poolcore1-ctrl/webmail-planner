package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_items")
data class WorkItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val type: String, // MEETING, TASK, DEADLINE, FOLLOW_UP, REMINDER
    val date: String, // YYYY-MM-DD
    val time: String? = null, // HH:mm
    val priority: String, // HIGH, MEDIUM, LOW
    val status: String, // PENDING, COMPLETED
    val projectLabel: String? = null,
    val assignedPerson: String? = null,
    val meetingLink: String? = null,
    val attachmentStatus: String = "Not Detected", // Attachment Present, Attachment Mentioned, Not Detected, Unknown
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val archivedAt: Long? = null,
    val version: Int = 1,
    val sourceType: String = "USER", // USER, SHARED_MAIL
    val privacyStatus: String = "LOCAL_ONLY"
)

@Entity(tableName = "mail_references")
data class MailReference(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workItemId: Int,
    val subjectSnippet: String,
    val senderName: String,
    val dateString: String,
    val sourceLabel: String,
    val deepLink: String? = null
)

@Entity(tableName = "user_notes")
data class UserNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val content: String,
    val isChecklist: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val archivedAt: Long? = null,
    val version: Int = 1,
    val sourceType: String = "USER",
    val privacyStatus: String = "LOCAL_ONLY"
)

@Entity(tableName = "user_day_attachments")
data class UserDayAttachment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val fileUri: String,
    val fileName: String,
    val fileType: String, // IMAGE, PDF, DOCUMENT, SPREADSHEET, PRESENTATION, AUDIO, ARCHIVE, OTHER
    val fileSize: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val archivedAt: Long? = null,
    val version: Int = 1,
    val sourceType: String = "USER",
    val privacyStatus: String = "LOCAL_ONLY"
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workItemId: Int? = null,
    val title: String,
    val date: String, // YYYY-MM-DD
    val reminderTime: Long, // timestamp
    val isTriggered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val archivedAt: Long? = null,
    val version: Int = 1,
    val sourceType: String = "USER",
    val privacyStatus: String = "LOCAL_ONLY"
)

@Entity(tableName = "attachment_indicators")
data class AttachmentIndicator(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workItemId: Int,
    val status: String // Attachment Present, Attachment Mentioned, Not Detected, Unknown
)

@Entity(tableName = "calendar_references")
data class CalendarReference(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workItemId: Int,
    val androidEventId: Long? = null,
    val isSynced: Boolean = false
)

@Entity(tableName = "pdf_export_records")
data class PdfExportRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateRange: String,
    val filterCriteria: String,
    val exportedAt: Long = System.currentTimeMillis(),
    val filePath: String
)

@Entity(tableName = "graph_nodes")
data class GraphNode(
    @PrimaryKey val id: String, // format: "type:value" e.g., "person:Alice", "project:Launch"
    val label: String,
    val type: String // PERSON, PROJECT, DATE, MEETING, TASK, DEADLINE, FOLLOW_UP, NOTE, USER_FILE, OUTLOOK_REF, ATTACHMENT_IND, CALENDAR_EVENT
)

@Entity(tableName = "graph_edges")
data class GraphEdge(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fromNodeId: String,
    val toNodeId: String,
    val relationType: String // assigned_to, due_on, related_to, linked_to, follow_up_with, source_of
)

@Entity(tableName = "trash_items")
data class TrashItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalTable: String, // work_items, user_notes, user_day_attachments
    val originalId: Int,
    val title: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val restoreDataJson: String // Serialized entity state for reconstruction
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String, // CREATE, UPDATE, DELETE, RESTORE, EXPORT_PDF, ARCHIVE
    val entityType: String,
    val entityId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "version_histories")
data class VersionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityType: String, // work_items, user_notes
    val entityId: Int,
    val version: Int,
    val stateJson: String,
    val changedAt: Long = System.currentTimeMillis()
)
