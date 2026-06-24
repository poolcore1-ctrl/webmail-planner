package com.example.ui.screens

import android.graphics.PointF
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GraphEdge
import com.example.data.GraphNode
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val nodes by viewModel.graphNodes.collectAsState()
    val edges by viewModel.graphEdges.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var focusedNodeId by remember { mutableStateOf<String?>(null) }

    // Filter nodes based on search query
    val filteredNodes = remember(nodes, searchQuery) {
        if (searchQuery.isBlank()) {
            nodes
        } else {
            nodes.filter { it.label.lowercase().contains(searchQuery.lowercase()) || it.type.lowercase().contains(searchQuery.lowercase()) }
        }
    }

    // Determine current focal node
    val activeFocusNode = remember(filteredNodes, focusedNodeId) {
        val match = filteredNodes.find { it.id == focusedNodeId }
        match ?: filteredNodes.firstOrNull()
    }

    // Connected neighbors of focused node
    val connectedEdges = remember(edges, activeFocusNode) {
        if (activeFocusNode == null) emptyList()
        else edges.filter { it.fromNodeId == activeFocusNode.id || it.toNodeId == activeFocusNode.id }
    }

    val connectedNodes = remember(filteredNodes, connectedEdges, activeFocusNode) {
        if (activeFocusNode == null) emptyList()
        else {
            val ids = connectedEdges.flatMap { listOf(it.fromNodeId, it.toNodeId) }.distinct()
            filteredNodes.filter { it.id in ids && it.id != activeFocusNode.id }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title block
        Column {
            Text(
                text = "Work Knowledge Graph",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Interactive visual relationship graph (local-only)",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Project graph, Person graph, or Tasks...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
            modifier = Modifier.fillMaxWidth().testTag("graph_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CorporateIndigo,
                unfocusedBorderColor = AccentBorder
            ),
            singleLine = true
        )

        // Focused Node Overview
        activeFocusNode?.let { node ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepSlateCard),
                border = BorderStroke(1.dp, CorporateIndigo.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Focused Node: ${node.label}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Type: ${node.type} | Connections: ${connectedNodes.size}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = {
                            focusedNodeId = null
                            searchQuery = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Reset Focus", fontSize = 11.sp)
                    }
                }
            }
        }

        // Custom Visual Canvas for Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, AccentBorder, RoundedCornerShape(12.dp))
                .background(DeepSlateSurface)
                .testTag("interactive_graph_canvas")
        ) {
            if (activeFocusNode == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Hub, contentDescription = "Empty graph", tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No items on timeline to map graph", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                // Precompute focal coordinates for math layout
                val nodePositions = remember(activeFocusNode, connectedNodes) {
                    val map = mutableMapOf<String, PointF>()
                    // Place center focal node
                    map[activeFocusNode.id] = PointF(300f, 300f) // Normalized local canvas points

                    // Place surrounding connected nodes radially
                    val count = connectedNodes.size
                    if (count > 0) {
                        val angleStep = 360.0 / count
                        val radius = 180f
                        connectedNodes.forEachIndexed { index, neighbor ->
                            val radians = Math.toRadians(index * angleStep)
                            val x = 300f + (radius * cos(radians)).toFloat()
                            val y = 300f + (radius * sin(radians)).toFloat()
                            map[neighbor.id] = PointF(x, y)
                        }
                    }
                    map
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activeFocusNode, connectedNodes) {
                            detectTapGestures { offset ->
                                // Detect clicking nodes on canvas
                                val scaleX = size.width / 600f
                                val scaleY = size.height / 600f

                                var clickedId: String? = null
                                for ((id, pos) in nodePositions) {
                                    val actualX = pos.x * scaleX
                                    val actualY = pos.y * scaleY
                                    val dist = Math.hypot((offset.x - actualX).toDouble(), (offset.y - actualY).toDouble())
                                    if (dist <= 40 * scaleX) {
                                        clickedId = id
                                        break
                                    }
                                }

                                if (clickedId != null) {
                                    focusedNodeId = clickedId
                                    val label = nodes.find { it.id == clickedId }?.label ?: ""
                                    Toast.makeText(context, "Focused on: $label", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    
                    val scaleX = w / 600f
                    val scaleY = h / 600f

                    // Draw connection edges (lines)
                    connectedEdges.forEach { edge ->
                        val pStart = nodePositions[edge.fromNodeId]
                        val pEnd = nodePositions[edge.toNodeId]

                        if (pStart != null && pEnd != null) {
                            val startX = pStart.x * scaleX
                            val startY = pStart.y * scaleY
                            val endX = pEnd.x * scaleX
                            val endY = pEnd.y * scaleY

                            // Draw line
                            drawLine(
                                color = TextSecondary.copy(alpha = 0.5f),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 2f * scaleX
                            )

                            // Relation Label
                            val midX = (startX + endX) / 2
                            val midY = (startY + endY) / 2
                            drawContext.canvas.nativeCanvas.drawText(
                                edge.relationType,
                                midX,
                                midY - 6f,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 10f * scaleX
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                            )
                        }
                    }

                    // Draw node circles
                    nodePositions.forEach { (id, pos) ->
                        val node = nodes.find { it.id == id } ?: return@forEach
                        val cx = pos.x * scaleX
                        val cy = pos.y * scaleY

                        val isFocal = id == activeFocusNode.id

                        val nodeColor = when (node.type) {
                            "PERSON" -> CorporateEmerald
                            "PROJECT" -> CorporateAmber
                            "DATE" -> CorporateIndigo
                            "MEETING" -> CorporateIndigo
                            "TASK" -> CorporateEmerald
                            "DEADLINE" -> CorporateRuby
                            else -> TextSecondary
                        }

                        // Outer highlight glow if focused
                        if (isFocal) {
                            drawCircle(
                                color = Color.White,
                                radius = 28f * scaleX,
                                center = Offset(cx, cy),
                                style = Stroke(width = 2f * scaleX)
                            )
                        }

                        // Fill circle
                        drawCircle(
                            color = nodeColor,
                            radius = 22f * scaleX,
                            center = Offset(cx, cy)
                        )

                        // Inside Label initials
                        val labelInitials = if (node.label.length >= 2) node.label.take(2).uppercase() else node.label
                        drawContext.canvas.nativeCanvas.drawText(
                            labelInitials,
                            cx,
                            cy + (5f * scaleY),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 11f * scaleX
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )

                        // Full label drawn under node
                        drawContext.canvas.nativeCanvas.drawText(
                            if (node.label.length > 15) node.label.take(13) + ".." else node.label,
                            cx,
                            cy + (40f * scaleY),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 10f * scaleX
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }

        // Horizontal Quick-scroller lists for nodes
        Text(
            text = "Knowledge Nodes Registry",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredNodes) { node ->
                val isFocused = node.id == activeFocusNode?.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFocused) CorporateIndigo else DeepSlateCard)
                        .border(1.dp, AccentBorder, RoundedCornerShape(8.dp))
                        .clickable { focusedNodeId = node.id }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = node.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = node.type,
                            fontSize = 10.sp,
                            color = if (isFocused) Color.White.copy(alpha = 0.8f) else TextSecondary
                        )
                    }
                }
            }
        }
    }
}
