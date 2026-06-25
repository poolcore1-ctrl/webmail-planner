package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val extractionResult by viewModel.extractionResult.collectAsState()
    
    // Checkbox indices tracker for extracted items
    val selectedIndices = remember { mutableStateListOf<Int>() }

    // Re-initialize selection when result changes
    LaunchedEffect(extractionResult) {
        selectedIndices.clear()
        extractionResult?.items?.indices?.forEach { selectedIndices.add(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Outlook Safe Intake",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Rule-based parsing from shared text Sheet",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }
        }

        // Shared input text paste panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightCard),
                border = BorderStroke(1.dp, AccentBorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Share Snippet Text from Outlook",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To guarantee 100% policy compliance, we do not monitor notification streams, read background folders, or request inbox tokens. Highlight email text in Outlook, tap Share, and select WorkMail Planner, or paste below:",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            viewModel.loadSharedText(it)
                        },
                        placeholder = { Text("Paste shared email content here...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("intake_input_field"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = AccentBorderLight
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                inputText = ""
                                viewModel.clearIntakeBuffer()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", color = Color.White)
                        }

                        Button(
                            onClick = {
                                val templateText = """
                                    Hi Team, Project Launch setup meetings will take place next week on Tuesday (2026-06-30) at 10:30 with Alice to discuss timeline.
                                    Please ensure you complete the database model and PDF report task before that.
                                    Contact Bob for any help. Zoom link: https://zoom.us/meet/98213791.
                                    Find the attachment layout.pdf for details.
                                """.trimIndent()
                                inputText = templateText
                                viewModel.loadSharedText(templateText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            modifier = Modifier.weight(1.5f).testTag("load_sample_intake")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Load Sample", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load Sample", color = Color.White)
                        }
                    }
                }
            }
        }

        // Local extraction results preview
        item {
            AnimatedVisibility(visible = extractionResult != null) {
                val result = extractionResult!!
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    // Executive Summary Block
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightSurface),
                        border = BorderStroke(1.dp, AccentBorderLight)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Rule-Based Parsing Summary", fontWeight = FontWeight.Bold, color = Color.White)
                                
                                // Risk Badge
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (result.riskLevel == "HIGH") DeadlineRed.copy(alpha = 0.2f) else WarningAmber.copy(alpha = 0.2f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${result.riskLevel} RISK",
                                        color = if (result.riskLevel == "HIGH") DeadlineRed else WarningAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(result.executiveSummary, fontSize = 12.sp, color = TextPrimaryDark)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(result.actionSummary, fontSize = 12.sp, color = TextSecondaryDark)
                        }
                    }

                    // Extracted Items list with checkbox selections
                    Text(
                        text = "Mapped Timeline Actions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    result.items.forEachIndexed { index, extracted ->
                        val isChecked = selectedIndices.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightCard, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isChecked) PrimaryPurple else AccentBorderLight, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (isChecked) selectedIndices.remove(index) else selectedIndices.add(index)
                                },
                                modifier = Modifier.testTag("intake_checkbox_$index")
                            )
                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(extracted.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(extracted.priority, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (extracted.priority == "HIGH") DeadlineRed else WarningAmber)
                                }
                                Text(extracted.description, fontSize = 11.sp, color = TextSecondaryDark)
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📅 ${extracted.date}", fontSize = 10.sp, color = PrimaryPurple)
                                    if (extracted.time != null) {
                                        Text("⏰ ${extracted.time}", fontSize = 10.sp, color = PrimaryPurple)
                                    }
                                    if (extracted.projectLabel != null) {
                                        Text("#${extracted.projectLabel}", fontSize = 10.sp, color = TaskGreen)
                                    }
                                }
                            }
                        }
                    }

                    // Approve import button
                    Button(
                        onClick = {
                            viewModel.approveExtraction(selectedIndices.toList())
                            inputText = ""
                            Toast.makeText(context, "Successfully approved and mapped to timeline!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("approve_intake_button")
                    ) {
                        Icon(Icons.Default.OfflinePin, contentDescription = "Approve")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Approve & Map to Timeline (${selectedIndices.size} Items)", color = Color.White)
                    }
                }
            }
        }
    }
}
