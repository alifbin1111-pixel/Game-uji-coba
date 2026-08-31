package com.example.model

enum class EngineType(val displayName: String, val badgeColor: Long) {
    RPG_MAKER_MV("RPG Maker MV", 0xFF00E5FF),
    RPG_MAKER_MZ("RPG Maker MZ", 0xFF00B0FF),
    RPG_MAKER_RGSS("RPG Maker RGSS (XP/VX/Ace)", 0xFFFF9100),
    RENPY("Ren'Py", 0xFFFF4081),
    UNITY("Unity", 0xFFE040FB),
    GODOT("Godot Engine", 0xFF448AFF),
    HTML("HTML5 / WebGL", 0xFF00E676),
    ANDROID_APK("Android APK", 0xFF81C784),
    WINDOWS_UNKNOWN("Unknown Windows Game", 0xFF90A4AE),
    UNKNOWN("Unknown / Custom", 0xFF78909C);

    companion object {
        fun fromString(name: String): EngineType {
            val clean = name.trim().uppercase()
            return when {
                clean == "RPG_MAKER_MV" || clean.contains("MV") -> RPG_MAKER_MV
                clean == "RPG_MAKER_MZ" || clean.contains("MZ") -> RPG_MAKER_MZ
                clean == "RPG_MAKER_RGSS" || clean.contains("RGSS") || clean.contains("VX") || clean.contains("XP") -> RPG_MAKER_RGSS
                clean == "RENPY" || clean.contains("REN'PY") -> RENPY
                clean == "UNITY" -> UNITY
                clean == "GODOT" -> GODOT
                clean == "HTML" || clean == "HTML5" || clean.contains("WEBGL") -> HTML
                clean == "ANDROID_APK" || clean.contains("APK") -> ANDROID_APK
                clean == "WINDOWS_UNKNOWN" || clean.contains("WINDOWS") -> WINDOWS_UNKNOWN
                clean == "UNKNOWN" || clean == "CUSTOM" -> UNKNOWN
                else -> entries.find { it.name.equals(clean, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) } ?: UNKNOWN
            }
        }
    }
}

enum class RuntimeState(val label: String) {
    INSTALLED("Installed"),
    NOT_INSTALLED("Not Installed"),
    UNSUPPORTED("Unsupported")
}

enum class RuntimeStatus {
    AVAILABLE_READY,
    AVAILABLE_ADAPTER,
    EXPERIMENTAL,
    REQUIRES_PLUGIN,
    UNSUPPORTED_ON_DEVICE
}

enum class CompatibilityRating {
    SUPPORTED,
    PARTIALLY_SUPPORTED,
    UNSUPPORTED,
    REQUIRES_EXTERNAL_LAYER
}
