package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.ControllerProfileEntity
import com.example.model.GameEntity
import com.example.model.GameSettingsEntity
import com.example.model.SaveBackupEntity
import com.example.model.TranslationEntity

@Database(
    entities = [
        GameEntity::class,
        GameSettingsEntity::class,
        TranslationEntity::class,
        ControllerProfileEntity::class,
        SaveBackupEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun gameSettingsDao(): GameSettingsDao
    abstract fun translationDao(): TranslationDao
    abstract fun controllerProfileDao(): ControllerProfileDao
    abstract fun saveBackupDao(): SaveBackupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gamebridge_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
