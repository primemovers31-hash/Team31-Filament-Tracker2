package com.bambu.nfc.domain.repository

import com.bambu.nfc.domain.model.Spool
import kotlinx.coroutines.flow.Flow

interface SpoolRepository {
    fun getActive(): Flow<List<Spool>>
    fun getUsedUp(): Flow<List<Spool>>
    fun search(query: String): Flow<List<Spool>>
    suspend fun findActiveByTrayUid(trayUid: String): Spool?
    suspend fun getById(id: Long): Spool?
    suspend fun save(spool: Spool): Long
    suspend fun update(spool: Spool)
    suspend fun delete(id: Long)
}
