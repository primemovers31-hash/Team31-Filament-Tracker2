package com.bambu.nfc.domain.model

enum class SpoolStatus {
    IN_STOCK,
    IN_USE,
    USED_UP;

    fun displayName(): String = when (this) {
        IN_STOCK -> "In Stock"
        IN_USE -> "In Use"
        USED_UP -> "Used Up"
    }
}
