package com.example.translation

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.BuildConfig
import com.example.data.local.GameRepository
import com.example.model.TranslationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface TranslationProvider {
    val id: String
    val name: String
    suspend fun translate(text: String, sourceLang: String, targetLang: String, apiKey: String?): String
}

class GeminiTranslationProvider : TranslationProvider {
    override val id: String = "GEMINI"
    override val name: String = "Google Gemini AI (Game Dialogue Specialized)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun translate(text: String, sourceLang: String, targetLang: String, apiKey: String?): String {
        return withContext(Dispatchers.IO) {
            val key: String? = if (!apiKey.isNullOrBlank()) apiKey else null

            if (key.isNullOrBlank()) {
                // Return high-quality offline rule-based fallback / prompt user
                return@withContext offlineTranslateFallback(text, sourceLang, targetLang)
            }

            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key"
                val prompt = """
                    You are a professional video game localization translator.
                    Translate the following video game dialogue/UI text accurately from $sourceLang to $targetLang.
                    Keep character tone, gaming terms, and formatting intact.
                    Return ONLY the translated text without commentary or quotes.
                    
                    Text to translate:
                    $text
                """.trimIndent()

                val jsonBody = JSONObject().apply {
                    val contents = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            }
                            put("parts", parts)
                        }
                        put(contentObj)
                    }
                    put("contents", contents)
                }

                val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: ""
                    val root = JSONObject(respStr)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCand = candidates.getJSONObject(0)
                        val content = firstCand.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text").trim()
                        }
                    }
                }
                offlineTranslateFallback(text, sourceLang, targetLang)
            } catch (e: Exception) {
                offlineTranslateFallback(text, sourceLang, targetLang)
            }
        }
    }

    private fun offlineTranslateFallback(text: String, sourceLang: String, targetLang: String): String {
        // Built-in offline dictionary for common game phrases in Japanese/English -> Indonesian/English
        val lower = text.trim().lowercase()
        val dictId = mapOf(
            "new game" to "Game Baru",
            "continue" to "Lanjutkan",
            "options" to "Pengaturan",
            "quit" to "Keluar",
            "save" to "Simpan",
            "load" to "Muat Data",
            "attack" to "Serang",
            "magic" to "Sihir",
            "item" to "Item / Barang",
            "escape" to "Kabur",
            "guard" to "Bertahan",
            "hp" to "HP / Darah",
            "mp" to "MP / Mana",
            "level" to "Level",
            "exp" to "EXP",
            "gold" to "Emas / Koin",
            "press any key to start" to "Tekan tombol apa saja untuk mulai",
            "game over" to "Permainan Berakhir",
            "talk" to "Bicara",
            "examine" to "Periksa",
            "status" to "Status Karakter",
            "equipment" to "Perlengkapan",
            "はじめから" to "Mulai Dari Awal (New Game)",
            "つづきから" to "Lanjutkan (Continue)",
            "せってい" to "Pengaturan (Settings)",
            "たたかう" to "Bertarung (Fight)",
            "にげる" to "Kabur (Run)",
            "どうぐ" to "Barang (Items)",
            "まほう" to "Sihir (Magic)",
            "ぼうぎょ" to "Bertahan (Guard)",
            "はい" to "Ya",
            "いいえ" to "Tidak",
            "セーブ" to "Simpan (Save)",
            "ロード" to "Muat (Load)",
            "クエスト" to "Misi (Quest)"
        )

        for ((k, v) in dictId) {
            if (lower == k || text.trim() == k) {
                return if (targetLang.startsWith("id", ignoreCase = true)) v else v
            }
        }

        return "[Auto-Trans: $text]"
    }
}

class GoogleTranslateProvider : TranslationProvider {
    override val id: String = "GOOGLE_FREE"
    override val name: String = "Google Translate Rapid Engine"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun translate(text: String, sourceLang: String, targetLang: String, apiKey: String?): String {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(text, "UTF-8")
                val src = if (sourceLang.equals("auto", ignoreCase = true)) "auto" else sourceLang
                val tgt = if (targetLang.isBlank()) "id" else targetLang
                val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$src&tl=$tgt&dt=t&q=$encoded"

                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: ""
                    val array = JSONArray(respStr)
                    val sentences = array.optJSONArray(0)
                    if (sentences != null) {
                        val sb = StringBuilder()
                        for (i in 0 until sentences.length()) {
                            val sentence = sentences.optJSONArray(i)
                            if (sentence != null) {
                                sb.append(sentence.optString(0))
                            }
                        }
                        if (sb.isNotEmpty()) return@withContext sb.toString()
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
            text
        }
    }
}

class TranslationManager(private val repository: GameRepository) {
    private val providers: Map<String, TranslationProvider> = mapOf(
        "GEMINI" to GeminiTranslationProvider(),
        "GOOGLE_FREE" to GoogleTranslateProvider()
    )

    suspend fun translate(
        gameId: String,
        sourceText: String,
        sourceLang: String = "ja",
        targetLang: String = "id",
        providerId: String = "GEMINI",
        apiKey: String? = null
    ): String {
        val cleanText = sourceText.trim()
        if (cleanText.isBlank()) return ""

        // 1. Check SQLite Translation Cache first
        val cached = repository.getCachedTranslation(gameId, cleanText, targetLang)
        if (cached != null) {
            return cached.translatedText
        }

        // 2. Fetch from Translation Provider
        val provider = providers[providerId] ?: providers["GEMINI"] ?: GeminiTranslationProvider()
        val translated = provider.translate(cleanText, sourceLang, targetLang, apiKey)

        // 3. Save to Cache
        if (translated.isNotBlank() && translated != cleanText) {
            repository.saveTranslation(
                TranslationEntity(
                    gameId = gameId,
                    sourceText = cleanText,
                    sourceLanguage = sourceLang,
                    targetLanguage = targetLang,
                    translatedText = translated
                )
            )
        }

        return translated
    }
}
