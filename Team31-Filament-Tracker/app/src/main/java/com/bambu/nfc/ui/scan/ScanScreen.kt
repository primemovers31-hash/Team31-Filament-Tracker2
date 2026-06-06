package com.bambu.nfc.ui.scan

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bambu.nfc.domain.model.FilamentData
import com.bambu.nfc.domain.model.Spool
import com.bambu.nfc.domain.model.SpoolStatus
import com.bambu.nfc.ui.components.ColorSwatch
import com.bambu.nfc.ui.components.TemperatureInfo
import com.bambu.nfc.util.toHexColorString

@Composable
fun ScanScreen(viewModel: ScanViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState) {
                is ScanViewModel.UiState.Ready -> ScanPrompt()
                is ScanViewModel.UiState.Scanning -> ScanningIndicator()
                is ScanViewModel.UiState.Error -> {
                    ScanPrompt()
                    Spacer(Modifier.height(24.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = (uiState as ScanViewModel.UiState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is ScanViewModel.UiState.ChooseAction -> ScanPrompt()
                is ScanViewModel.UiState.Done -> ScanPrompt()
            }
        }

        val state = uiState
        if (state is ScanViewModel.UiState.ChooseAction) {
            ActionChooserSheet(
                filament = state.filament,
                existingSpool = state.existingSpool,
                viewModel = viewModel,
                onDismiss = { viewModel.dismiss() }
            )
        }

        if (state is ScanViewModel.UiState.Done) {
            DoneSheet(
                spool = state.spool,
                message = state.message,
                onDismiss = { viewModel.dismiss() }
            )
        }
    }
}

@Composable
private fun ScanPrompt() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Icon(
        imageVector = Icons.Default.Nfc,
        contentDescription = "NFC",
        modifier = Modifier
            .size(120.dp)
            .alpha(alpha),
        tint = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(24.dp))
    Text(
        text = "Hold phone to spool",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Place your phone against the NFC tag on the filament spool",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ScanningIndicator() {
    CircularProgressIndicator(modifier = Modifier.size(64.dp))
    Spacer(Modifier.height(24.dp))
    Text(
        text = "Reading spool...",
        style = MaterialTheme.typography.headlineMedium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionChooserSheet(
    filament: FilamentData,
    existingSpool: Spool?,
    viewModel: ScanViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Filament identity
            ColorSwatch(colorRgba = filament.colorRgba, size = 72.dp)
            Spacer(Modifier.height(12.dp))
            Text(filament.detailedType, style = MaterialTheme.typography.headlineMedium)
            Text(
                "${filament.filamentType}  |  ${filament.colorRgba.toHexColorString()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (filament.maxHotendTempC > 0) {
                Spacer(Modifier.height(8.dp))
                TemperatureInfo(
                    minHotend = filament.minHotendTempC,
                    maxHotend = filament.maxHotendTempC,
                    bedTemp = filament.bedTempC
                )
            }

            Spacer(Modifier.height(20.dp))

            if (existingSpool != null) {
                // Existing spool — show current status and relevant actions
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "#%03d  |  ${existingSpool.status.displayName()}".format(existingSpool.id),
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (existingSpool.filament.productionDate.isNotBlank()) {
                            Text(
                                "Produced: ${existingSpool.filament.productionDate}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                when (existingSpool.status) {
                    SpoolStatus.IN_STOCK -> {
                        // Can: mark as in use, or remove (empty spool)
                        Button(
                            onClick = { viewModel.markInUse(existingSpool) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Put in printer")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.removeFromInventory(existingSpool) },
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
                        // Can: mark not in use (back to stock), or remove (finished)
                        Button(
                            onClick = { viewModel.markNotInUse(existingSpool) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Stop, null, Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Remove from printer")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.removeFromInventory(existingSpool) },
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
                        // Shouldn't normally happen (used up spools are filtered out)
                        Text(
                            "This spool was already removed from inventory",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // New spool — only option is to add
                Text(
                    "New spool",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "This spool is not in your inventory yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.addToInventory(filament) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Add to inventory")
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoneSheet(
    spool: Spool,
    message: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(16.dp))

            ColorSwatch(colorRgba = spool.filament.colorRgba, size = 56.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                "#%03d  %s".format(spool.id, spool.filament.detailedType),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                spool.status.displayName(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
