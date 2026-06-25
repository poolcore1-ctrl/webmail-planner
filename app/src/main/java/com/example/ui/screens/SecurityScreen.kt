package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
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
import com.example.data.AuditLog
import com.example.data.TrashItem
import com.example.security.CryptoManager
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val auditLogs by viewModel.auditLogs.collectAsState()
    val trashItems by viewModel.trashItems.collectAsState()

    val sharedPrefs = remember { context.getSharedPreferences("WorkMailPlannerPrefs", Context.MODE_PRIVATE) }
    var flagSecureEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("FLAG_SECURE_ENABLED", false)) }

    val isRooted = remember { CryptoManager.isDeviceRooted() }
    val isDebug = remember { CryptoManager.isDebugBuild(context) }

    var selectedSection by remember { mutableStateOf("Audit") } // Audit, Trash, Specs

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Block
        item {
            Column {
                Text(
                    text = "Security & Privacy",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Sandbox telemetry diagnostics and safety controls",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }
        }

        // Real-Time Safety Indicators Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Keystore block
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = LightCard),
                    border = BorderStroke(1.dp, AccentBorderLight)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Outlined.Shield, contentDescription = "Keystore Active", tint = TaskGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Hardware KeyStore", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("AES-2CM Encrypted", fontSize = 10.sp, color = TaskGreen)
                    }
                }

                // Root indicator block
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = LightCard),
                    border = BorderStroke(1.dp, if (isRooted) DeadlineRed else AccentBorderLight)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(
                            imageVector = if (isRooted) Icons.Default.Warning else Icons.Default.VerifiedUser,
                            contentDescription = "Root status",
                            tint = if (isRooted) DeadlineRed else TaskGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Device Root Integrity", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(if (isRooted) "COMPROMISED (ROOT)" else "Verified Secure", fontSize = 10.sp, color = if (isRooted) DeadlineRed else TaskGreen)
                    }
                }

                // Debug indicator block
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = LightCard),
                    border = BorderStroke(1.dp, if (isDebug) WarningAmber else AccentBorderLight)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(
                            imageVector = if (isDebug) Icons.Default.BugReport else Icons.Default.CheckCircle,
                            contentDescription = "Debug build",
                            tint = if (isDebug) WarningAmber else TaskGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Build Compilation", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(if (isDebug) "Debug APK Mode" else "Release Verified", fontSize = 10.sp, color = if (isDebug) WarningAmber else TaskGreen)
                    }
                }
            }
        }

        // FLAG_SECURE Screen protection setting block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightCard),
                border = BorderStroke(1.dp, PrimaryPurple)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FLAG_SECURE Privacy Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "When active, prevents OS screenshotting, screen shares, and masks recents preview window to protect corporate confidentiality.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                    Switch(
                        checked = flagSecureEnabled,
                        onCheckedChange = {
                            flagSecureEnabled = it
                            sharedPrefs.edit().putBoolean("FLAG_SECURE_ENABLED", it).apply()
                            Toast.makeText(
                                context,
                                "FLAG_SECURE: Restart app or navigate back to trigger window safety parameters.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple),
                        modifier = Modifier.testTag("flag_secure_toggle")
                    )
                }
            }
        }

        // Section Select Toggles (Audit Logs, Trash Bin, Security Specs)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightCard, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Audit", "Trash", "Specs").forEach { sec ->
                    val selected = selectedSection == sec
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) PrimaryPurple else Color.Transparent)
                            .clickable { selectedSection = sec }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (sec == "Audit") "Audit Logs" else if (sec == "Trash") "Trash Bin (${trashItems.size})" else "Diagnostic Specs",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else TextSecondaryDark
                        )
                    }
                }
            }
        }

        // Dynamic Section rendering
        when (selectedSection) {
            "Audit" -> {
                if (auditLogs.isEmpty()) {
                    item {
                        Text("No audit log events recorded yet.", fontSize = 12.sp, color = TextSecondaryDark, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    items(auditLogs) { log ->
                        AuditRow(log)
                    }
                }
            }
            "Trash" -> {
                if (trashItems.isEmpty()) {
                    item {
                        Text("Trash bin is empty.", fontSize = 12.sp, color = TextSecondaryDark, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    item {
                        Button(
                            onClick = {
                                viewModel.securePurgeTrash()
                                Toast.makeText(context, "Secure local purge executed successfully!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeadlineRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("secure_purge_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Purge All")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Secure local Purge All Trash", color = Color.White)
                        }
                    }
                    items(trashItems) { trash ->
                        TrashRow(trash) {
                            viewModel.restoreWorkItemFromTrash(trash.id)
                            Toast.makeText(context, "Item restored successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            else -> {
                item {
                    SpecsCard()
                }
            }
        }
    }
}

// Audit Row layout
@Composable
fun AuditRow(log: AuditLog) {
    val dateStr = remember(log.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(log.timestamp))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightCard, RoundedCornerShape(6.dp))
            .border(1.dp, AccentBorderLight, RoundedCornerShape(6.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            when (log.actionType) {
                                "CREATE" -> TaskGreen
                                "DELETE", "PURGE" -> DeadlineRed
                                "EXPORT_PDF" -> WarningAmber
                                else -> PrimaryPurple
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("[${log.actionType}] ${log.entityType}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(log.details, fontSize = 12.sp, color = TextPrimaryDark)
            Text(dateStr, fontSize = 9.sp, color = TextSecondaryDark)
        }
    }
}

// Trash Bin List Row Composable
@Composable
fun TrashRow(trash: TrashItem, onRestore: () -> Unit) {
    val dateStr = remember(trash.deletedAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(trash.deletedAt))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightCard, RoundedCornerShape(6.dp))
            .border(1.dp, AccentBorderLight, RoundedCornerShape(6.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(trash.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Table: ${trash.originalTable} | Deleted: $dateStr", fontSize = 10.sp, color = TextSecondaryDark)
        }
        Button(
            onClick = onRestore,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("restore_button_${trash.id}")
        ) {
            Icon(Icons.Default.Restore, contentDescription = "Restore item", modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Restore", fontSize = 11.sp, color = Color.White)
        }
    }
}

// Diagnostics list panel
@Composable
fun SpecsCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = LightCard),
        border = BorderStroke(1.dp, AccentBorderLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Diagnostic Specifications", fontWeight = FontWeight.Bold, color = Color.White)
            Text("• Encryption Algorithm: AES/GCM/NoPadding (AndroidKeyStore hardware backed)", fontSize = 12.sp, color = TextPrimaryDark)
            Text("• Local Database Engine: Room SQLCipher compatible SQLite standard", fontSize = 12.sp, color = TextPrimaryDark)
            Text("• Storage Policy: Local sandboxed memory folder with secure zero-fill deletions", fontSize = 12.sp, color = TextPrimaryDark)
            Text("• Network Operations: Prohibited (No INTERNET Manifest permissions requested)", fontSize = 12.sp, color = TextPrimaryDark)
            Text("• External Integration: Purely local share sheet input parsing with custom NLP heuristic engines", fontSize = 12.sp, color = TextPrimaryDark)
        }
    }
}
