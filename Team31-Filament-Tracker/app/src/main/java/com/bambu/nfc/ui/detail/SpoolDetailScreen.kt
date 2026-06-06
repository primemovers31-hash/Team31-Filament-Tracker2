package com.bambu.nfc.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bambu.nfc.domain.model.SpoolStatus
import com.bambu.nfc.ui.components.StatusBadge
import com.bambu.nfc.ui.components.TemperatureInfo
import com.bambu.nfc.util.toComposeColor
import com.bambu.nfc.util.toHexColorString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolDetailScreen(
    viewModel: SpoolDetailViewModel,
    onBack: () -> Unit
) {
    val spool by viewModel.spool.collectAsState()
    val navigateBack by viewModel.navigateBack.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(navigateBack) {
        if (navigateBack) onBack()
    }

    val currentSpool = spool ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("#%03d  %s".format(currentSpool.id, currentSpool.filament.detailedType))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Color banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(currentSpool.filament.colorRgba.toComposeColor())
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type + status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            currentSpool.filament.detailedType,
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            currentSpool.filament.filamentType,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadge(status = currentSpool.status)
                }

                // Actions based on current status
                when (currentSpool.status) {
                    SpoolStatus.IN_STOCK -> {
                        Button(
                            onClick = { viewModel.markInUse() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Put in printer")
                        }
                        OutlinedButton(
                            onClick = { viewModel.markUsedUp() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, null, Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Spool is empty / remove")
                        }
                    }
                    SpoolStatus.IN_USE -> {
                        Button(
                            onClick = { viewModel.markNotInUse() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Stop, null, Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Remove from printer")
                        }
                        OutlinedButton(
                            onClick = { viewModel.markUsedUp() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, null, Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Spool is empty / remove")
                        }
                    }
                    SpoolStatus.USED_UP -> {
                        Text(
                            "This spool has been used up",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Details
                DetailRow("ID", "#%03d".format(currentSpool.id))
                DetailRow("Tag UID", currentSpool.filament.tagUid)
                DetailRow("Color", currentSpool.filament.colorRgba.toHexColorString())
                if (currentSpool.filament.weightGrams > 0)
                    DetailRow("Weight", "${currentSpool.filament.weightGrams}g")
                if (currentSpool.filament.diameterMm > 0)
                    DetailRow("Diameter", "${currentSpool.filament.diameterMm}mm")
                if (currentSpool.filament.lengthMeters > 0)
                    DetailRow("Length", "${currentSpool.filament.lengthMeters}m")
                if (currentSpool.filament.productionDate.isNotBlank())
                    DetailRow("Produced", currentSpool.filament.productionDate)

                if (currentSpool.filament.maxHotendTempC > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Print Settings", style = MaterialTheme.typography.titleLarge)
                    TemperatureInfo(
                        minHotend = currentSpool.filament.minHotendTempC,
                        maxHotend = currentSpool.filament.maxHotendTempC,
                        bedTemp = currentSpool.filament.bedTempC
                    )
                    if (currentSpool.filament.dryingTempC > 0) {
                        DetailRow(
                            "Drying",
                            "${currentSpool.filament.dryingTempC}\u00B0C for ${currentSpool.filament.dryingTimeHours}h"
                        )
                    }
                }

                // Notes
                Spacer(Modifier.height(8.dp))
                var notesText by remember(currentSpool.notes) {
                    mutableStateOf(currentSpool.notes)
                }
                OutlinedTextField(
                    value = notesText,
                    onValueChange = {
                        notesText = it
                        viewModel.updateNotes(it)
                    },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete permanently?") },
            text = { Text("This will completely remove #%03d %s from the database, including history.".format(currentSpool.id, currentSpool.filament.detailedType)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
