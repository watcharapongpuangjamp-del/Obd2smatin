package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VillageEntity::class,
        VillageMembershipEntity::class,
        HouseholdEntity::class,
        PersonEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SmartOsmDatabase : RoomDatabase() {
    abstract fun smartOsmDao(): SmartOsmDao

    companion object {
        @Volatile
        private var INSTANCE: SmartOsmDatabase? = null

        fun getDatabase(context: Context): SmartOsmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartOsmDatabase::class.java,
                    "smart_osm_db"
                ).fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
