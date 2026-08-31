package com.example.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

interface TranslationProvider {
    val id: String
    val name: String
    val description: String
    val isConfigured: Boolean
    suspend fun translate(text: String, sourceLang: String, targetLang: String, apiKey: String?): String
}

/**
 * Built-in high-performance local dictionary and rule-based localization provider.
 * Translates RPG Maker vocabularies, UI menus, battle commands, skills, items,
 * system dialogues, and common gaming phrases without requiring any network or API keys.
 */
class LocalTranslationProvider : TranslationProvider {
    override val id: String = "LOCAL"
    override val name: String = "Built-in Offline Game Dictionary"
    override val description: String = "Instant zero-latency localization for RPG Maker UI, menus, battle actions & common gaming dialogues."
    override val isConfigured: Boolean = true

    // Japanese -> Indonesian dictionary for gaming
    private val jaToIdDict = mapOf(
        // System & Title
        "ニューゲーム" to "Permainan Baru",
        "はじめから" to "Mulai Dari Awal",
        "コンティニュー" to "Lanjutkan",
        "つづきから" to "Lanjutkan",
        "ロード" to "Muat Data",
        "セーブ" to "Simpan Data",
        "オプション" to "Pengaturan",
        "せってい" to "Pengaturan",
        "設定" to "Pengaturan",
        "ゲーム終了" to "Keluar Permainan",
        "おわる" to "Selesai / Keluar",
        "タイトルへ" to "Ke Layar Judul",
        "はい" to "Ya",
        "いいえ" to "Tidak",
        "決定" to "Pilih / Konfirmasi",
        "キャンセル" to "Batal",
        "もどる" to "Kembali",
        "閉じる" to "Tutup",

        // Main Menu
        "アイテム" to "Barang / Item",
        "どうぐ" to "Barang",
        "道具" to "Barang",
        "スキル" to "Keahlian / Skill",
        "特技" to "Keahlian Khusus",
        "魔法" to "Sihir / Magic",
        "まほう" to "Sihir",
        "装備" to "Perlengkapan",
        "そうび" to "Perlengkapan",
        "ステータス" to "Status Karakter",
        "つよさ" to "Status Kekuatan",
        "並び替え" to "Susun Formasi",
        "ならびかえ" to "Ubah Posisi",
        "隊列" to "Formasi Tim",
        "クエスト" to "Misi / Quest",
        "図鑑" to "Ensiklopedia / Bestiary",
        "所持金" to "Uang Dimiliki",
        "ゴールド" to "Gold / Emas",
        "お金" to "Uang",

        // Battle Commands & UI
        "たたかう" to "Bertarung",
        "戦う" to "Bertarung",
        "攻撃" to "Serang",
        "こうげき" to "Serang",
        "防御" to "Bertahan",
        "ぼうぎょ" to "Bertahan",
        "にげる" to "Kabur",
        "逃げる" to "Kabur",
        "オート" to "Otomatis",
        "必殺技" to "Jurus Pamungkas",
        "リピート" to "Ulangi Aksi",
        "逃走" to "Melarikan Diri",
        "勝利" to "Kemenangan!",
        "敗北" to "Kekalahan...",
        "全滅" to "Pasukan Musnah",
        "経験値" to "Poin Pengalaman (EXP)",
        "レベルアップ" to "Naik Level!",
        "レベル" to "Level",
        "獲得" to "Memperoleh",

        // Character Attributes & Stats
        "最大HP" to "HP Maks",
        "最大MP" to "MP Maks",
        "最大TP" to "TP Maks",
        "HP" to "HP",
        "MP" to "MP",
        "TP" to "TP",
        "LV" to "LV",
        "EXP" to "EXP",
        "次のレベルまで" to "Menuju Level Berikutnya",
        "攻撃力" to "Daya Serang (ATK)",
        "防御力" to "Daya Tahan (DEF)",
        "魔法力" to "Kekuatan Sihir (MAT)",
        "魔法防御" to "Pertahanan Sihir (MDF)",
        "敏捷性" to "Kelincahan (AGI)",
        "運" to "Keberuntungan (LUK)",
        "命中率" to "Akurasi Serangan",
        "回避率" to "Daya Menghindar",
        "会心率" to "Peluang Critical",

        // Equipment Slots
        "武器" to "Senjata",
        "盾" to "Perisai",
        "頭" to "Kepala / Helm",
        "身体" to "Badan / Zirah",
        "装飾品" to "Aksesoris",
        "最強装備" to "Perlengkapan Terkuat",
        "全て外す" to "Lepas Semua",

        // Common Dialog & Actions
        "話す" to "Bicara",
        "調べる" to "Periksa",
        "開ける" to "Buka",
        "読む" to "Baca",
        "買う" to "Beli",
        "売る" to "Jual",
        "やめる" to "Batal",
        "宝箱を開けた" to "Membuka peti harta karun!",
        "手に入れた" to "Telah diperoleh!",
        "見つけた" to "Menemukan!",
        "失った" to "Kehilangan...",
        "宿屋" to "Penginapan",
        "泊まる" to "Menginap",
        "休む" to "Istirahat",
        "勇者" to "Pahlawan",
        "魔王" to "Raja Iblis",
        "長老" to "Tetua Desa",
        "村人" to "Penduduk Desa",
        "兵士" to "Prajurit",
        "王様" to "Baginda Raja"
    )

