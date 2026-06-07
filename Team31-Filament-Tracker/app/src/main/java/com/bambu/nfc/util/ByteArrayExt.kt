package com.bambu.nfc.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

fun ByteArray.toUInt16LE(offset: Int): Int {
    if (offset + 2 > size) return 0
    return (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8)
}

fun ByteArray.toDoubleLE(offset: Int): Double {
    if (offset + 8 > size) return 0.0
    val buf = ByteBuffer.wrap(this, offset, 8).order(ByteOrder.LITTLE_ENDIAN)
    return buf.double
}

fun ByteArray.toAsciiString(offset: Int, length: Int): String {
    val end = minOf(offset + length, this.size)
    return String(this, offset, end - offset, Charsets.US_ASCII).trimEnd('\u0000', ' ')
}

fun ByteArray.toHex(): String {
    return joinToString("") { "%02X".format(it) }
}

fun ByteArray.toRgbaInt(offset: Int): Int {
    if (offset + 4 > size) return 0
    val r = this[offset].toInt() and 0xFF
    val g = this[offset + 1].toInt() and 0xFF
    val b = this[offset + 2].toInt() and 0xFF
    val a = this[offset + 3].toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
