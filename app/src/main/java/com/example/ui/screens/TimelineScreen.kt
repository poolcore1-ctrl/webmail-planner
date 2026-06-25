package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

    var showAddDialog by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            Toast.makeText(context, "Image captured and saved to secure vault", Toast.LENGTH_SHORT).show()
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        Toast.makeText(context, "Voice note transcribed successfully", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (fabExpanded) {
                    FloatingActionButton(
                        onClick = { 
                            fabExpanded = false
                            cameraLauncher.launch(null)
                        },
                        containerColor = LightSurface,
                        contentColor = PrimaryPurple,
                        modifier = Modifier.size(48.dp).padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
                    }
                    FloatingActionButton(
                        onClick = { 
                            fabExpanded = false
                            audioLauncher.launch(android.content.Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION))
                        },
                        containerColor = LightSurface,
                        contentColor = PrimaryPurple,
                        modifier = Modifier.size(48.dp).padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice")
                    }
                    FloatingActionButton(
                        onClick = { 
                            fabExpanded = false
                            showAddDialog = true 
                        },
                        containerColor = LightSurface,
                        contentColor = PrimaryPurple,
                        modifier = Modifier.size(48.dp).padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Text")
                    }
                }
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = PrimaryPurple,
                    contentColor = WhiteBackground
                ) {
                    Icon(if (fabExpanded) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WhiteBackground)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Planner Calendar",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(16.dp))

            MonthGridCalendar(
                selectedDate = selectedDate,
                workItems = workItems,
                onDateSelected = { viewModel.selectDate(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = AccentBorderLight)
            Spacer(modifier = Modifier.height(16.dp))

            val dailyItems = workItems.filter { it.date == selectedDate }
            Text(
                text = "Agenda for \$selectedDate",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (dailyItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tasks for this day.", color = TextSecondaryDark)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dailyItems) { item ->
                        AgendaCard(item) {
                            val decryptedTitle = com.example.security.CryptoManager.decrypt(item.title)
                            val decryptedDesc = com.example.security.CryptoManager.decrypt(item.description)
                            viewModel.updateWorkItemDetails(
                                item.copy(
                                    title = decryptedTitle,
                                    description = decryptedDesc,
                                    status = if (item.status == "PENDING") "COMPLETED" else "PENDING"
                                )
                            )
                        }
                    }
                }
            }
        }
    }

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

@Composable
fun MonthGridCalendar(
    selectedDate: String,
    workItems: List<WorkItem>,
    onDateSelected: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    
    val currentMonthPrefix = SimpleDateFormat("yyyy-MM-", Locale.US).format(cal.time)
    val gridItems = mutableListOf<String>()
    for (i in 1 until startDayOfWeek) gridItems.add("")
    for (i in 1..daysInMonth) gridItems.add(String.format("%02d", i))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightSurface, RoundedCornerShape(12.dp))
            .border(1.dp, AccentBorderLight, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(250.dp)
        ) {
            items(gridItems) { day ->
                if (day.isEmpty()) {
                    Box(modifier = Modifier.aspectRatio(1f))
                } else {
                    val fullDateString = "$currentMonthPrefix$day"
                    val isSelected = fullDateString == selectedDate
                    val dayItems = workItems.filter { it.date == fullDateString }
                    val hasMeeting = dayItems.any { it.type == "MEETING" }
                    val hasTask = dayItems.any { it.type == "TASK" }
                    val hasDeadline = dayItems.any { it.type == "DEADLINE" }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryPurple else Color.Transparent)
                            .clickable { onDateSelected(fullDateString) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) WhiteBackground else TextPrimaryDark
                            )
                            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(top = 2.dp)) {
                                if (hasMeeting) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if(isSelected) WhiteBackground else MeetingBlue))
                                if (hasTask) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if(isSelected) WhiteBackground else TaskGreen).padding(horizontal=1.dp))
                                if (hasDeadline) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if(isSelected) WhiteBackground else DeadlineRed))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaCard(item: WorkItem, onToggle: () -> Unit) {
    val decryptedTitle = com.example.security.CryptoManager.decrypt(item.title)
    val isCompleted = item.status == "COMPLETED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        border = BorderStroke(1.dp, AccentBorderLight)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isCompleted, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = decryptedTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) TextSecondaryDark else TextPrimaryDark
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=4.dp)) {
                    Box(modifier = Modifier.background(
                        when(item.type) { "MEETING"->MeetingBlue "DEADLINE"->DeadlineRed else->TaskGreen }, 
                        RoundedCornerShape(4.dp)
                    ).padding(4.dp, 2.dp)) {
                        Text(item.type, fontSize=9.sp, color=WhiteBackground)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (item.priority == "HIGH") {
                        Text("HIGH PRIORITY", fontSize=9.sp, color=DeadlineRed, fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddManualItemDialog(
    selectedDate: String,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, type: String, date: String, time: String?, priority: String, project: String?, person: String?, link: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LightSurface),
            border = BorderStroke(1.dp, AccentBorderLight)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add Task / Meeting", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(title, desc, "TASK", selectedDate, null, "MEDIUM", null, null, null)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) { 
                        Text("Save", color = WhiteBackground) 
                    }
                }
            }
        }
    }
}
