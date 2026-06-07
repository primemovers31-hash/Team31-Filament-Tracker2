package com.bambu.nfc.data.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import com.bambu.nfc.BuildConfig
import com.bambu.nfc.util.toHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BambuTagReader @Inject constructor() {

    companion object {
        private const val TAG = "BambuTagReader"
    }

    sealed class ReadResult {
        data class Success(val uid: ByteArray, val blocks: Map<Int, ByteArray>) : ReadResult()
        data class Error(val message: String) : ReadResult()
    }

    suspend fun readTag(tag: Tag): ReadResult = withContext(Dispatchers.IO) {
        val techList = tag.techList.joinToString(", ")
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Tag tech list: $techList")
            Log.d(TAG, "Tag UID: ${tag.id.toHex()} (${tag.id.size} bytes)")
        }

        val mifare = MifareClassic.get(tag)
        if (mifare == null) {
            return@withContext ReadResult.Error(
                "Your device does not support MIFARE Classic.\n\n" +
                "Tag technologies: $techList\n" +
                "UID: ${tag.id.toHex()}\n\n" +
                "This typically happens on phones with Broadcom NFC chips (e.g. Google Pixel). " +
                "Phones with NXP NFC chips (many Samsung, OnePlus, Xiaomi) usually work."
            )
        }

        try {
            mifare.connect()
            val uid = tag.id
            if (BuildConfig.DEBUG) Log.d(TAG, "Connected. Type: ${mifare.type}, Sectors: ${mifare.sectorCount}, Size: ${mifare.size}")

            val blocks = mutableMapOf<Int, ByteArray>()
            var authFailures = 0

            for (sector in 0 until mifare.sectorCount) {
                val key = KeyDerivation.deriveKeyForSector(uid, sector)

                val authenticated = try {
                    mifare.authenticateSectorWithKeyA(sector, key)
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.w(TAG, "Auth exception sector $sector: ${e.message}")
                    false
                }

                if (!authenticated) {
                    if (BuildConfig.DEBUG) Log.w(TAG, "Auth failed for sector $sector")
                    authFailures++
                    continue
                }

                if (BuildConfig.DEBUG) Log.d(TAG, "Sector $sector authenticated OK")
                val firstBlock = mifare.sectorToBlock(sector)
                val blockCount = mifare.getBlockCountInSector(sector)

                for (blockOffset in 0 until blockCount - 1) {
                    val blockIndex = firstBlock + blockOffset
                    try {
                        val data = mifare.readBlock(blockIndex)
                        blocks[blockIndex] = data
                        if (BuildConfig.DEBUG) Log.d(TAG, "Block $blockIndex: ${data.toHex()}")
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) Log.w(TAG, "Failed to read block $blockIndex: ${e.message}")
                    }
                }
            }

            if (blocks.isEmpty()) {
                ReadResult.Error(
                    "Could not authenticate any sector.\n\n" +
                    "UID: ${uid.toHex()} (${uid.size} bytes)\n" +
                    "Sectors attempted: ${mifare.sectorCount}\n" +
                    "Auth failures: $authFailures\n\n" +
                    "The tag may not be a Bambu Lab spool, or the key derivation may not work for this tag revision."
                )
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "Successfully read ${blocks.size} blocks")
                ReadResult.Success(uid, blocks)
            }
        } catch (e: android.nfc.TagLostException) {
            ReadResult.Error("Tag lost. Hold your phone steady against the spool and try again.")
        } catch (e: java.io.IOException) {
            ReadResult.Error("Communication error: ${e.message}")
        } catch (e: Exception) {
            ReadResult.Error("Unexpected error: ${e.javaClass.simpleName}: ${e.message}")
        } finally {
            try {
                mifare.close()
            } catch (_: Exception) {
            }
        }
    }
}
