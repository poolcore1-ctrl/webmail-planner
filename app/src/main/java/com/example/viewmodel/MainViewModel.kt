package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.ExtractionEngine
import com.example.engine.ExtractionResult
import com.example.pdf.PdfGenerator
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DatabaseRepository
    private val db: AppDatabase

    init {
        db = AppDatabase.getDatabase(application)
        repository = DatabaseRepository(db.plannerDao())
    }

    // --- TIMELINE SELECTION STATE ---
    private val _selectedDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    // --- LIVE RECONCILED DATA STREAMS ---
    val activeWorkItems: StateFlow<List<WorkItem>> = repository.activeWorkItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedWorkItems: StateFlow<List<WorkItem>> = repository.archivedWorkItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<UserNote>> = repository.activeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAttachments: StateFlow<List<UserDayAttachment>> = repository.activeAttachments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeReminders: StateFlow<List<Reminder>> = repository.activeReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashItems: StateFlow<List<TrashItem>> = repository.trashItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val graphNodes: StateFlow<List<GraphNode>> = repository.graphNodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val graphEdges: StateFlow<List<GraphEdge>> = repository.graphEdges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SAFE INTAKE STATE BUFFER ---
    private val _intakeBufferText = MutableStateFlow("")
    val intakeBufferText: StateFlow<String> = _intakeBufferText.asStateFlow()

    private val _extractionResult = MutableStateFlow<ExtractionResult?>(null)
    val extractionResult: StateFlow<ExtractionResult?> = _extractionResult.asStateFlow()

    fun loadSharedText(text: String) {
        _intakeBufferText.value = text
        _extractionResult.value = ExtractionEngine.extract(text)
    }

    fun clearIntakeBuffer() {
        _intakeBufferText.value = ""
        _extractionResult.value = null
    }

    // Approve & Save Selected Mapped Items
    fun approveExtraction(selectedIndices: List<Int>) {
        val result = _extractionResult.value ?: return
        viewModelScope.launch {
            result.items.forEachIndexed { index, extracted ->
                if (selectedIndices.contains(index)) {
                    val workItem = WorkItem(
                        title = extracted.title,
                        description = extracted.description,
                        type = extracted.type,
                        date = extracted.date,
                        time = extracted.time,
                        priority = extracted.priority,
                        status = "PENDING",
                        projectLabel = extracted.projectLabel,
                        assignedPerson = extracted.assignedPerson,
                        meetingLink = extracted.meetingLink,
                        attachmentStatus = extracted.attachmentStatus,
                        sourceType = "SHARED_MAIL"
                    )
                    
                    val mailRef = MailReference(
                        workItemId = 0,
                        subjectSnippet = extracted.title,
                        senderName = extracted.assignedPerson ?: "Outlook Shared Item",
                        dateString = extracted.date,
                        sourceLabel = extracted.projectLabel ?: "Outlook_Intake"
                    )
                    
                    repository.insertWorkItem(workItem, mailRef)
                }
            }
            clearIntakeBuffer()
        }
    }

    // --- MANUAL DATA ACTIONS (SINGLE) ---
    fun addManualWorkItem(
        title: String,
        description: String,
        type: String,
        date: String,
        time: String?,
        priority: String,
        projectLabel: String?,
        assignedPerson: String?,
        meetingLink: String?
    ) {
        viewModelScope.launch {
            val item = WorkItem(
                title = title,
                description = description,
                type = type,
                date = date,
                time = time,
                priority = priority,
                status = "PENDING",
                projectLabel = projectLabel?.ifBlank { null },
                assignedPerson = assignedPerson?.ifBlank { null },
                meetingLink = meetingLink?.ifBlank { null },
                sourceType = "USER"
            )
            repository.insertWorkItem(item)
        }
    }

    fun updateWorkItemDetails(item: WorkItem) {
        viewModelScope.launch {
            repository.updateWorkItem(item)
        }
    }

    fun duplicateWorkItem(item: WorkItem) {
        viewModelScope.launch {
            val decryptedTitle = CryptoManager.decrypt(item.title)
            val decryptedDesc = CryptoManager.decrypt(item.description)
            val dup = item.copy(
                id = 0,
                title = "$decryptedTitle (Copy)",
                description = decryptedDesc,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                version = 1
            )
            repository.insertWorkItem(dup)
        }
    }

    fun archiveWorkItem(item: WorkItem) {
        viewModelScope.launch {
            val decryptedTitle = CryptoManager.decrypt(item.title)
            val decryptedDesc = CryptoManager.decrypt(item.description)
            val updated = item.copy(
                title = decryptedTitle,
                description = decryptedDesc,
                archivedAt = System.currentTimeMillis()
            )
            repository.updateWorkItem(updated)
        }
    }

    fun deleteWorkItem(item: WorkItem) {
        viewModelScope.launch {
            repository.deleteWorkItem(item)
        }
    }

    fun restoreWorkItemFromTrash(trashId: Int) {
        viewModelScope.launch {
            repository.restoreWorkItem(trashId)
        }
    }

    // --- BULK OPERATIONS ---
    fun bulkMove(ids: List<Int>, newDate: String) {
        viewModelScope.launch {
            repository.bulkMoveWorkItems(ids, newDate)
        }
    }

    fun bulkDelete(items: List<WorkItem>) {
        viewModelScope.launch {
            repository.bulkDeleteWorkItems(items)
        }
    }

    fun bulkComplete(ids: List<Int>) {
        viewModelScope.launch {
            repository.bulkMarkCompleteWorkItems(ids)
        }
    }

    fun bulkArchive(ids: List<Int>) {
        viewModelScope.launch {
            repository.bulkArchiveWorkItems(ids)
        }
    }

    // --- DAILY NOTES ACTIONS ---
    fun saveDailyNote(date: String, content: String, isChecklist: Boolean) {
        viewModelScope.launch {
            val note = UserNote(
                date = date,
                content = content,
                isChecklist = isChecklist
            )
            repository.insertUserNote(note)
        }
    }

    fun updateDailyNote(note: UserNote) {
        viewModelScope.launch {
            val decryptedContent = CryptoManager.decrypt(note.content)
            repository.updateUserNote(note.copy(content = decryptedContent))
        }
    }

    fun deleteDailyNote(note: UserNote) {
        viewModelScope.launch {
            repository.deleteUserNote(note)
        }
    }

    // --- ATTACHMENTS ACTIONS ---
    fun addLocalFileAttachment(date: String, uri: Uri, name: String, type: String, size: Long) {
        viewModelScope.launch {
            val attachment = UserDayAttachment(
                date = date,
                fileUri = uri.toString(),
                fileName = name,
                fileType = type,
                fileSize = size
            )
            repository.insertUserDayAttachment(attachment)
        }
    }

    fun deleteAttachment(attachment: UserDayAttachment) {
        viewModelScope.launch {
            repository.deleteUserDayAttachment(attachment)
        }
    }

    // --- LOCAL REMINDERS ---
    fun addLocalReminder(title: String, date: String, timestamp: Long, workItemId: Int? = null) {
        viewModelScope.launch {
            val reminder = Reminder(
                workItemId = workItemId,
                title = title,
                date = date,
                reminderTime = timestamp
            )
            repository.insertReminder(reminder)
        }
    }

    // --- OFFLINE PDF REPORT GENERATION ---
    fun exportPdfReport(context: Context, dateRangeLabel: String, outputStream: OutputStream) {
        viewModelScope.launch {
            // Retrieve snapshot data
            val currentItems = activeWorkItems.value
            val currentNotes = activeNotes.value
            val currentAttachments = activeAttachments.value
            val nodeCount = graphNodes.value.size
            val edgeCount = graphEdges.value.size

            PdfGenerator.generateTimelinePdf(
                context = context,
                titleLabel = dateRangeLabel,
                items = currentItems,
                notes = currentNotes,
                attachments = currentAttachments,
                nodeCount = nodeCount,
                edgeCount = edgeCount,
                outputStream = outputStream
            )

            // Log export
            repository.logPdfExport(dateRangeLabel, "Filtered List", "Storage_Access_Framework")
        }
    }

    // --- TRASH PURGE ---
    fun securePurgeTrash() {
        viewModelScope.launch {
            val items = trashItems.value
            items.forEach { trash ->
                db.plannerDao().deleteTrashItemById(trash.id)
            }
            // Add audit entry for complete purge
            db.plannerDao().insertAuditLog(
                AuditLog(
                    actionType = "PURGE",
                    entityType = "trash_items",
                    entityId = "all",
                    details = "Secured complete local purge of all deleted timeline entries and notes",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
