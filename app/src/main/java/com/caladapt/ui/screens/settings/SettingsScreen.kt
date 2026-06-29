package com.caladapt.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caladapt.ui.components.GlassCard
import com.caladapt.ui.components.MeshBackground
import com.caladapt.domain.model.UnitSystem
import com.caladapt.ui.theme.AccentRed
import com.caladapt.ui.theme.AccentOrange
import com.caladapt.ui.theme.TextMain
import com.caladapt.ui.theme.TextSub

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onDataCleared: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Observe export events
    LaunchedEffect(viewModel) {
        viewModel.exportEvent.collect { files ->
            if (files.isNotEmpty()) {
                val uris = files.map { file ->
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "text/csv"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Export Data"))
            }
        }
    }

    LaunchedEffect(state.dataCleared) {
        if (state.dataCleared) {
            onDataCleared()
        }
    }

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = TextMain) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        MeshBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassCard {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = AccentRed)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Smart Notifications", color = TextMain, fontWeight = FontWeight.Bold)
                                Text(
                                    "Daily reminders & weekly review alerts",
                                    color = TextSub,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = state.notificationsEnabled,
                                onCheckedChange = { viewModel.toggleNotifications(it) }
                            )
                        }
                    }
                }

                // Unit Toggle
                GlassCard {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Straighten, contentDescription = null, tint = AccentRed)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Unit System", color = TextMain, fontWeight = FontWeight.Bold)
                                Text(
                                    if (state.currentUnitSystem == UnitSystem.METRIC) "Metric (kg, cm)" else "Imperial (lbs, in)",
                                    color = TextSub,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = state.currentUnitSystem == UnitSystem.METRIC,
                                onCheckedChange = { viewModel.toggleUnitSystem(it) },
                                thumbContent = if (state.currentUnitSystem == UnitSystem.METRIC) {
                                    { Text("M", style = MaterialTheme.typography.labelSmall) }
                                } else {
                                    { Text("I", style = MaterialTheme.typography.labelSmall) }
                                }
                            )
                        }
                    }
                }

                // Data Export
                GlassCard {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = AccentRed)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Export Data (CSV)", color = TextMain, fontWeight = FontWeight.Bold)
                                Text(
                                    "Download all weights, calories, and measurements",
                                    color = TextSub,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.exportData() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            enabled = !state.isExporting
                        ) {
                            Text(if (state.isExporting) "Exporting..." else "Export to CSV")
                        }
                    }
                }
                
                // About / How It Works
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AccentRed)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("How CalAdapt Works", color = TextMain, fontWeight = FontWeight.Bold)
                            Text("Learn about the algorithm", color = TextSub, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { showAboutDialog = true }) {
                            Text("Read", color = AccentRed)
                        }
                    }
                }

                // Danger Zone
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = AccentOrange)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear All Data", color = AccentOrange, fontWeight = FontWeight.Bold)
                            Text("This cannot be undone", color = TextSub, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { showClearDataDialog = true }) {
                            Text("Delete", color = AccentOrange)
                        }
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("How CalAdapt Works") },
            text = {
                Text(
                    "CalAdapt uses an Exponential Moving Average (EMA) to filter out daily water weight fluctuations and uncover your true body weight trend.\n\n" +
                    "By comparing this trend against the calories you consume, the app solves for your actual Total Daily Energy Expenditure (TDEE). " +
                    "Your daily targets are then dynamically adjusted every week to ensure you reach your goal weight optimally."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Got it", color = AccentRed)
                }
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data?") },
            text = { Text("Are you sure you want to delete all your weight logs, calorie logs, and profile data? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        viewModel.clearAllData()
                    }
                ) {
                    Text("Delete Everything", color = AccentOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
