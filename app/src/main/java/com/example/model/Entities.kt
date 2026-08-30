package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val gamePath: String,
    val coverUri: String? = null,
    val engineType: String,
    val engineVersion: String = "1.0",
    val confidence: Float = 0.95f,
    val lastPlayed: Long = 0L,
    val isFavorite: Boolean = false,
    val runtimeId: String = "auto",
    val executablePath: String = "",
    val fileSizeBytes: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "game_settings")
data class GameSettingsEntity(
    @PrimaryKey val gameId: String,
    val resolutionScale: Float = 1.0f,
    val fpsLimit: Int = 60,
    val touchControlsEnabled: Boolean = true,
    val virtualControllerEnabled: Boolean = true,
    val controllerProfileId: String = "default",
    val audioVolume: Float = 1.0f,
    val translationEnabled: Boolean = false,
    val ocrEnabled: Boolean = false,
    val sourceLanguage: String = "ja",
    val targetLanguage: String = "id",
    val translationProvider: String = "GEMINI",
    val performanceMode: String = "BALANCED",
    val aspectRatio: String = "16:9",
    val orientation: String = "LANDSCAPE"
)

@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val gameId: String,
    val sourceText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val translatedText: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "controller_profiles")
data class ControllerProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val opacity: Float = 0.65f,
    val scale: Float = 1.0f,
    val dpadX: Float = 0.08f,
    val dpadY: Float = 0.68f,
    val btnAX: Float = 0.88f,
    val btnAY: Float = 0.72f,
    val btnBX: Float = 0.80f,
    val btnBY: Float = 0.82f,
    val btnXX: Float = 0.80f,
    val btnXY: Float = 0.62f,
    val btnYX: Float = 0.72f,
    val btnYY: Float = 0.72f,
    val btnLX: Float = 0.10f,
    val btnLY: Float = 0.38f,
    val btnRX: Float = 0.88f,
    val btnRY: Float = 0.38f,
    val btnStartX: Float = 0.58f,
    val btnStartY: Float = 0.88f,
    val btnSelectX: Float = 0.42f,
    val btnSelectY: Float = 0.88f,
    val analogX: Float = 0.18f,
    val analogY: Float = 0.78f,
    val analogEnabled: Boolean = false,
    val hapticFeedback: Boolean = true
)

@Entity(tableName = "save_backups")
data class SaveBackupEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val title: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val notes: String = ""
)
