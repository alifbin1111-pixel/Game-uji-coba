package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.ControllerProfileEntity
import com.example.model.GameEntity
import com.example.model.GameSettingsEntity
import com.example.model.SaveBackupEntity
import com.example.model.TranslationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY lastPlayed DESC, addedAt DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: String): GameEntity?

    @Query("SELECT * FROM games WHERE isFavorite = 1 ORDER BY lastPlayed DESC")
    fun getFavoriteGames(): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("UPDATE games SET lastPlayed = :timestamp WHERE id = :id")
    suspend fun updateLastPlayed(id: String, timestamp: Long)

    @Query("UPDATE games SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE games SET title = :newTitle WHERE id = :id")
    suspend fun renameGame(id: String, newTitle: String)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGameById(id: String)
}

@Dao
interface GameSettingsDao {
    @Query("SELECT * FROM game_settings WHERE gameId = :gameId")
    suspend fun getSettings(gameId: String): GameSettingsEntity?

    @Query("SELECT * FROM game_settings WHERE gameId = :gameId")
    fun getSettingsFlow(gameId: String): Flow<GameSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: GameSettingsEntity)
}

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translations WHERE gameId = :gameId AND sourceText = :sourceText AND targetLanguage = :targetLang LIMIT 1")
    suspend fun getCachedTranslation(gameId: String, sourceText: String, targetLang: String): TranslationEntity?

    @Query("SELECT * FROM translations WHERE gameId = :gameId ORDER BY updatedAt DESC")
    fun getTranslationsForGame(gameId: String): Flow<List<TranslationEntity>>

    @Query("SELECT COUNT(*) FROM translations")
    fun getTotalTranslationCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(translation: TranslationEntity)

    @Query("DELETE FROM translations WHERE gameId = :gameId")
    suspend fun clearTranslationsForGame(gameId: String)

    @Query("DELETE FROM translations")
    suspend fun clearAllTranslations()
}

@Dao
interface ControllerProfileDao {
    @Query("SELECT * FROM controller_profiles")
    fun getAllProfiles(): Flow<List<ControllerProfileEntity>>

    @Query("SELECT * FROM controller_profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ControllerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ControllerProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ControllerProfileEntity)
}

@Dao
interface SaveBackupDao {
    @Query("SELECT * FROM save_backups WHERE gameId = :gameId ORDER BY timestamp DESC")
    fun getBackupsForGame(gameId: String): Flow<List<SaveBackupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: SaveBackupEntity)

    @Query("DELETE FROM save_backups WHERE id = :id")
    suspend fun deleteBackupById(id: String)
}