    // English -> Indonesian dictionary for gaming
    private val enToIdDict = mapOf(
        // System & Title
        "new game" to "Permainan Baru",
        "continue" to "Lanjutkan",
        "load game" to "Muat Permainan",
        "save game" to "Simpan Permainan",
        "options" to "Pengaturan",
        "settings" to "Pengaturan",
        "quit" to "Keluar",
        "exit" to "Keluar",
        "back" to "Kembali",
        "confirm" to "Konfirmasi",
        "cancel" to "Batal",
        "yes" to "Ya",
        "no" to "Tidak",
        "close" to "Tutup",
        "title screen" to "Layar Judul",

        // Main Menu & Inventory
        "item" to "Barang / Item",
        "items" to "Daftar Barang",
        "inventory" to "Inventaris",
        "skill" to "Keahlian / Skill",
        "skills" to "Daftar Keahlian",
        "magic" to "Sihir",
        "equipment" to "Perlengkapan",
        "equip" to "Pasang Perlengkapan",
        "status" to "Status Karakter",
        "formation" to "Formasi Tim",
        "order" to "Urutan",
        "quest" to "Misi",
        "quests" to "Daftar Misi",
        "save" to "Simpan",
        "load" to "Muat",
        "gold" to "Emas / Gold",
        "money" to "Uang",

        // Battle UI
        "fight" to "Bertarung",
        "attack" to "Serang",
        "guard" to "Bertahan",
        "defend" to "Bertahan",
        "escape" to "Kabur",
        "run" to "Kabur",
        "auto" to "Otomatis",
        "special" to "Spesial",
        "victory" to "Kemenangan!",
        "defeat" to "Kekalahan...",
        "game over" to "Permainan Berakhir",
        "level up" to "Naik Level!",

        // Stats
        "attack power" to "Daya Serang",
        "defense power" to "Pertahanan",
        "magic attack" to "Serangan Sihir",
        "magic defense" to "Pertahanan Sihir",
        "agility" to "Kelincahan",
        "luck" to "Keberuntungan",
        "accuracy" to "Akurasi",
        "evasion" to "Menghindar",
        "critical" to "Serangan Kritis",

        // Common Equipment
        "weapon" to "Senjata",
        "shield" to "Perisai",
        "head" to "Kepala",
        "body" to "Badan",
        "accessory" to "Aksesoris",
        "optimize" to "Optimalkan",
        "clear" to "Kosongkan"
    )

    override suspend fun translate(text: String, sourceLang: String, targetLang: String, apiKey: String?): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""

        // Check exact match Japanese dictionary
        jaToIdDict[trimmed]?.let { return it }

