package com.bambu.nfc.domain.model

data class Spool(
    val id: Long = 0,
    val filament: FilamentData,
    val status: SpoolStatus = SpoolStatus.IN_STOCK,
    val dateAdded: Long = System.currentTimeMillis(),
    val dateLastScanned: Long = System.currentTimeMillis(),
    val notes: String = ""
)
