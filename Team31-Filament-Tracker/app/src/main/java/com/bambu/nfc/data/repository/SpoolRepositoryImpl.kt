package com.bambu.nfc.data.repository

import com.bambu.nfc.data.firestore.FirestoreSync
import com.bambu.nfc.data.local.dao.SpoolDao
import com.bambu.nfc.data.local.entity.SpoolEntity
import com.bambu.nfc.domain.model.FilamentData
import com.bambu.nfc.domain.model.Spool
import com.bambu.nfc.domain.model.SpoolStatus
import com.bambu.nfc.domain.repository.SpoolRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpoolRepositoryImpl @Inject constructor(
    private val dao: SpoolDao,
    private val firestoreSync: FirestoreSync
) : SpoolRepository {

    override fun getActive(): Flow<List<Spool>> =
        dao.getActive().map { list -> list.map { it.toDomain() } }

    override fun getUsedUp(): Flow<List<Spool>> =
        dao.getUsedUp().map { list -> list.map { it.toDomain() } }

    override fun search(query: String): Flow<List<Spool>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun findActiveByTrayUid(trayUid: String): Spool? =
        dao.findActiveByTrayUid(trayUid)?.toDomain()

    override suspend fun getById(id: Long): Spool? =
        dao.getById(id)?.toDomain()

    override suspend fun save(spool: Spool): Long {
        val entity = spool.toEntity()
        val id = dao.insert(entity)
        firestoreSync.pushSpool(entity.copy(id = id))
        return id
    }

    override suspend fun update(spool: Spool) {
        val entity = spool.toEntity()
        dao.update(entity)
        firestoreSync.pushSpool(entity)
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
        firestoreSync.deleteSpool(id)
    }

    private fun SpoolEntity.toDomain() = Spool(
        id = id,
        filament = FilamentData(
            tagUid = tagUid,
            trayUid = trayUid,
            materialId = materialId,
            filamentType = filamentType,
            detailedType = detailedType,
            colorRgba = colorRgba,
            weightGrams = weightGrams,
            diameterMm = diameterMm,
            dryingTempC = dryingTempC,
            dryingTimeHours = dryingTimeHours,
            bedTempC = bedTempC,
            maxHotendTempC = maxHotendTempC,
            minHotendTempC = minHotendTempC,
            productionDate = productionDate,
            lengthMeters = lengthMeters
        ),
        status = SpoolStatus.valueOf(status),
        dateAdded = dateAdded,
        dateLastScanned = dateLastScanned,
        notes = notes
    )

    private fun Spool.toEntity() = SpoolEntity(
        id = id,
        tagUid = filament.tagUid,
        trayUid = filament.trayUid,
        materialId = filament.materialId,
        filamentType = filament.filamentType,
        detailedType = filament.detailedType,
        colorRgba = filament.colorRgba,
        weightGrams = filament.weightGrams,
        diameterMm = filament.diameterMm,
        dryingTempC = filament.dryingTempC,
        dryingTimeHours = filament.dryingTimeHours,
        bedTempC = filament.bedTempC,
        maxHotendTempC = filament.maxHotendTempC,
        minHotendTempC = filament.minHotendTempC,
        productionDate = filament.productionDate,
        lengthMeters = filament.lengthMeters,
        status = status.name,
        dateAdded = dateAdded,
        dateLastScanned = dateLastScanned,
        notes = notes
    )
}
