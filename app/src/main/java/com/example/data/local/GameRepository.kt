package com.example.data.local

import com.example.model.ControllerProfileEntity
import com.example.model.GameEntity
import com.example.model.GameSettingsEntity
import com.example.model.SaveBackupEntity
import com.example.model.TranslationEntity
import kotlinx.coroutines.flow.Flow

class GameRepository(private val db: AppDatabase) {
    val allGames: Flow<List<GameEntity>> = db.gameDao().getAllGames()
    val favoriteGames: Flow<List<GameEntity>> = db.gameDao().getFavoriteGames()
    val controllerProfiles: Flow<List<ControllerProfileEntity>> = db.controllerProfileDao().getAllProfiles()
    val totalTranslations: Flow<Int> = db.translationDao().getTotalTranslationCount()

    suspend fun getGameById(id: String): GameEntity? = db.gameDao().getGameById(id)
    suspend fun insertGame(game: GameEntity) = db.gameDao().insertGame(game)
    suspend fun updateGame(game: GameEntity) = db.gameDao().updateGame(game)
    suspend fun updateLastPlayed(id: String, timestamp: Long = System.currentTimeMillis()) = db.gameDao().updateLastPlayed(id, timestamp)
    suspend fun toggleFavorite(id: String, isFavorite: Boolean) = db.gameDao().updateFavorite(id, isFavorite)
    suspend fun renameGame(id: String, newTitle: String) = db.gameDao().renameGame(id, newTitle)
    suspend fun deleteGame(id: String) {
        db.gameDao().deleteGameById(id)
        db.translationDao().clearTranslationsForGame(id)
    }

    // Settings
    suspend fun getSettings(gameId: String): GameSettingsEntity {
        return db.gameSettingsDao().getSettings(gameId) ?: GameSettingsEntity(gameId = gameId).also {
            db.gameSettingsDao().insertOrUpdate(it)
        }
    }
    fun getSettingsFlow(gameId: String): Flow<GameSettingsEntity?> = db.gameSettingsDao().getSettingsFlow(gameId)
    suspend fun saveSettings(settings: GameSettingsEntity) = db.gameSettingsDao().insertOrUpdate(settings)

    // Translations
    suspend fun getCachedTranslation(gameId: String, text: String, targetLang: String): TranslationEntity? {
        return db.translationDao().getCachedTranslation(gameId, text, targetLang)
    }
    suspend fun saveTranslation(translation: TranslationEntity) = db.translationDao().insertTranslation(translation)
    fun getTranslationsForGame(gameId: String): Flow<List<TranslationEntity>> = db.translationDao().getTranslationsForGame(gameId)
    suspend fun clearTranslationsForGame(gameId: String) = db.translationDao().clearTranslationsForGame(gameId)
    suspend fun clearAllTranslations() = db.translationDao().clearAllTranslations()

    // Controller
    suspend fun getControllerProfile(id: String): ControllerProfileEntity? = db.controllerProfileDao().getProfileById(id)
    suspend fun saveControllerProfile(profile: ControllerProfileEntity) = db.controllerProfileDao().insertProfile(profile)

    // Backups
    fun getBackupsForGame(gameId: String): Flow<List<SaveBackupEntity>> = db.saveBackupDao().getBackupsForGame(gameId)
    suspend fun getBackupById(id: String): SaveBackupEntity? = db.saveBackupDao().getBackupById(id)
    suspend fun insertBackup(backup: SaveBackupEntity) = db.saveBackupDao().insertBackup(backup)
    suspend fun deleteBackup(id: String) = db.saveBackupDao().deleteBackupById(id)
}
