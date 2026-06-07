package com.bambu.nfc.data.nfc

import android.util.Log
import com.bambu.nfc.BuildConfig
import com.bambu.nfc.domain.model.FilamentData
import com.bambu.nfc.util.toAsciiString
import com.bambu.nfc.util.toDoubleLE
import com.bambu.nfc.util.toHex
import com.bambu.nfc.util.toRgbaInt
import com.bambu.nfc.util.toUInt16LE
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BambuTagParser @Inject constructor() {

    companion object {
        private const val TAG = "BambuTagParser"
    }

    fun parse(uid: ByteArray, blocks: Map<Int, ByteArray>): FilamentData {
        val tagUid = uid.toHex()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Parsing tag UID=$tagUid with ${blocks.size} blocks")
            blocks.forEach { (k, v) -> Log.d(TAG, "  Block $k: ${v.toHex()}") }
        }

        // Block 1 (sector 0, block 1): Material variant + material ID
        val materialId = blocks[1]?.toAsciiString(8, 8) ?: ""

        // Block 2 (sector 0, block 2): Filament type string
        val filamentType = blocks[2]?.toAsciiString(0, 16) ?: "Unknown"

        // Block 4 (sector 1, block 0): Detailed filament type
        val detailedType = blocks[4]?.toAsciiString(0, 16) ?: filamentType

        // Block 5 (sector 1, block 1): Color RGBA (4b) + weight uint16 LE (2b) + diameter float64 LE (8b)
        val block5 = blocks[5]
        val colorRgba = block5?.toRgbaInt(0) ?: 0xFF808080.toInt()
        val weightGrams = block5?.toUInt16LE(4) ?: 0
        val diameterMm = if (block5 != null && block5.size >= 14) {
            block5.toDoubleLE(6).toFloat()
        } else {
            1.75f
        }

        // Block 6 (sector 1, block 2): Temperatures (all uint16 LE)
        // 0-1: drying temp, 2-3: drying time, 4-5: bed temp type,
        // 6-7: bed temp, 8-9: max hotend, 10-11: min hotend
        val block6 = blocks[6]
        val dryingTempC = block6?.toUInt16LE(0) ?: 0
        val dryingTimeHours = block6?.toUInt16LE(2) ?: 0
        val bedTempC = block6?.toUInt16LE(6) ?: 0
        val maxHotendTempC = block6?.toUInt16LE(8) ?: 0
        val minHotendTempC = block6?.toUInt16LE(10) ?: 0

        // Block 9 (sector 2, block 1): Tray UID
        val trayUid = blocks[9]?.toAsciiString(0, 16) ?: tagUid

        // Block 12 (sector 3, block 0): Production date "YYYY_MM_DD_HH_MM"
        val productionDate = blocks[12]?.toAsciiString(0, 16) ?: ""

        // Block 14 (sector 3, block 2): Filament length at offset 4
        val lengthMeters = blocks[14]?.toUInt16LE(4) ?: 0

        val result = FilamentData(
            tagUid = tagUid,
            trayUid = trayUid,
            materialId = materialId,
            filamentType = extractBaseType(filamentType),
            detailedType = detailedType.ifBlank { filamentType },
            colorRgba = colorRgba,
            weightGrams = weightGrams,
            diameterMm = diameterMm,
            dryingTempC = dryingTempC,
            dryingTimeHours = dryingTimeHours,
            bedTempC = bedTempC,
            maxHotendTempC = maxHotendTempC,
            minHotendTempC = minHotendTempC,
            productionDate = formatDate(productionDate),
            lengthMeters = lengthMeters
        )

        if (BuildConfig.DEBUG) Log.d(TAG, "Parsed: type=${result.detailedType}, color=${result.colorRgba}, weight=${result.weightGrams}g")
        return result
    }

    private fun extractBaseType(type: String): String {
        val upper = type.uppercase().trim()
        return when {
            "PLA" in upper -> "PLA"
            "PETG" in upper -> "PETG"
            "ABS" in upper -> "ABS"
            "TPU" in upper -> "TPU"
            "ASA" in upper -> "ASA"
            "PA" in upper -> "PA"
            "PC" in upper -> "PC"
            "PVA" in upper -> "PVA"
            else -> type.trim()
        }
    }

    private fun formatDate(raw: String): String {
        val parts = raw.split("_")
        return if (parts.size >= 3) {
            "${parts[0]}-${parts[1]}-${parts[2]}"
        } else {
            raw
        }
    }
}
