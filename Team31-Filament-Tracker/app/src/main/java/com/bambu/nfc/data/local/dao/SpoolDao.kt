package com.bambu.nfc.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bambu.nfc.data.local.entity.SpoolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpoolDao {

    @Query("SELECT * FROM spools WHERE status != 'USED_UP' ORDER BY dateLastScanned DESC")
    fun getActive(): Flow<List<SpoolEntity>>

    @Query("SELECT * FROM spools WHERE status = 'USED_UP' ORDER BY dateLastScanned DESC")
    fun getUsedUp(): Flow<List<SpoolEntity>>

    @Query(
        "SELECT * FROM spools WHERE (filamentType LIKE '%' || :query || '%' " +
        "OR detailedType LIKE '%' || :query || '%' " +
        "OR notes LIKE '%' || :query || '%') " +
        "AND status != 'USED_UP' " +
        "ORDER BY dateLastScanned DESC"
    )
    fun search(query: String): Flow<List<SpoolEntity>>

    @Query("SELECT * FROM spools WHERE trayUid = :trayUid AND status != 'USED_UP' LIMIT 1")
    suspend fun findActiveByTrayUid(trayUid: String): SpoolEntity?

    @Query("SELECT * FROM spools WHERE id = :id")
    suspend fun getById(id: Long): SpoolEntity?

    @Query("SELECT * FROM spools")
    suspend fun getAll(): List<SpoolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spool: SpoolEntity): Long

    @Update
    suspend fun update(spool: SpoolEntity)

    @Query("DELETE FROM spools WHERE id = :id")
    suspend fun deleteById(id: Long)
}
