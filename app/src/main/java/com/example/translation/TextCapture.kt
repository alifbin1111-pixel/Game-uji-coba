package com.example.translation

import com.example.model.GameSettingsEntity

enum class TextContextType(val label: String) {
    DIALOG("Dialog & Narrative"),
    DIALOG_CHOICE("Dialogue Choices"),
    MENU("Menu & Options"),
    BUTTON("Interactive Buttons"),
    INVENTORY("Inventory & Items"),
    STATUS("Character Status & Attributes"),
    SKILL("Skills & Magic"),
    QUEST("Quests & Missions"),
    BATTLE_UI("Battle UI & Action Commands"),
    TITLE_SCREEN("Title Screen"),
    SYSTEM("System Notifications"),
    TOOLTIP("Help Tooltip"),
    HUD("Head-Up Display (HUD)"),
    CANVAS_UI("Canvas Text");

    companion object {
        fun fromString(value: String): TextContextType {
            val upper = value.uppercase()
            return when {
                upper.contains("CHOICE") -> DIALOG_CHOICE
                upper.contains("DIALOG") || upper.contains("MESSAGE") -> DIALOG
                upper.contains("BATTLE") || upper.contains("FIGHT") -> BATTLE_UI
                upper.contains("MENU") || upper.contains("COMMAND") -> MENU
                upper.contains("BUTTON") -> BUTTON
                upper.contains("ITEM") || upper.contains("INVENTORY") -> INVENTORY
                upper.contains("STATUS") || upper.contains("PARAM") -> STATUS
                upper.contains("SKILL") || upper.contains("MAGIC") -> SKILL
                upper.contains("QUEST") -> QUEST
                upper.contains("TITLE") -> TITLE_SCREEN
                upper.contains("TOOLTIP") || upper.contains("HELP") -> TOOLTIP
                upper.contains("HUD") -> HUD
                upper.contains("CANVAS") -> CANVAS_UI
                else -> DIALOG
            }
        }
    }
}

data class CapturedTextItem(
    val rawText: String,
    val cleanText: String,
    val contextType: TextContextType,
    val timestamp: Long = System.currentTimeMillis()
)

object TextCapture {
    // Regex for matching RPG Maker escape codes e.g. \C[1], \N[2], \V[10], \I[42], \G, \!, \., \|
    private val rpgMakerEscapeRegex = Regex("""\\[A-Za-z]+\[\d+]|\\[.!|^><{}\\]""")

    fun filterAndClean(text: String, context: String = "DIALOG"): CapturedTextItem? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // Ignore pure numbers or single special chars
        if (trimmed.matches(Regex("""^\d+$""")) || trimmed.length < 2 && !trimmed.any { it in '\u3040'..'\u30ff' || it in '\u4e00'..'\u9faf' }) {
            return null
        }

        val type = TextContextType.fromString(context)
        // Clean out RPG Maker codes for translation payload, but preserve placeholders
        val cleaned = trimmed.replace(rpgMakerEscapeRegex, "").trim()

        return CapturedTextItem(
            rawText = trimmed,
            cleanText = if (cleaned.isNotEmpty()) cleaned else trimmed,
            contextType = type
        )
    }

    fun isAllowedBySettings(contextType: TextContextType, settings: GameSettingsEntity): Boolean {
        if (!settings.translationEnabled) return false

        return when (contextType) {
            TextContextType.DIALOG, TextContextType.DIALOG_CHOICE -> settings.translateDialog
            TextContextType.MENU, TextContextType.TITLE_SCREEN, TextContextType.TOOLTIP -> settings.translateMenu
            TextContextType.BATTLE_UI -> settings.translateBattleUi
            TextContextType.BUTTON,
            TextContextType.INVENTORY,
            TextContextType.STATUS,
            TextContextType.SKILL,
            TextContextType.QUEST,
            TextContextType.SYSTEM,
            TextContextType.HUD,
            TextContextType.CANVAS_UI -> settings.translateUi
        }
    }
}
