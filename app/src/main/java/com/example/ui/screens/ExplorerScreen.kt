package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserDayAttachment
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val attachments by viewModel.activeAttachments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // Categories list definitions
    val categoriesList = listOf(
        Pair("All", Icons.Default.Folder),
        Pair("Images", Icons.Default.Image),
        Pair("PDFs", Icons.Default.PictureAsPdf),
        Pair("Documents", Icons.Default.Description),
        Pair("Spreadsheets", Icons.Default.GridOn),
        Pair("Presentations", Icons.Default.Slideshow),
        Pair("Audio", Icons.Default.AudioFile),
        Pair("Archives", Icons.Default.Inventory),
        Pair("Other", Icons.Default.AttachFile)
    )

    // Filter attachments matching search and category
    val filteredFiles = remember(attachments, searchQuery, selectedCategory) {
        attachments.filter { doc ->
            // Search criteria
            val matchesSearch = doc.fileName.lowercase().contains(searchQuery.lowercase())
            
            // Category criteria
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Images" -> doc.fileType in listOf("JPG", "PNG", "WEBP", "GIF", "IMAGE")
                "PDFs" -> doc.fileType == "PDF"
                "Documents" -> doc.fileType in listOf("DOC", "DOCX", "TXT", "DOCUMENTS", "DOCUMENT")
                "Spreadsheets" -> doc.fileType in listOf("XLS", "XLSX", "CSV", "SPREADSHEETS")
                "Presentations" -> doc.fileType in listOf("PPT", "PPTX", "PRESENTATIONS")
                "Audio" -> doc.fileType in listOf("MP3", "WAV", "M4A", "AUDIO")
                "Archives" -> doc.fileType in listOf("ZIP", "RAR", "TAR", "7Z", "ARCHIVE")
                else -> doc.fileType !in listOf("PDF", "ZIP", "PNG", "JPG", "DOCX", "XLSX")
            }
            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title block
        Column {
            Text(
                text = "Timeline File Explorer",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Secure local sandboxed files and reports vault",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search document name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
            modifier = Modifier.fillMaxWidth().testTag("explorer_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CorporateIndigo,
                unfocusedBorderColor = AccentBorder
            ),
            singleLine = true
        )

        // Categories Grid Scroll block
        Text(
            text = "Categories",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(180.dp)
        ) {
            items(categoriesList) { (cat, icon) ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CorporateIndigo else DeepSlateCard)
                        .border(1.dp, if (isSelected) Color.Transparent else AccentBorder, RoundedCornerShape(8.dp))
                        .clickable { selectedCategory = cat }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, contentDescription = cat, tint = if (isSelected) Color.White else TextSecondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.White)
                    }
                }
            }
        }

        Divider(color = AccentBorder)

        // Search files matching categories
        Text(
            text = "Files Found (${filteredFiles.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        if (filteredFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, AccentBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Empty Folder", tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Sandboxed Files Linked", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Link files on specific day from Timeline screen", fontSize = 11.sp, color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFiles) { doc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "Opening file secure sandbox internally...", Toast.LENGTH_SHORT).show()
                            },
                        colors = CardDefaults.cardColors(containerColor = DeepSlateCard),
                        border = BorderStroke(1.dp, AccentBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (doc.fileType) {
                                    "PDF" -> Icons.Default.PictureAsPdf
                                    "ZIP", "RAR", "TAR" -> Icons.Default.Inventory
                                    "XLS", "XLSX", "CSV" -> Icons.Default.GridOn
                                    "JPG", "PNG", "IMAGE" -> Icons.Default.Image
                                    else -> Icons.Default.Description
                                },
                                contentDescription = "File Type",
                                tint = CorporateIndigo,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.fileName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📅 ${doc.date}", fontSize = 10.sp, color = TextSecondary)
                                    Text("•", fontSize = 10.sp, color = TextSecondary)
                                    Text("${(doc.fileSize / 1024)} KB", fontSize = 10.sp, color = TextSecondary)
                                }
                            }

                            Icon(Icons.Default.OpenInNew, contentDescription = "Open file", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
