package com.bambu.nfc.data.firestore

import android.util.Log
import com.bambu.nfc.BuildConfig
import com.bambu.nfc.data.local.dao.SpoolDao
import com.bambu.nfc.data.local.entity.SpoolEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bambu.nfc.domain.model.SpoolStatus
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncs spool data between local Room database and Firestore.
 * Room is the local source of truth. Firestore provides cloud backup and cross-device sync.
 *
 * Firestore structure: users/{userId}/spools/{spoolId}
 */
@Singleton
class FirestoreSync @Inject constructor(
    private val dao: SpoolDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "FirestoreSync"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_SPOOLS = "spools"
    }

    private val userId: String?
        get() = auth.currentUser?.uid

    private fun spoolsCollection() =
        userId?.let { firestore.collection(COLLECTION_USERS).document(it).collection(COLLECTION_SPOOLS) }

    /**
     * Push a spool to Firestore after a local save/update.
     */
    suspend fun pushSpool(entity: SpoolEntity) {
        val collection = spoolsCollection() ?: run {
            if (BuildConfig.DEBUG) Log.d(TAG, "Not signed in, skipping push")
            return
        }
        try {
            val doc = collection.document(entity.id.toString())
            doc.set(entity.toMap(), SetOptions.merge()).await()
            if (BuildConfig.DEBUG) Log.d(TAG, "Pushed spool #${entity.id} to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push spool #${entity.id}", e)
        }
    }

    /**
     * Delete a spool from Firestore.
     */
    suspend fun deleteSpool(spoolId: Long) {
        val collection = spoolsCollection() ?: return
        try {
            collection.document(spoolId.toString()).delete().await()
            if (BuildConfig.DEBUG) Log.d(TAG, "Deleted spool #$spoolId from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete spool #$spoolId", e)
        }
    }

    /**
     * Pull all spools from Firestore into the local database.
     * Used on sign-in or app start to sync cloud data down.
     * Merges by ID: cloud spools that don't exist locally get inserted,
     * existing spools are updated if the cloud version is newer.
     */
    suspend fun pullAll() {
        val collection = spoolsCollection() ?: run {
            if (BuildConfig.DEBUG) Log.d(TAG, "Not signed in, skipping pull")
            return
        }
        try {
            val snapshot = collection.get().await()
            var pulled = 0
            for (doc in snapshot.documents) {
                val cloudSpool = doc.toSpoolEntity() ?: continue
                val local = dao.getById(cloudSpool.id)
                if (local == null) {
                    dao.insert(cloudSpool)
                    pulled++
                } else if (cloudSpool.dateLastScanned > local.dateLastScanned) {
                    dao.update(cloudSpool)
                    pulled++
                }
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "Pulled $pulled spools from Firestore (${snapshot.size()} total in cloud)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull from Firestore", e)
        }
    }

    /**
     * Push all local spools to Firestore.
     * Used after first sign-in to upload existing local data.
     */
    suspend fun pushAll() {
        val collection = spoolsCollection() ?: return
        try {
            val batch = firestore.batch()
            val allSpools = dao.getAll()
            for (spool in allSpools) {
                val doc = collection.document(spool.id.toString())
                batch.set(doc, spool.toMap(), SetOptions.merge())
            }
            batch.commit().await()
            if (BuildConfig.DEBUG) Log.d(TAG, "Pushed ${allSpools.size} spools to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push all to Firestore", e)
        }
    }

    /**
     * Delete all cloud spool data for the current user.
     * Used when the user requests account deletion.
     */
    suspend fun deleteAllCloud() {
        val collection = spoolsCollection() ?: return
        try {
            val snapshot = collection.get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            if (BuildConfig.DEBUG) Log.d(TAG, "Deleted ${snapshot.size()} spools from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all cloud data", e)
        }
    }

    /**
     * Listen for real-time changes from Firestore.
     * Returns a Flow that emits whenever cloud data changes.
     */
    fun observeChanges(): Flow<Unit> = callbackFlow {
        val collection = spoolsCollection()
        if (collection == null) {
            close()
            return@callbackFlow
        }

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore listener error", error)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                trySend(Unit)
            }
        }

        awaitClose { listener.remove() }
    }

    private fun SpoolEntity.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "tagUid" to tagUid,
        "trayUid" to trayUid,
        "materialId" to materialId,
        "filamentType" to filamentType,
        "detailedType" to detailedType,
        "colorRgba" to colorRgba,
        "weightGrams" to weightGrams,
        "diameterMm" to diameterMm.toDouble(),
        "dryingTempC" to dryingTempC,
        "dryingTimeHours" to dryingTimeHours,
        "bedTempC" to bedTempC,
        "maxHotendTempC" to maxHotendTempC,
        "minHotendTempC" to minHotendTempC,
        "productionDate" to productionDate,
        "lengthMeters" to lengthMeters,
        "status" to status,
        "dateAdded" to dateAdded,
        "dateLastScanned" to dateLastScanned,
        "notes" to notes
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toSpoolEntity(): SpoolEntity? {
        return try {
            SpoolEntity(
                id = getLong("id") ?: return null,
                tagUid = getString("tagUid") ?: "",
                trayUid = getString("trayUid") ?: "",
                materialId = getString("materialId") ?: "",
                filamentType = getString("filamentType") ?: "",
                detailedType = getString("detailedType") ?: "",
                colorRgba = getLong("colorRgba")?.toInt() ?: 0,
                weightGrams = getLong("weightGrams")?.toInt() ?: 0,
                diameterMm = getDouble("diameterMm")?.toFloat() ?: 0f,
                dryingTempC = getLong("dryingTempC")?.toInt() ?: 0,
                dryingTimeHours = getLong("dryingTimeHours")?.toInt() ?: 0,
                bedTempC = getLong("bedTempC")?.toInt() ?: 0,
                maxHotendTempC = getLong("maxHotendTempC")?.toInt() ?: 0,
                minHotendTempC = getLong("minHotendTempC")?.toInt() ?: 0,
                productionDate = getString("productionDate") ?: "",
                lengthMeters = getLong("lengthMeters")?.toInt() ?: 0,
                status = (getString("status") ?: "IN_STOCK").let { raw ->
                    if (SpoolStatus.entries.any { it.name == raw }) raw else "IN_STOCK"
                },
                dateAdded = getLong("dateAdded") ?: System.currentTimeMillis(),
                dateLastScanned = getLong("dateLastScanned") ?: System.currentTimeMillis(),
                notes = getString("notes") ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Firestore document ${id}", e)
            null
        }
    }
}
