package com.bambu.nfc.data.nfc

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object KeyDerivation {

    private val SALT = hexToBytes("9a759cf2c4f7caff222cb9769b41bc96")
    private val INFO = "RFID-A\u0000".toByteArray(Charsets.US_ASCII)
    private const val MIFARE_KEY_LENGTH = 6
    private const val NUM_SECTORS = 16

    private var cachedUid: ByteArray? = null
    private var cachedKeys: List<ByteArray>? = null

    fun deriveKeyForSector(uid: ByteArray, sector: Int): ByteArray {
        val uid4 = if (uid.size > 4) uid.copyOf(4) else uid

        val cached = cachedKeys
        if (cached != null && cachedUid?.contentEquals(uid4) == true) {
            return cached[sector]
        }

        val keys = deriveAllKeys(uid4)
        cachedUid = uid4.clone()
        cachedKeys = keys
        return keys[sector]
    }

    private fun deriveAllKeys(uid: ByteArray): List<ByteArray> {
        val prk = hkdfExtract(SALT, uid)
        val totalLength = MIFARE_KEY_LENGTH * NUM_SECTORS // 96 bytes
        val okm = hkdfExpand(prk, INFO, totalLength)

        return (0 until NUM_SECTORS).map { i ->
            okm.copyOfRange(i * MIFARE_KEY_LENGTH, (i + 1) * MIFARE_KEY_LENGTH)
        }
    }

    private fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        return mac.doFinal(ikm)
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        val hashLen = 32 // SHA-256 output
        val n = (length + hashLen - 1) / hashLen
        val okm = ByteArray(n * hashLen)
        var prev = ByteArray(0)

        for (i in 1..n) {
            mac.reset()
            mac.update(prev)
            mac.update(info)
            mac.update(byteArrayOf(i.toByte()))
            prev = mac.doFinal()
            System.arraycopy(prev, 0, okm, (i - 1) * hashLen, hashLen)
        }

        return okm.copyOf(length)
    }

    private fun hexToBytes(hex: String): ByteArray {
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
