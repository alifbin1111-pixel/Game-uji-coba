package com.example.model

data class GameInfo(
    val path: String,
    val title: String,
    val engine: EngineType,
    val engineName: String,
    val version: String,
    val platform: String,
    val confidence: Float,
    val detectedFiles: List<String>,
    val runtimeRequired: String,
    val isDirectlyPlayable: Boolean,
    val executablePath: String = "",
    val fileSizeBytes: Long = 0L,
    val notes: String = ""
)
