package com.bambu.nfc.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TemperatureInfo(
    minHotend: Int,
    maxHotend: Int,
    bedTemp: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("Nozzle", style = MaterialTheme.typography.labelLarge)
            Text(
                "${minHotend}-${maxHotend}\u00B0C",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Column {
            Text("Bed", style = MaterialTheme.typography.labelLarge)
            Text(
                "${bedTemp}\u00B0C",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
