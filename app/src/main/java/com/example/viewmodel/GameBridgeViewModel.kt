package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.GameRepository
import com.example.importer.DemoGamesBundler
import com.example.importer.GameImporter
import com.example.model.ControllerProfileEntity
import com.example.model.GameEntity
import com.example.model.GameSettingsEntity
import com.example.model.SaveBackupEntity
import com.example.save.SaveManager
import com.example.translation.TranslationManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

class GameBridgeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = GameRepository(database)
    private val importer = GameImporter(application, repository)
    private val saveManager = SaveManager(application, repository)
    private val translationManager = TranslationManager(repository)

    val allGames: StateFlow<List<GameEntity>> = repository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteGames: StateFlow<List<GameEntity>> = repository.favoriteGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val controllerProfiles: StateFlow<List<ControllerProfileEntity>> = repository.controllerProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTranslations: StateFlow<Int> = repository.totalTranslations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedEngineFilter = MutableStateFlow("ALL")
    val selectedEngineFilter = _selectedEngineFilter.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    // Filtered games combining search and engine type
    val filteredGames: StateFlow<List<GameEntity>> = combine(
        allGames,
        searchQuery,
        selectedEngineFilter
    ) { games, query, filter ->
        games.filter { game ->
            val matchesQuery = query.isBlank() || game.title.contains(query, ignoreCase = true) || game.engineType.contains(query, ignoreCase = true)
            val matchesEngine = filter == "ALL" || game.engineType.contains(filter, ignoreCase = true) || (filter == "RPG_MAKER" && game.engineType.startsWith("RPG_MAKER"))
            matchesQuery && matchesEngine
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Seed initial controller profile if empty
            if (repository.getControllerProfile("default") == null) {
                repository.saveControllerProfile(
                    ControllerProfileEntity(
                        id = "default",
                        name = "Standard Virtual Gamepad",
                        opacity = 0.70f,
                        scale = 1.0f,
                        hapticFeedback = true
                    )
                )
            }
            // Auto install demo games for immediate out-of-the-box playability
            DemoGamesBundler.installSampleGamesIfEmpty(application, repository)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedEngineFilter(filter: String) {
        _selectedEngineFilter.value = filter
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun toggleFavorite(gameId: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(gameId, !currentFavorite)
        }
    }

    fun updateLastPlayed(gameId: String) {
        viewModelScope.launch {
            repository.updateLastPlayed(gameId)
        }
    }

    fun renameGame(gameId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameGame(gameId, newTitle.trim())
            _statusMessage.value = "Game berhasil diubah namanya."
        }
    }

    fun deleteGame(gameId: String) {
        viewModelScope.launch {
            repository.deleteGame(gameId)
            _statusMessage.value = "Game dihapus dari library."
        }
    }

    fun getSettingsFlow(gameId: String): Flow<GameSettingsEntity?> {
        return repository.getSettingsFlow(gameId)
    }

    fun saveSettings(settings: GameSettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            _statusMessage.value = "Pengaturan game disimpan."
        }
    }

    fun saveControllerProfile(profile: ControllerProfileEntity) {
        viewModelScope.launch {
            repository.saveControllerProfile(profile)
            _statusMessage.value = "Profil controller disimpan."
        }
    }

    fun getBackupsForGame(gameId: String): Flow<List<SaveBackupEntity>> {
        return repository.getBackupsForGame(gameId)
    }

    suspend fun createSaveBackup(gameId: String, title: String): SaveBackupEntity {
        val backup = saveManager.createBackup(gameId, title)
        _statusMessage.value = "Backup save berhasil dibuat!"
        return backup
    }

    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            repository.deleteBackup(backupId)
            _statusMessage.value = "Backup save dihapus."
        }
    }

    suspend fun translate(
        gameId: String,
        text: String,
        sourceLang: String = "ja",
        targetLang: String = "id",
        provider: String = "GEMINI",
        apiKey: String? = null
    ): String {
        return translationManager.translate(gameId, text, sourceLang, targetLang, provider, apiKey)
    }

    fun clearTranslationCache(gameId: String? = null) {
        viewModelScope.launch {
            if (gameId != null) {
                repository.clearTranslationsForGame(gameId)
            } else {
                repository.clearAllTranslations()
            }
            _statusMessage.value = "Cache terjemahan dibersihkan."
        }
    }

    fun importZipFile(inputStream: InputStream, filename: String) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val game = importer.importFromZipStream(inputStream, filename)
                _statusMessage.value = "Game '${game.title}' berhasil di-import (${game.engineType})!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal mengimpor ZIP: ${e.localizedMessage}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun importDirectory(dir: File) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val game = importer.importFromDirectory(dir)
                _statusMessage.value = "Folder game '${game.title}' berhasil ditambahkan!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal memindai folder: ${e.localizedMessage}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun resetSampleGames() {
        viewModelScope.launch {
            _isImporting.value = true
            DemoGamesBundler.installSampleGamesIfEmpty(getApplication(), repository)
            _isImporting.value = false
            _statusMessage.value = "Contoh game berhasil dipulihkan!"
        }
    }
}
