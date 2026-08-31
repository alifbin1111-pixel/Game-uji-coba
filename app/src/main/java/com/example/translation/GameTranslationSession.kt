package com.example.translation

import android.util.Log
import com.example.model.GameSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class TranslationSessionStats(
    val totalCaptured: Int = 0,
    val totalTranslated: Int = 0,
    val cacheHits: Int = 0,
    val queueSize: Int = 0,
    val lastTranslatedText: String = "",
    val lastSourceText: String = ""
)

class GameTranslationSession(
    val gameId: String,
    val settings: GameSettingsEntity,
    private val translationManager: TranslationManager,
    private val scope: CoroutineScope,
    private val onTranslationApplied: (original: String, translated: String, context: String) -> Unit
) {
    private val tag = "GameTranslationSession"

    private val capturedCount = AtomicInteger(0)
    private val translatedCount = AtomicInteger(0)
    private val cacheHitCount = AtomicInteger(0)

    private val _stats = MutableStateFlow(TranslationSessionStats())
    val stats = _stats.asStateFlow()

    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val requestChannel = Channel<Pair<CapturedTextItem, String>>(capacity = Channel.UNLIMITED)
    private var workerJob: Job? = null

    init {
        startWorker()
    }

    private fun startWorker() {
        workerJob = scope.launch(Dispatchers.IO) {
            for ((item, sourceLang) in requestChannel) {
                try {
                    val raw = item.rawText
                    val targetLang = settings.targetLanguage

                    // 1. Double check cache
                    val cached = translationManager.getCached(gameId, raw, sourceLang, targetLang)
                    if (cached != null) {
                        cacheHitCount.incrementAndGet()
                        updateStats(raw, cached)
                        onTranslationApplied(raw, cached, item.contextType.name)
                        inFlight.remove(raw)
                        continue
                    }

                    // 2. Perform translation
                    val translated = translationManager.translate(
                        gameId = gameId,
                        text = item.cleanText,
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        providerId = settings.translationProvider,
                        apiKey = null,
                        cacheEnabled = settings.translationCacheEnabled
                    )

                    if (translated.isNotBlank()) {
                        translatedCount.incrementAndGet()
                        updateStats(raw, translated)
                        onTranslationApplied(raw, translated, item.contextType.name)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Translation worker error: ${e.message}")
                } finally {
                    inFlight.remove(item.rawText)
                }
            }
        }
    }

    fun handleCapturedText(rawText: String, context: String = "DIALOG", sourceLanguage: String = settings.sourceLanguage) {
        if (!settings.translationEnabled) return

        val item = TextCapture.filterAndClean(rawText, context) ?: return
        if (!TextCapture.isAllowedBySettings(item.contextType, settings)) return

        capturedCount.incrementAndGet()

        // Fast synchronous memory lookup
        val srcLang = if (sourceLanguage.isBlank() || sourceLanguage == "auto") "ja" else sourceLanguage
        val cached = translationManager.getCachedInMemory(gameId, item.rawText, srcLang, settings.targetLanguage)
        if (cached != null) {
            cacheHitCount.incrementAndGet()
            updateStats(item.rawText, cached)
            onTranslationApplied(item.rawText, cached, item.contextType.name)
            return
        }

        if (inFlight.add(item.rawText)) {
            scope.launch {
                requestChannel.send(Pair(item, srcLang))
            }
        }
    }

    fun handleBatchCapturedTexts(jsonArrayString: String, sourceLanguage: String = settings.sourceLanguage) {
        if (!settings.translationEnabled) return
        try {
            val array = JSONArray(jsonArrayString)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                if (obj != null) {
                    val text = obj.optString("text")
                    val context = obj.optString("context", "CANVAS_UI")
                    if (text.isNotEmpty()) {
                        handleCapturedText(text, context, sourceLanguage)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse batch texts", e)
        }
    }

    private fun updateStats(source: String, translated: String) {
        _stats.value = TranslationSessionStats(
            totalCaptured = capturedCount.get(),
            totalTranslated = translatedCount.get(),
            cacheHits = cacheHitCount.get(),
            queueSize = inFlight.size,
            lastTranslatedText = translated,
            lastSourceText = source
        )
    }

    fun close() {
        workerJob?.cancel()
        requestChannel.close()
        inFlight.clear()
    }
}
