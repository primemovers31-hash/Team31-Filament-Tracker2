package com.bambu.nfc.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bambu.nfc.domain.model.SpoolStatus

@Composable
fun StatusBadge(status: SpoolStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        SpoolStatus.IN_STOCK -> Color(0xFF4CAF50) to Color.White
        SpoolStatus.IN_USE -> Color(0xFF2196F3) to Color.White
        SpoolStatus.USED_UP -> Color(0xFF9E9E9E) to Color.White
    }

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = status.displayName(),
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
