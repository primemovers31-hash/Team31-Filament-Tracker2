package com.bambu.nfc.domain.model

data class FilamentData(
    val tagUid: String,
    val trayUid: String,
    val materialId: String,
    val filamentType: String,
    val detailedType: String,
    val colorRgba: Int,
    val weightGrams: Int,
    val diameterMm: Float,
    val dryingTempC: Int,
    val dryingTimeHours: Int,
    val bedTempC: Int,
    val maxHotendTempC: Int,
    val minHotendTempC: Int,
    val productionDate: String,
    val lengthMeters: Int
)
