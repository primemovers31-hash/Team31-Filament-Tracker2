package com.bambu.nfc.util

import androidx.compose.ui.graphics.Color

fun Int.toComposeColor(): Color {
    return Color(this)
}

fun Int.toHexColorString(): String {
    val r = (this shr 16) and 0xFF
    val g = (this shr 8) and 0xFF
    val b = this and 0xFF
    return "#%02X%02X%02X".format(r, g, b)
}
