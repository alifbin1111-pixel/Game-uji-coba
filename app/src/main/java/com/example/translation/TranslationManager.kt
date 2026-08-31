package com.example.translation

import com.example.data.local.GameRepository
import com.example.model.GameSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONObject

class TranslationManager(private val repository: GameRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val cache = TranslationCache(repository, scope)

    val providers: List<TranslationProvider> = listOf(
        LocalTranslationProvider(),
        GoogleTranslationProvider(),
        GeminiTranslationProvider()
    )

    private val providerMap: Map<String, TranslationProvider> = providers.associateBy { it.id }

    fun getProvider(id: String): TranslationProvider {
        return providerMap[id] ?: providerMap["LOCAL"] ?: LocalTranslationProvider()
    }

    suspend fun getCached(gameId: String, text: String, sourceLang: String, targetLang: String): String? {
        return cache.get(gameId, text, sourceLang, targetLang, isCacheEnabled = true)
    }

    fun getCachedInMemory(gameId: String, text: String, sourceLang: String, targetLang: String): String? {
        val trimmed = text.trim()
        val entries = cache.getAllMemoryEntriesForGame(gameId, targetLang)
        return entries[trimmed]
    }

    suspend fun translate(
        gameId: String,
        text: String,
        sourceLang: String = "ja",
        targetLang: String = "id",
        providerId: String = "LOCAL",
        apiKey: String? = null,
        cacheEnabled: Boolean = true
    ): String {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return ""

        // 1. Check Cache
        val cached = cache.get(gameId, cleanText, sourceLang, targetLang, cacheEnabled)
        if (cached != null && cached.isNotBlank()) {
            return cached
        }

        // 2. Fetch from active Provider
        val provider = getProvider(providerId)
        val translated = provider.translate(cleanText, sourceLang, targetLang, apiKey)

        // 3. Save to Cache
        if (translated.isNotBlank()) {
            cache.put(gameId, cleanText, sourceLang, targetLang, translated, cacheEnabled)
        }

        return translated
    }

    fun createSession(
        gameId: String,
        settings: GameSettingsEntity,
        sessionScope: CoroutineScope,
        onTranslationApplied: (original: String, translated: String, context: String) -> Unit
    ): GameTranslationSession {
        return GameTranslationSession(
            gameId = gameId,
            settings = settings,
            translationManager = this,
            scope = sessionScope,
            onTranslationApplied = onTranslationApplied
        )
    }

    suspend fun getInitialCacheJson(gameId: String, targetLang: String): String {
        val localDict = LocalTranslationProvider()
        val json = JSONObject()

        // Preload common local dictionary items
        val sampleKeys = listOf(
            "ニューゲーム", "はじめから", "コンティニュー", "つづきから", "ロード", "セーブ", "オプション",
            "せってい", "設定", "ゲーム終了", "アイテム", "スキル", "装備", "ステータス", "たたかう",
            "にげる", "防御", "はい", "いいえ", "決定", "キャンセル", "閉じる", "もどる", "所持金", "ゴールド",
            "New Game", "Continue", "Options", "Save", "Load", "Item", "Skill", "Equip", "Status",
            "Attack", "Guard", "Escape", "Yes", "No"
        )

        for (k in sampleKeys) {
            val trans = localDict.translate(k, "ja", targetLang, null)
            if (trans.isNotBlank()) {
                json.put(k, trans)
            }
        }

        return json.toString()
    }
}
