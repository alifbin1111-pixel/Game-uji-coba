package com.example.translation

import com.example.data.local.GameRepository
import com.example.model.TranslationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class TranslationCache(
    private val repository: GameRepository,
    private val scope: CoroutineScope
) {
    // L1 In-Memory Fast Cache: Key = "$gameId:$sourceLang:$targetLang:$sourceText" -> Value = translatedText
    private val memoryCache = ConcurrentHashMap<String, String>()

    private fun buildKey(gameId: String, sourceLang: String, targetLang: String, text: String): String {
        return "$gameId:$sourceLang:$targetLang:$text"
    }

    suspend fun get(
        gameId: String,
        sourceText: String,
        sourceLang: String,
        targetLang: String,
        isCacheEnabled: Boolean = true
    ): String? {
        if (!isCacheEnabled) return null
        val trimmed = sourceText.trim()
        if (trimmed.isEmpty()) return null

        val key = buildKey(gameId, sourceLang, targetLang, trimmed)
        // 1. Check Fast L1 Memory Cache
        memoryCache[key]?.let { return it }

        // 2. Check Persistent SQLite Room Cache
        val dbResult = repository.getCachedTranslation(gameId, trimmed, targetLang)
        if (dbResult != null && dbResult.translatedText.isNotBlank()) {
            memoryCache[key] = dbResult.translatedText
            return dbResult.translatedText
        }

        return null
    }

    fun put(
        gameId: String,
        sourceText: String,
        sourceLanguage: String,
        targetLanguage: String,
        translatedText: String,
        isCacheEnabled: Boolean = true
    ) {
        val trimmedSource = sourceText.trim()
        val trimmedTrans = translatedText.trim()
        if (trimmedSource.isEmpty() || trimmedTrans.isEmpty()) return

        val key = buildKey(gameId, sourceLanguage, targetLanguage, trimmedSource)
        memoryCache[key] = trimmedTrans

        if (isCacheEnabled) {
            scope.launch(Dispatchers.IO) {
                repository.saveTranslation(
                    TranslationEntity(
                        gameId = gameId,
                        sourceText = trimmedSource,
                        sourceLanguage = sourceLanguage,
                        targetLanguage = targetLanguage,
                        translatedText = trimmedTrans
                    )
                )
            }
        }
    }

    fun getAllMemoryEntriesForGame(gameId: String, targetLang: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val prefix = "$gameId:"
        for ((k, v) in memoryCache) {
            if (k.startsWith(prefix) && k.contains(":$targetLang:")) {
                val parts = k.split(":$targetLang:", limit = 2)
                if (parts.size == 2) {
                    result[parts[1]] = v
                }
            }
        }
        return result
    }

    fun clear(gameId: String? = null) {
        if (gameId == null) {
            memoryCache.clear()
        } else {
            val prefix = "$gameId:"
            memoryCache.keys.removeIf { it.startsWith(prefix) }
        }
    }
}
