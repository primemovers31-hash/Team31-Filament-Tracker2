package com.bambu.nfc.domain.usecase

import com.bambu.nfc.domain.model.FilamentData
import com.bambu.nfc.domain.model.Spool
import com.bambu.nfc.domain.model.SpoolStatus
import com.bambu.nfc.domain.repository.SpoolRepository
import javax.inject.Inject

class ProcessScannedTagUseCase @Inject constructor(
    private val repository: SpoolRepository
) {
    data class ScanInfo(
        val filament: FilamentData,
        val existingSpool: Spool?
    )

    suspend fun lookup(filament: FilamentData): ScanInfo {
        val existing = repository.findActiveByTrayUid(filament.trayUid)
        return ScanInfo(filament, existing)
    }

    suspend fun addToInventory(filament: FilamentData): Spool {
        val spool = Spool(filament = filament, status = SpoolStatus.IN_STOCK)
        val id = repository.save(spool)
        return spool.copy(id = id)
    }

    suspend fun markInUse(spool: Spool): Spool {
        val updated = spool.copy(
            status = SpoolStatus.IN_USE,
            dateLastScanned = System.currentTimeMillis()
        )
        repository.update(updated)
        return updated
    }

    suspend fun markNotInUse(spool: Spool): Spool {
        val updated = spool.copy(
            status = SpoolStatus.IN_STOCK,
            dateLastScanned = System.currentTimeMillis()
        )
        repository.update(updated)
        return updated
    }

    suspend fun removeFromInventory(spool: Spool): Spool {
        val updated = spool.copy(
            status = SpoolStatus.USED_UP,
            dateLastScanned = System.currentTimeMillis()
        )
        repository.update(updated)
        return updated
    }
}
