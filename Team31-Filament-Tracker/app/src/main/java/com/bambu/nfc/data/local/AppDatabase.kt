package com.bambu.nfc.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bambu.nfc.data.local.dao.SpoolDao
import com.bambu.nfc.data.local.entity.SpoolEntity

@Database(entities = [SpoolEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spoolDao(): SpoolDao
}
