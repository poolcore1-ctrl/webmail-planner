package com.example.data

import android.content.Context
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class DatabaseRepository(private val plannerDao: PlannerDao) {

    // Streams
    val activeWorkItems: Flow<List<WorkItem>> = plannerDao.getActiveWorkItemsFlow()
    val archivedWorkItems: Flow<List<WorkItem>> = plannerDao.getArchivedWorkItemsFlow()
    val activeNotes: Flow<List<UserNote>> = plannerDao.getActiveNotesFlow()
    val activeAttachments: Flow<List<UserDayAttachment>> = plannerDao.getActiveAttachmentsFlow()
    val activeReminders: Flow<List<Reminder>> = plannerDao.getActiveRemindersFlow()
    val pdfExportRecords: Flow<List<PdfExportRecord>> = plannerDao.getPdfExportRecordsFlow()
    val trashItems: Flow<List<TrashItem>> = plannerDao.getTrashItemsFlow()
    val auditLogs: Flow<List<AuditLog>> = plannerDao.getAuditLogsFlow()
    val graphNodes: Flow<List<GraphNode>> = plannerDao.getNodesFlow()
    val graphEdges: Flow<List<GraphEdge>> = plannerDao.getEdgesFlow()

    // --- WORK ITEM ACTIONS ---
    suspend fun insertWorkItem(item: WorkItem, mailReference: MailReference? = null): Long {
        // Secure sensitive fields
        val encryptedTitle = CryptoManager.encrypt(item.title)
        val encryptedDesc = CryptoManager.encrypt(item.description)
        
        val secureItem = item.copy(
            title = encryptedTitle,
            description = encryptedDesc,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            version = 1
        )
        val id = plannerDao.insertWorkItem(secureItem).toInt()
        
        if (mailReference != null) {
            val secureMail = mailReference.copy(
                workItemId = id,
                subjectSnippet = CryptoManager.encrypt(mailReference.subjectSnippet),
                senderName = CryptoManager.encrypt(mailReference.senderName)
            )
            plannerDao.insertMailReference(secureMail)
            
            // Add attachment indicator
            plannerDao.insertAttachmentIndicator(
                AttachmentIndicator(workItemId = id, status = item.attachmentStatus)
            )
        }

        // Sync with Knowledge Graph
        syncWorkItemToGraph(id, item)

        // Audit Log
        insertAudit("CREATE", "work_items", id.toString(), "Created work item: ${item.title}")

        return id.toLong()
    }

    suspend fun updateWorkItem(item: WorkItem) {
        val existing = plannerDao.getWorkItemById(item.id) ?: return
        
        // Save current state in version history before updating
        val oldStateJson = "{\"title\":\"${existing.title}\",\"description\":\"${existing.description}\",\"date\":\"${existing.date}\",\"time\":\"${existing.time}\",\"priority\":\"${existing.priority}\",\"status\":\"${existing.status}\",\"projectLabel\":\"${existing.projectLabel}\",\"assignedPerson\":\"${existing.assignedPerson}\",\"version\":${existing.version}}"
        plannerDao.insertVersionHistory(
            VersionHistory(
                entityType = "work_items",
                entityId = item.id,
                version = existing.version,
                stateJson = oldStateJson,
                changedAt = System.currentTimeMillis()
            )
        )

        // Encrypt updated details
        val secureItem = item.copy(
            title = CryptoManager.encrypt(item.title),
            description = CryptoManager.encrypt(item.description),
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
        plannerDao.insertWorkItem(secureItem)

        // Sync to graph
        syncWorkItemToGraph(item.id, item)

        // Audit Log
        insertAudit("UPDATE", "work_items", item.id.toString(), "Updated work item: ${item.title}")
    }

    suspend fun deleteWorkItem(item: WorkItem) {
        val existing = plannerDao.getWorkItemById(item.id) ?: return
        
        // Save to Trash
        val stateJson = "{\"title\":\"${existing.title}\",\"description\":\"${existing.description}\",\"type\":\"${existing.type}\",\"date\":\"${existing.date}\",\"time\":\"${existing.time}\",\"priority\":\"${existing.priority}\",\"status\":\"${existing.status}\",\"projectLabel\":\"${existing.projectLabel}\",\"assignedPerson\":\"${existing.assignedPerson}\",\"meetingLink\":\"${existing.meetingLink}\",\"attachmentStatus\":\"${existing.attachmentStatus}\",\"sourceType\":\"${existing.sourceType}\"}"
        val trashId = plannerDao.insertTrashItem(
            TrashItem(
                originalTable = "work_items",
                originalId = item.id,
                title = CryptoManager.decrypt(existing.title),
                restoreDataJson = stateJson,
                deletedAt = System.currentTimeMillis()
            )
        ).toInt()

        // Soft delete
        val softDeleted = existing.copy(
            deletedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        plannerDao.insertWorkItem(softDeleted)

        // Remove graph elements
        plannerDao.deleteEdgesForNode("workitem:${item.id}")
        plannerDao.deleteNodeById("workitem:${item.id}")

        // Audit Log
        insertAudit("DELETE", "work_items", item.id.toString(), "Moved work item to trash bin: ${item.title}")
    }

    suspend fun restoreWorkItem(trashId: Int) {
        val trash = plannerDao.getTrashItemById(trashId) ?: return
        if (trash.originalTable == "work_items") {
            val existing = plannerDao.getWorkItemById(trash.originalId)
            if (existing != null) {
                val restored = existing.copy(
                    deletedAt = null,
                    updatedAt = System.currentTimeMillis()
                )
                plannerDao.insertWorkItem(restored)
                
                // Re-decrypt title for graph sync
                val decryptedTitle = CryptoManager.decrypt(restored.title)
                val decryptedDesc = CryptoManager.decrypt(restored.description)
                syncWorkItemToGraph(restored.id, restored.copy(title = decryptedTitle, description = decryptedDesc))
                
                // Remove from trash
                plannerDao.deleteTrashItemById(trashId)

                // Audit Log
                insertAudit("RESTORE", "work_items", restored.id.toString(), "Restored work item: $decryptedTitle")
            }
        }
    }

    suspend fun getWorkItemHistory(workItemId: Int): List<VersionHistory> {
        return plannerDao.getVersionHistoriesForEntity("work_items", workItemId)
    }

    // --- USER NOTES ACTIONS ---
    suspend fun insertUserNote(note: UserNote): Long {
        val secureNote = note.copy(
            content = CryptoManager.encrypt(note.content),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = plannerDao.insertUserNote(secureNote).toInt()

        // Sync note node to graph
        plannerDao.insertNode(GraphNode("note:$id", "Note: " + note.content.take(20), "NOTE"))
        plannerDao.insertNode(GraphNode("date:${note.date}", note.date, "DATE"))
        plannerDao.insertEdge(GraphEdge(fromNodeId = "note:$id", toNodeId = "date:${note.date}", relationType = "linked_to"))

        insertAudit("CREATE", "user_notes", id.toString(), "Created daily note for ${note.date}")
        return id.toLong()
    }

    suspend fun updateUserNote(note: UserNote) {
        val existing = plannerDao.getUserNoteById(note.id) ?: return
        
        // Save history
        val oldStateJson = "{\"content\":\"${existing.content}\",\"date\":\"${existing.date}\",\"isChecklist\":${existing.isChecklist}}"
        plannerDao.insertVersionHistory(
            VersionHistory(
                entityType = "user_notes",
                entityId = note.id,
                version = existing.version,
                stateJson = oldStateJson,
                changedAt = System.currentTimeMillis()
            )
        )

        val secureNote = note.copy(
            content = CryptoManager.encrypt(note.content),
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
        plannerDao.insertUserNote(secureNote)

        insertAudit("UPDATE", "user_notes", note.id.toString(), "Updated daily note for ${note.date}")
    }

    suspend fun deleteUserNote(note: UserNote) {
        val existing = plannerDao.getUserNoteById(note.id) ?: return
        
        val stateJson = "{\"content\":\"${existing.content}\",\"date\":\"${existing.date}\",\"isChecklist\":${existing.isChecklist}}"
        plannerDao.insertTrashItem(
            TrashItem(
                originalTable = "user_notes",
                originalId = note.id,
                title = "Note on " + note.date,
                restoreDataJson = stateJson,
                deletedAt = System.currentTimeMillis()
            )
        )

        val softDeleted = existing.copy(
            deletedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        plannerDao.insertUserNote(softDeleted)

        // Clear graph connections
        plannerDao.deleteEdgesForNode("note:${note.id}")
        plannerDao.deleteNodeById("note:${note.id}")

        insertAudit("DELETE", "user_notes", note.id.toString(), "Moved daily note to trash bin")
    }

    suspend fun restoreUserNote(trashId: Int) {
        val trash = plannerDao.getTrashItemById(trashId) ?: return
        if (trash.originalTable == "user_notes") {
            val existing = plannerDao.getUserNoteById(trash.originalId)
            if (existing != null) {
                val restored = existing.copy(
                    deletedAt = null,
                    updatedAt = System.currentTimeMillis()
                )
                plannerDao.insertUserNote(restored)
                
                // Sync graph
                val decryptedContent = CryptoManager.decrypt(restored.content)
                plannerDao.insertNode(GraphNode("note:${restored.id}", "Note: " + decryptedContent.take(20), "NOTE"))
                plannerDao.insertNode(GraphNode("date:${restored.date}", restored.date, "DATE"))
                plannerDao.insertEdge(GraphEdge(fromNodeId = "note:${restored.id}", toNodeId = "date:${restored.date}", relationType = "linked_to"))

                plannerDao.deleteTrashItemById(trashId)
                insertAudit("RESTORE", "user_notes", restored.id.toString(), "Restored daily note for ${restored.date}")
            }
        }
    }

    // --- USER DAY ATTACHMENTS ACTIONS ---
    suspend fun insertUserDayAttachment(attachment: UserDayAttachment): Long {
        val secureAttachment = attachment.copy(
            fileUri = CryptoManager.encrypt(attachment.fileUri),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = plannerDao.insertAttachment(secureAttachment).toInt()

        // Sync file to graph
        plannerDao.insertNode(GraphNode("file:$id", attachment.fileName, "USER_FILE"))
        plannerDao.insertNode(GraphNode("date:${attachment.date}", attachment.date, "DATE"))
        plannerDao.insertEdge(GraphEdge(fromNodeId = "file:$id", toNodeId = "date:${attachment.date}", relationType = "linked_to"))

        insertAudit("CREATE", "user_day_attachments", id.toString(), "Attached file: ${attachment.fileName}")
        return id.toLong()
    }

    suspend fun deleteUserDayAttachment(attachment: UserDayAttachment) {
        val existing = plannerDao.getAttachmentById(attachment.id) ?: return
        
        val softDeleted = existing.copy(
            deletedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        plannerDao.insertAttachment(softDeleted)

        // Clear from graph
        plannerDao.deleteEdgesForNode("file:${attachment.id}")
        plannerDao.deleteNodeById("file:${attachment.id}")

        insertAudit("DELETE", "user_day_attachments", attachment.id.toString(), "Deleted attached file: ${attachment.fileName}")
    }

    // --- BULK OPERATIONS ---
    suspend fun bulkMoveWorkItems(ids: List<Int>, newDate: String) {
        for (id in ids) {
            val existing = plannerDao.getWorkItemById(id)
            if (existing != null) {
                val updated = existing.copy(date = newDate, updatedAt = System.currentTimeMillis())
                plannerDao.insertWorkItem(updated)
                
                val decryptedTitle = CryptoManager.decrypt(updated.title)
                val decryptedDesc = CryptoManager.decrypt(updated.description)
                syncWorkItemToGraph(id, updated.copy(title = decryptedTitle, description = decryptedDesc))
                
                insertAudit("UPDATE", "work_items", id.toString(), "Bulk moved work item to $newDate")
            }
        }
    }

    suspend fun bulkDeleteWorkItems(items: List<WorkItem>) {
        for (item in items) {
            deleteWorkItem(item)
        }
    }

    suspend fun bulkMarkCompleteWorkItems(ids: List<Int>) {
        for (id in ids) {
            val existing = plannerDao.getWorkItemById(id)
            if (existing != null) {
                val updated = existing.copy(status = "COMPLETED", updatedAt = System.currentTimeMillis())
                plannerDao.insertWorkItem(updated)
                
                val decryptedTitle = CryptoManager.decrypt(updated.title)
                val decryptedDesc = CryptoManager.decrypt(updated.description)
                syncWorkItemToGraph(id, updated.copy(title = decryptedTitle, description = decryptedDesc))

                insertAudit("UPDATE", "work_items", id.toString(), "Bulk completed work item")
            }
        }
    }

    suspend fun bulkArchiveWorkItems(ids: List<Int>) {
        for (id in ids) {
            val existing = plannerDao.getWorkItemById(id)
            if (existing != null) {
                val updated = existing.copy(archivedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
                plannerDao.insertWorkItem(updated)
                
                val decryptedTitle = CryptoManager.decrypt(updated.title)
                val decryptedDesc = CryptoManager.decrypt(updated.description)
                syncWorkItemToGraph(id, updated.copy(title = decryptedTitle, description = decryptedDesc))

                insertAudit("ARCHIVE", "work_items", id.toString(), "Bulk archived work item")
            }
        }
    }

    // --- GRAPH GRAPH-SYNC UTILITY ---
    private suspend fun syncWorkItemToGraph(id: Int, item: WorkItem) {
        val nodeKey = "workitem:$id"
        plannerDao.deleteEdgesForNode(nodeKey)

        // 1. Insert Node
        val typeLabel = item.type
        plannerDao.insertNode(GraphNode(nodeKey, item.title, typeLabel))

        // 2. Link to Date
        val dateNodeKey = "date:${item.date}"
        plannerDao.insertNode(GraphNode(dateNodeKey, item.date, "DATE"))
        plannerDao.insertEdge(GraphEdge(fromNodeId = nodeKey, toNodeId = dateNodeKey, relationType = "due_on"))

        // 3. Link to Project
        val proj = item.projectLabel
        if (!proj.isNullOrBlank()) {
            val projKey = "project:$proj"
            plannerDao.insertNode(GraphNode(projKey, proj, "PROJECT"))
            plannerDao.insertEdge(GraphEdge(fromNodeId = nodeKey, toNodeId = projKey, relationType = "related_to"))
        }

        // 4. Link to Person
        val person = item.assignedPerson
        if (!person.isNullOrBlank()) {
            val personKey = "person:$person"
            plannerDao.insertNode(GraphNode(personKey, person, "PERSON"))
            plannerDao.insertEdge(GraphEdge(fromNodeId = nodeKey, toNodeId = personKey, relationType = "assigned_to"))
        }

        // 5. Link to Outlook Reference
        val reference = plannerDao.getMailReferenceForWorkItem(id)
        if (reference != null) {
            val refKey = "mailref:${reference.id}"
            val decryptedSubj = CryptoManager.decrypt(reference.subjectSnippet)
            plannerDao.insertNode(GraphNode(refKey, decryptedSubj, "OUTLOOK_REF"))
            plannerDao.insertEdge(GraphEdge(fromNodeId = nodeKey, toNodeId = refKey, relationType = "source_of"))
        }
    }

    // --- PDF LOGGING ---
    suspend fun logPdfExport(range: String, filter: String, path: String) {
        plannerDao.insertPdfExportRecord(
            PdfExportRecord(
                dateRange = range,
                filterCriteria = filter,
                filePath = path
            )
        )
        insertAudit("EXPORT_PDF", "pdf_export_records", "0", "Exported timeline PDF for $range with criteria: $filter")
    }

    // --- AUDIT SYSTEM ---
    private suspend fun insertAudit(action: String, entity: String, entId: String, details: String) {
        plannerDao.insertAuditLog(
            AuditLog(
                actionType = action,
                entityType = entity,
                entityId = entId,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // --- REMINDERS ---
    suspend fun insertReminder(reminder: Reminder) {
        plannerDao.insertReminder(reminder)
        insertAudit("CREATE", "reminders", reminder.workItemId?.toString() ?: "0", "Created reminder for ${reminder.date}")
    }

    suspend fun getWorkItemDetailsFlow(workItemId: Int): Flow<WorkItem?> {
        // Return matching live work item flow
        return kotlinx.coroutines.flow.flow {
            emit(plannerDao.getWorkItemById(workItemId))
        }
    }
}
