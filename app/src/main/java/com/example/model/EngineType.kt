package com.example.model

enum class EngineType(val displayName: String, val badgeColor: Long) {
    RPG_MAKER_MV("RPG Maker MV", 0xFF00E5FF),
    RPG_MAKER_MZ("RPG Maker MZ", 0xFF00B0FF),
    RPG_MAKER_VXACE("RPG Maker VX Ace", 0xFFFF9100),
    RPG_MAKER_VX("RPG Maker VX", 0xFFFF6D00),
    RPG_MAKER_XP("RPG Maker XP", 0xFFFFD600),
    RENPY("Ren'Py", 0xFFFF4081),
    HTML5("HTML5 / WebGL", 0xFF00E676),
    GODOT("Godot Engine", 0xFF448AFF),
    UNITY("Unity", 0xFFE040FB),
    GAMEMAKER("GameMaker", 0xFF69F0AE),
    UNREAL("Unreal Engine", 0xFF7C4DFF),
    CUSTOM("Custom / Native", 0xFF9E9E9E);

    companion object {
        fun fromString(name: String): EngineType {
            return entries.find { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) } ?: CUSTOM
        }
    }
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