        // Check lowercase Japanese / English dictionary
        val lower = trimmed.lowercase()
        enToIdDict[lower]?.let { return it }
        jaToIdDict[lower]?.let { return it }

        // Partial match replacements for common phrases
        var result = trimmed
        for ((k, v) in jaToIdDict) {
            if (result.contains(k)) {
                result = result.replace(k, v)
            }
        }
        for ((k, v) in enToIdDict) {
            if (result.contains(k, ignoreCase = true)) {
                result = result.replace(Regex("(?i)\\b" + Regex.escape(k) + "\\b"), v)
            }
        }

        return result
    }
}

/**
 * Free Google Translate Web Endpoint with multi-language fallback.
 */
class GoogleTranslationProvider : TranslationProvider {
    override val id: String = "GOOGLE_FREE"
    override val name: String = "Google Translate Rapid Engine"
    override val description: String = "Online translation without requiring API credentials. High translation coverage."
    override val isConfigured: Boolean = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun translate(text: String, sourceLang: String, targetLang: String, apiKey: String?): String {
        return withContext(Dispatchers.IO) {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return@withContext ""

            try {
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                val src = if (sourceLang.equals("auto", ignoreCase = true)) "auto" else sourceLang
                val tgt = if (targetLang.isBlank()) "id" else targetLang
                val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$src&tl=$tgt&dt=t&q=$encoded"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:128.0)")
                    .get()
                    .build()

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
                        if (sb.isNotEmpty()) return@withContext sb.toString().trim()
                    }
                }
            } catch (e: Exception) {
                // Silently fallback to local dictionary
            }
            // Fallback to local dictionary
            LocalTranslationProvider().translate(trimmed, sourceLang, targetLang, null)
        }
    }
}

/**
 * Google Gemini Generative AI Provider with game localization tuning.
 */
class GeminiTranslationProvider : TranslationProvider {
    override val id: String = "GEMINI"
    override val name: String = "Google Gemini AI (Game Localization Specialist)"
    override val description: String = "Context-aware gaming translations preserving lore, character voice & RPG terms."
    
    private fun resolveApiKey(explicitKey: String?): String? {
        if (!explicitKey.isNullOrBlank() && explicitKey != "null") return explicitKey
        val envKey = System.getenv("GEMINI_API_KEY") ?: System.getenv("API_KEY")
        if (!envKey.isNullOrBlank() && envKey != "null") return envKey
        return try {
            val buildConfigClass = Class.forName("com.example.BuildConfig")
            val field = buildConfigClass.getField("GEMINI_API_KEY")
            val value = field.get(null) as? String
            if (!value.isNullOrBlank() && value != "null" && !value.startsWith("YOUR_")) value else null
        } catch (e: Exception) {
            null
        }
    }

    override val isConfigured: Boolean
        get() = resolveApiKey(null) != null

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun translate(text: String, sourceLang: String, targetLang: String, apiKey: String?): String {
        return withContext(Dispatchers.IO) {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return@withContext ""

            val key = resolveApiKey(apiKey)
            if (key.isNullOrBlank()) {
                // Not configured: gracefully use free Google / local dictionary without error
                return@withContext GoogleTranslationProvider().translate(trimmed, sourceLang, targetLang, null)
            }

            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key"
                val prompt = """
                    You are an expert video game localization engine (like MTool localization tool).
                    Translate this video game UI element, dialogue line, or menu string from $sourceLang to $targetLang.
                    Maintain RPG conventions, honorifics, gaming terms, and formatting tags like \V[1], \N[2], \C[3] intact.
                    Return ONLY the translated string with no explanations or quote marks.

                    Text:
                    $trimmed
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
                val request = Request.Builder().url(url).post(body).build()
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
                            val trans = parts.getJSONObject(0).optString("text").trim()
                            if (trans.isNotEmpty()) return@withContext trans
                        }
                    }
                }
            } catch (e: Exception) {
                // fallback
            }

            GoogleTranslationProvider().translate(trimmed, sourceLang, targetLang, null)
        }
    }
}
