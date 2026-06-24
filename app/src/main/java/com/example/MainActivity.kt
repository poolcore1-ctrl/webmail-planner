package com.example

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Bind SharedPreferences secure listeners
        sharedPrefs = getSharedPreferences("WorkMailPlannerPrefs", MODE_PRIVATE)
        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
        applyFlagSecure()

        // Check if app was opened via the Android Share Sheet Sheet
        handleShareSheetIntent(intent)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                var currentRoute by remember { mutableStateOf("timeline") }

                // Synchronize shared sheet intake nav transitions
                val intakeBufferResult by viewModel.extractionResult.collectAsState()
                LaunchedEffect(intakeBufferResult) {
                    if (intakeBufferResult != null && currentRoute != "intake") {
                        currentRoute = "intake"
                        navController.navigate("intake") {
                            popUpTo("timeline") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("main_bottom_nav")
                        ) {
                            val items = listOf(
                                NavigationItem("Timeline", "timeline", Icons.Default.CalendarMonth, "nav_timeline"),
                                NavigationItem("Intake", "intake", Icons.Default.Input, "nav_intake"),
                                NavigationItem("Graph", "graph", Icons.Default.Hub, "nav_graph"),
                                NavigationItem("Explorer", "explorer", Icons.Default.Folder, "nav_explorer"),
                                NavigationItem("Security", "security", Icons.Default.Shield, "nav_security")
                            )

                            items.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        currentRoute = item.route
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "timeline",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("timeline") {
                            TimelineScreen(viewModel)
                        }
                        composable("intake") {
                            IntakeScreen(viewModel)
                        }
                        composable("graph") {
                            GraphScreen(viewModel)
                        }
                        composable("explorer") {
                            ExplorerScreen(viewModel)
                        }
                        composable("security") {
                            SecurityScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareSheetIntent(intent)
    }

    private fun handleShareSheetIntent(intent: Intent?) {
        if (intent != null && intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.loadSharedText(sharedText)
                Toast.makeText(this, "Outlook email shared snippet loaded successfully!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun applyFlagSecure() {
        val flagSecure = sharedPrefs.getBoolean("FLAG_SECURE_ENABLED", false)
        if (flagSecure) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        if (key == "FLAG_SECURE_ENABLED") {
            applyFlagSecure()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this)
    }
}

data class NavigationItem(
    val label: String,
    val route: String,
    val icon: ImageVector,
    val testTag: String
)
