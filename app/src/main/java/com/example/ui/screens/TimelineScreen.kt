package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.security.CryptoManager
import com.example.data.UserDayAttachment
import com.example.data.UserNote
import com.example.data.WorkItem
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val workItems by viewModel.activeWorkItems.collectAsState()
    val notes by viewModel.activeNotes.collectAsState()
    val attachments by viewModel.activeAttachments.collectAsState()

    var activeTab by remember { mutableStateOf("Day") } // Day, Week, Month, Agenda
    var showAddDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showExportWarningDialog by remember { mutableStateOf(false) }
    var showOutlookVerifyDialog by remember { mutableStateOf(false) }
    var selectedVerifyItem by remember { mutableStateOf<WorkItem?>(null) }

    // Multi-select bulk state
    val selectedItemIds = remember { mutableStateListOf<Int>() }
    var isBulkMode by remember { mutableStateOf(false) }

    // SAF Document Creators and Openers
    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    viewModel.exportPdfReport(context, "Range: $selectedDate", os)
                }
                Toast.makeText(context, "PDF Report exported successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            var name = "Attached_Document"
            var size = 0L
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) name = it.getString(nameIndex)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) size = it.getLong(sizeIndex)
                }
            }
            viewModel.addLocalFileAttachment(
                date = selectedDate,
                uri = uri,
                name = name,
                type = name.substringAfterLast(".", "OTHER").uppercase(),
                size = size
            )
            Toast.makeText(context, "Added file to vault!", Toast.LENGTH_SHORT).show()
        }
    }

    // Filter data matching selected view
    val displayedItems = remember(selectedDate, activeTab, workItems) {
        when (activeTab) {
            "Day" -> workItems.filter { it.date == selectedDate }
            "Week" -> {
                // Approximate matching this week
                val cal = Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val base = sdf.parse(selectedDate) ?: Date()
                cal.time = base
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                val start = sdf.format(cal.time)
                cal.add(Calendar.DAY_OF_WEEK, 6)
                val end = sdf.format(cal.time)
                workItems.filter { it.date >= start && it.date <= end }
            }
            "Month" -> {
                val prefix = selectedDate.take(7) // YYYY-MM
                workItems.filter { it.date.startsWith(prefix) }
            }
            else -> workItems // Agenda View shows everything
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CorporateIndigo,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_item_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Work Item")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepSlateBackground)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WorkMail Planner",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Local Timeline Sandbox",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // PDF Export trigger button
                IconButton(
                    onClick = { showExportWarningDialog = true },
                    modifier = Modifier
                        .background(DeepSlateCard, RoundedCornerShape(8.dp))
                        .testTag("export_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF Report", tint = CorporateAmber)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // View Mode Toggle (Day, Week, Month, Agenda)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepSlateCard, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Day", "Week", "Month", "Agenda").forEach { tab ->
                    val selected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) CorporateIndigo else Color.Transparent)
                            .clickable { activeTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar Strip (Horizontal Days Carousel)
            if (activeTab == "Day" || activeTab == "Week") {
                CalendarStrip(selectedDate) { date ->
                    viewModel.selectDate(date)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bulk action bar
            if (isBulkMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CorporateIndigo.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedItemIds.size} Selected Items",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.bulkComplete(selectedItemIds.toList())
                                selectedItemIds.clear()
                                isBulkMode = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CorporateEmerald),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Complete", fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                val toDelete = workItems.filter { selectedItemIds.contains(it.id) }
                                viewModel.bulkDelete(toDelete)
                                selectedItemIds.clear()
                                isBulkMode = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CorporateRuby),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Trash", fontSize = 11.sp)
                        }
                        IconButton(onClick = {
                            selectedItemIds.clear()
                            isBulkMode = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel bulk mode", tint = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Agenda / Content Layout
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Agenda Timeline Items
                if (displayedItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentBorder, RoundedCornerShape(8.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.CalendarMonth, contentDescription = "No timeline items", tint = TextSecondary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Mapped Work Items Today", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Paste from Outlook share intake sheet or tap '+' FAB", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(displayedItems) { item ->
                        val isSelected = selectedItemIds.contains(item.id)
                        WorkItemCard(
                            item = item,
                            isBulkSelected = isSelected,
                            isBulkActive = isBulkMode,
                            onToggleSelect = {
                                if (selectedItemIds.contains(item.id)) {
                                    selectedItemIds.remove(item.id)
                                    if (selectedItemIds.isEmpty()) isBulkMode = false
                                } else {
                                    selectedItemIds.add(item.id)
                                    isBulkMode = true
                                }
                            },
                            onStatusToggle = {
                                val decryptedTitle = com.example.security.CryptoManager.decrypt(item.title)
                                val decryptedDesc = com.example.security.CryptoManager.decrypt(item.description)
                                viewModel.updateWorkItemDetails(
                                    item.copy(
                                        title = decryptedTitle,
                                        description = decryptedDesc,
                                        status = if (item.status == "PENDING") "COMPLETED" else "PENDING"
                                    )
                                )
                            },
                            onVerifyInOutlook = {
                                selectedVerifyItem = item
                                showOutlookVerifyDialog = true
                            },
                            onDuplicate = { viewModel.duplicateWorkItem(item) },
                            onArchive = { viewModel.archiveWorkItem(item) },
                            onTrash = { viewModel.deleteWorkItem(item) }
                        )
                    }
                }

                // Section 2: Vault Notes on current day
                val dateNotes = notes.filter { it.date == selectedDate }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Personal Day Vault",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (dateNotes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DeepSlateCard),
                            border = BorderStroke(1.dp, AccentBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("No text notes added to this day", fontSize = 12.sp, color = TextSecondary)
                                Button(
                                    onClick = { viewModel.saveDailyNote(selectedDate, "New hand-written task notes", false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CorporateIndigo),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("+ Note", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    items(dateNotes) { note ->
                        NoteItemCard(
                            note = note,
                            onToggleCheck = {
                                val decrypted = com.example.security.CryptoManager.decrypt(note.content)
                                viewModel.updateDailyNote(note.copy(content = decrypted, isChecklist = !note.isChecklist))
                            },
                            onUpdateText = { text ->
                                viewModel.updateDailyNote(note.copy(content = text))
                            },
                            onDelete = { viewModel.deleteDailyNote(note) }
                        )
                    }
                }

                // Section 3: Vault Local Documents Linked via SAF
                val dateDocs = attachments.filter { it.date == selectedDate }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sandbox Day Vault Files",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Outlined.FileOpen, contentDescription = "Add File via SAF", tint = CorporateIndigo)
                        }
                    }
                }

                if (dateDocs.isEmpty()) {
                    item {
                        Text(
                            text = "No custom sandbox files linked for today.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    items(dateDocs) { doc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepSlateCard, RoundedCornerShape(6.dp))
                                .border(1.dp, AccentBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attachment Indicator", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(doc.fileName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${doc.fileType} | ${(doc.fileSize / 1024)} KB", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteAttachment(doc) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove file link", tint = CorporateRuby, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Outlook verification deep link modal
    if (showOutlookVerifyDialog && selectedVerifyItem != null) {
        val item = selectedVerifyItem!!
        Dialog(onDismissRequest = { showOutlookVerifyDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                border = BorderStroke(1.dp, AccentBorder),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Verify in Microsoft Outlook", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Since we protect absolute offline corporate security, we NEVER read or sync directly with Outlook server mailboxes. Use this context helper to search in your outlook app securely:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Context Metadata Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepSlateCard, RoundedCornerShape(6.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔍 Subject Snippet: ${com.example.security.CryptoManager.decrypt(item.title)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("👤 Assignee / Person: ${item.assignedPerson ?: "Unspecified"}", fontSize = 12.sp, color = TextSecondary)
                        Text("📅 Date Mapped: ${item.date}", fontSize = 12.sp, color = TextSecondary)
                        Text("🏷️ Project Label: ${item.projectLabel ?: "General"}", fontSize = 12.sp, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showOutlookVerifyDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", color = Color.White)
                        }
                        Button(
                            onClick = {
                                showOutlookVerifyDialog = false
                                Toast.makeText(context, "Opening Outlook search helper app...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CorporateIndigo),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Open Outlook", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // PDF export corporate policy warning dialog
    if (showExportWarningDialog) {
        Dialog(onDismissRequest = { showExportWarningDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                border = BorderStroke(1.dp, AccentBorder),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Export Work Timeline PDF", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This report may contain work metadata. Export only if allowed by your organization.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CorporateAmber
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "The resulting PDF is compiled completely on-device. No data or metadata will leave your secure container. Do you agree to conform with company guidelines?",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showExportWarningDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                        Button(
                            onClick = {
                                showExportWarningDialog = false
                                pdfExportLauncher.launch("WorkMail_Timeline_Report_${selectedDate}.pdf")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CorporateIndigo),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Agree & Export", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Dialog to add manual workitem
    if (showAddDialog) {
        AddManualItemDialog(
            selectedDate = selectedDate,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, type, date, time, priority, project, person, link ->
                viewModel.addManualWorkItem(title, desc, type, date, time, priority, project, person, link)
                showAddDialog = false
            }
        )
    }
}

// Calendar Strip Horizontal Scroll Widget
@Composable
fun CalendarStrip(selectedDate: String, onDateSelect: (String) -> Unit) {
    val datesList = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        // Populate 15 preceding and 15 succeeding days
        cal.add(Calendar.DAY_OF_YEAR, -15)
        for (i in 0..30) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(datesList) { date ->
            val isSelected = date == selectedDate
            val parsedDate = remember(date) {
                val cal = Calendar.getInstance()
                cal.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: Date()
                val dayOfWeek = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US) ?: ""
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString()
                Pair(dayOfWeek, dayOfMonth)
            }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) CorporateIndigo else DeepSlateCard)
                    .border(1.dp, if (isSelected) Color.Transparent else AccentBorder, RoundedCornerShape(8.dp))
                    .clickable { onDateSelect(date) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = parsedDate.first,
                    fontSize = 11.sp,
                    color = if (isSelected) Color.White else TextSecondary
                )
                Text(
                    text = parsedDate.second,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color.White
                )
            }
        }
    }
}

// Individual Agenda Card Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkItemCard(
    item: WorkItem,
    isBulkSelected: Boolean,
    isBulkActive: Boolean,
    onToggleSelect: () -> Unit,
    onStatusToggle: () -> Unit,
    onVerifyInOutlook: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit
) {
    val decryptedTitle = remember(item.title) { com.example.security.CryptoManager.decrypt(item.title) }
    val decryptedDesc = remember(item.description) { com.example.security.CryptoManager.decrypt(item.description) }
    val isCompleted = item.status == "COMPLETED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isBulkActive) onToggleSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isBulkSelected) CorporateIndigo.copy(alpha = 0.2f) else DeepSlateCard
        ),
        border = BorderStroke(
            1.dp,
            if (isBulkSelected) CorporateIndigo else AccentBorder
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Checkbox, Badge, priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = if (isBulkActive) isBulkSelected else isCompleted,
                        onCheckedChange = { if (isBulkActive) onToggleSelect() else onStatusToggle() },
                        modifier = Modifier.size(32.dp).testTag("checkbox_${item.id}")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Task Type badge
                    Box(
                        modifier = Modifier
                            .background(
                                when (item.type) {
                                    "MEETING" -> CorporateIndigo.copy(alpha = 0.15f)
                                    "DEADLINE" -> CorporateAmber.copy(alpha = 0.15f)
                                    else -> CorporateEmerald.copy(alpha = 0.15f)
                                },
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.type,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (item.type) {
                                "MEETING" -> CorporateIndigo
                                "DEADLINE" -> CorporateAmber
                                else -> CorporateEmerald
                            }
                        )
                    }
                }

                // Priority Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (item.priority) {
                                    "HIGH" -> CorporateRuby
                                    "MEDIUM" -> CorporateAmber
                                    else -> CorporateEmerald
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.priority,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (item.priority) {
                            "HIGH" -> CorporateRuby
                            "MEDIUM" -> CorporateAmber
                            else -> CorporateEmerald
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body: Title & description
            Text(
                text = decryptedTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) TextSecondary else Color.White,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (decryptedDesc.isNotEmpty()) {
                Text(
                    text = decryptedDesc,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer metadata elements: links & metadata tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time & Labels
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.time != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Time marker", tint = TextSecondary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(item.time, fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                    if (item.projectLabel != null) {
                        Text("#${item.projectLabel}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CorporateIndigo)
                    }
                }

                // Interaction controls (Verify Outlook, Duplicate, Archive, Delete)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onVerifyInOutlook, modifier = Modifier.size(24.dp).testTag("verify_outlook_button_${item.id}")) {
                        Icon(Icons.Outlined.Email, contentDescription = "Verify in Outlook", tint = CorporateIndigo, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate item", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onArchive, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Archive, contentDescription = "Archive item", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onTrash, modifier = Modifier.size(24.dp).testTag("delete_button_${item.id}")) {
                        Icon(Icons.Default.Delete, contentDescription = "Move to trash", tint = CorporateRuby, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Attachment indicator safety alert container
            if (item.attachmentStatus != "Not Detected") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(4.dp))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Attachment, contentDescription = "Attachment Indicator", tint = CorporateAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Attachment Detected: [${item.attachmentStatus}]. Open Microsoft Outlook securely to view binary attachments.",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}

// User text note list items
@Composable
fun NoteItemCard(
    note: UserNote,
    onToggleCheck: () -> Unit,
    onUpdateText: (String) -> Unit,
    onDelete: () -> Unit
) {
    val decrypted = remember(note.content) { com.example.security.CryptoManager.decrypt(note.content) }
    var textState by remember { mutableStateOf(decrypted) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DeepSlateCard),
        border = BorderStroke(1.dp, AccentBorder)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = note.isChecklist,
                onCheckedChange = { onToggleCheck() },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = textState,
                onValueChange = {
                    textState = it
                    onUpdateText(it)
                },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                maxLines = 2
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = CorporateRuby, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// Add Item Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualItemDialog(
    selectedDate: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String?, String, String?, String?, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("TASK") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var time by remember { mutableStateOf("") }
    var project by remember { mutableStateOf("") }
    var person by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
            border = BorderStroke(1.dp, AccentBorder),
            modifier = Modifier.padding(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Add Manual Work Item", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth().testTag("add_item_title_field")
                    )
                }

                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Type dropdown
                        OutlinedTextField(
                            value = type,
                            onValueChange = { type = it },
                            label = { Text("Type (TASK/MEETING/DEADLINE)") },
                            modifier = Modifier.weight(1f)
                        )
                        // Priority dropdown
                        OutlinedTextField(
                            value = priority,
                            onValueChange = { priority = it },
                            label = { Text("Priority (HIGH/MEDIUM/LOW)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Time (e.g. 14:30)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = project,
                            onValueChange = { project = it },
                            label = { Text("Project Label") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = person,
                        onValueChange = { person = it },
                        label = { Text("Assigned Person") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("Meeting Link") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onSave(title, desc, type, selectedDate, time.ifBlank { null }, priority, project, person, link)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CorporateIndigo),
                            modifier = Modifier.testTag("save_manual_item_button")
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
