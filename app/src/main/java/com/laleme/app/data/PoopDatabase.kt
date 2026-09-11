package com.laleme.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PoopEntry::class], version = 1, exportSchema = false)
abstract class PoopDatabase : RoomDatabase() {
    abstract fun poopDao(): PoopDao

    companion object {
        @Volatile
        private var INSTANCE: PoopDatabase? = null

        fun get(context: Context): PoopDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                PoopDatabase::class.java,
                "laleme.db"
            ).build().also { INSTANCE = it }
        }
    }
}
