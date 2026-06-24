package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {

    // --- WORK ITEMS ---
    @Query("SELECT * FROM work_items WHERE deletedAt IS NULL AND archivedAt IS NULL ORDER BY date ASC, time ASC")
    fun getActiveWorkItemsFlow(): Flow<List<WorkItem>>

    @Query("SELECT * FROM work_items WHERE deletedAt IS NULL AND archivedAt IS NOT NULL ORDER BY date DESC")
    fun getArchivedWorkItemsFlow(): Flow<List<WorkItem>>

    @Query("SELECT * FROM work_items WHERE id = :id")
    suspend fun getWorkItemById(id: Int): WorkItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkItem(item: WorkItem): Long

    @Update
    suspend fun updateWorkItem(item: WorkItem)

    @Delete
    suspend fun deleteWorkItemDirect(item: WorkItem)

    // --- MAIL REFERENCES ---
    @Query("SELECT * FROM mail_references WHERE workItemId = :workItemId")
    suspend fun getMailReferenceForWorkItem(workItemId: Int): MailReference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMailReference(reference: MailReference): Long

    @Query("DELETE FROM mail_references WHERE workItemId = :workItemId")
    suspend fun deleteMailReferenceForWorkItem(workItemId: Int)

    // --- USER NOTES ---
    @Query("SELECT * FROM user_notes WHERE deletedAt IS NULL AND archivedAt IS NULL ORDER BY date DESC")
    fun getActiveNotesFlow(): Flow<List<UserNote>>

    @Query("SELECT * FROM user_notes WHERE date = :date AND deletedAt IS NULL AND archivedAt IS NULL")
    fun getNotesForDateFlow(date: String): Flow<List<UserNote>>

    @Query("SELECT * FROM user_notes WHERE id = :id")
    suspend fun getUserNoteById(id: Int): UserNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserNote(note: UserNote): Long

    @Update
    suspend fun updateUserNote(note: UserNote)

    @Delete
    suspend fun deleteUserNoteDirect(note: UserNote)

    // --- USER DAY ATTACHMENTS ---
    @Query("SELECT * FROM user_day_attachments WHERE deletedAt IS NULL AND archivedAt IS NULL ORDER BY date DESC")
    fun getActiveAttachmentsFlow(): Flow<List<UserDayAttachment>>

    @Query("SELECT * FROM user_day_attachments WHERE date = :date AND deletedAt IS NULL AND archivedAt IS NULL")
    fun getAttachmentsForDateFlow(date: String): Flow<List<UserDayAttachment>>

    @Query("SELECT * FROM user_day_attachments WHERE id = :id")
    suspend fun getAttachmentById(id: Int): UserDayAttachment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: UserDayAttachment): Long

    @Update
    suspend fun updateAttachment(attachment: UserDayAttachment)

    @Delete
    suspend fun deleteAttachmentDirect(attachment: UserDayAttachment)

    // --- REMINDERS ---
    @Query("SELECT * FROM reminders WHERE deletedAt IS NULL ORDER BY reminderTime ASC")
    fun getActiveRemindersFlow(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    // --- ATTACHMENT INDICATORS ---
    @Query("SELECT * FROM attachment_indicators WHERE workItemId = :workItemId")
    suspend fun getAttachmentIndicatorForWorkItem(workItemId: Int): AttachmentIndicator?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachmentIndicator(indicator: AttachmentIndicator): Long

    // --- CALENDAR REFERENCES ---
    @Query("SELECT * FROM calendar_references WHERE workItemId = :workItemId")
    suspend fun getCalendarReferenceForWorkItem(workItemId: Int): CalendarReference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarReference(reference: CalendarReference): Long

    // --- PDF EXPORT RECORDS ---
    @Query("SELECT * FROM pdf_export_records ORDER BY exportedAt DESC")
    fun getPdfExportRecordsFlow(): Flow<List<PdfExportRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdfExportRecord(record: PdfExportRecord): Long

    // --- GRAPH NODES ---
    @Query("SELECT * FROM graph_nodes")
    fun getNodesFlow(): Flow<List<GraphNode>>

    @Query("SELECT * FROM graph_nodes")
    suspend fun getAllNodes(): List<GraphNode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: GraphNode)

    @Query("DELETE FROM graph_nodes WHERE id = :id")
    suspend fun deleteNodeById(id: String)

    // --- GRAPH EDGES ---
    @Query("SELECT * FROM graph_edges")
    fun getEdgesFlow(): Flow<List<GraphEdge>>

    @Query("SELECT * FROM graph_edges")
    suspend fun getAllEdges(): List<GraphEdge>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdge(edge: GraphEdge)

    @Query("DELETE FROM graph_edges WHERE fromNodeId = :nodeId OR toNodeId = :nodeId")
    suspend fun deleteEdgesForNode(nodeId: String)

    // --- TRASH ITEMS ---
    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun getTrashItemsFlow(): Flow<List<TrashItem>>

    @Query("SELECT * FROM trash_items WHERE id = :id")
    suspend fun getTrashItemById(id: Int): TrashItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(item: TrashItem): Long

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteTrashItemById(id: Int)

    // --- AUDIT LOGS ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAuditLogsFlow(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long

    // --- VERSION HISTORIES ---
    @Query("SELECT * FROM version_histories WHERE entityType = :entityType AND entityId = :entityId ORDER BY version DESC")
    suspend fun getVersionHistoriesForEntity(entityType: String, entityId: Int): List<VersionHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersionHistory(history: VersionHistory): Long
}
