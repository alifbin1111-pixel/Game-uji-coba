package com.example.runtime

import android.app.Activity
import android.content.pm.ActivityInfo

/**
 * AutoOrientationManager - Handles automatic orientation detection and switching
 * Based on GameSettingsEntity orientation preference
 */
object AutoOrientationManager {

    enum class OrientationMode {
        AUTO,
        LANDSCAPE,
        LANDSCAPE_SENSOR,
        PORTRAIT,
        PORTRAIT_SENSOR,
        UNSPECIFIED
    }

    /**
     * Get ActivityInfo orientation constant from OrientationMode
     */
    fun getActivityInfoOrientation(mode: OrientationMode): Int {
        return when (mode) {
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            OrientationMode.LANDSCAPE_SENSOR -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationMode.PORTRAIT_SENSOR -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            OrientationMode.UNSPECIFIED -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    /**
     * Parse OrientationMode from string (typically from GameSettingsEntity)
     */
    fun fromString(orientation: String?): OrientationMode {
        return when (orientation?.uppercase()) {
            "AUTO" -> OrientationMode.AUTO
            "LANDSCAPE" -> OrientationMode.LANDSCAPE
            "LANDSCAPE_SENSOR" -> OrientationMode.LANDSCAPE_SENSOR
            "PORTRAIT" -> OrientationMode.PORTRAIT
            "PORTRAIT_SENSOR" -> OrientationMode.PORTRAIT_SENSOR
            else -> OrientationMode.UNSPECIFIED
        }
    }

    /**
     * Set activity orientation
     */
    fun setOrientation(activity: Activity?, mode: OrientationMode) {
        activity?.requestedOrientation = getActivityInfoOrientation(mode)
    }

    /**
     * Detect orientation from game files or engine type
     * Returns best guess based on game engine and content
     */
    fun detectOrientationFromEngine(engineType: String): OrientationMode {
        return when {
            engineType.contains("RPG_MAKER", ignoreCase = true) -> OrientationMode.LANDSCAPE
            engineType.contains("RENPY", ignoreCase = true) -> OrientationMode.LANDSCAPE
            engineType.contains("HTML5", ignoreCase = true) -> OrientationMode.LANDSCAPE
            engineType.contains("GODOT", ignoreCase = true) -> OrientationMode.LANDSCAPE_SENSOR
            engineType.contains("UNITY", ignoreCase = true) -> OrientationMode.LANDSCAPE_SENSOR
            else -> OrientationMode.AUTO
        }
    }

    /**
     * Restore activity to unspecified orientation (default)
     */
    fun restoreOrientation(activity: Activity?) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
