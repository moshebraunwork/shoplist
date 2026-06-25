package com.mobrauntech.shoplist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ItemEntity::class, SectionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun sectionDao(): SectionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shoplist.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}

/** Tracks the sync watermark (max updatedAt the device has applied from the server). */
class SyncPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("sync", Context.MODE_PRIVATE)
    var since: Long
        get() = prefs.getLong("since", 0L)
        set(value) { prefs.edit().putLong("since", value).apply() }
}
