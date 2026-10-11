package com.browxer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DownloadEntity::class], version = 1, exportSchema = false)
abstract class BrowxerDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
