package com.bambu.nfc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bambu.nfc.util.toComposeColor

@Composable
fun ColorSwatch(
    colorRgba: Int,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val color = colorRgba.toComposeColor()
    val needsBorder = color.luminance() > 0.85f || color.luminance() < 0.05f

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .then(
                if (needsBorder) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                } else Modifier
            )
    )
}
