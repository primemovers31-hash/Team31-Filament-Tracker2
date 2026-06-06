package com.bambu.nfc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spools")
data class SpoolEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val lengthMeters: Int,
    val status: String,
    val dateAdded: Long,
    val dateLastScanned: Long,
    val notes: String
)
