package com.example.runtime

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log

enum class GameOrientation(val label: String, val orientationCode: Int) {
    AUTO("Auto Detect", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE),
    LANDSCAPE("Landscape (Fixed)", ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
    LANDSCAPE_SENSOR("Landscape (Sensor)", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE),
    REVERSE_LANDSCAPE("Reverse Landscape", ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE),
    PORTRAIT("Portrait", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
    REVERSE_PORTRAIT("Reverse Portrait", ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT),
    SENSOR("Full Sensor", ActivityInfo.SCREEN_ORIENTATION_SENSOR),
    UNSPECIFIED("Unspecified", ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

    companion object {
        fun fromString(value: String): GameOrientation {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
                ?: AUTO
        }
    }
}

object OrientationManager {
    private const val TAG = "OrientationManager"
    private var savedLauncherOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    fun lockOrientationForGame(
        activity: Activity?,
        orientationSetting: String = "AUTO",
        engineType: String = ""
    ): Int {
        if (activity == null) return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        // Save current orientation before switching
        savedLauncherOrientation = activity.requestedOrientation
        Log.d(TAG, "Saved launcher orientation: $savedLauncherOrientation")

        val targetOrientation = when (orientationSetting.uppercase()) {
            "LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "LANDSCAPE_SENSOR", "SENSOR_LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "REVERSE_LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            "PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "REVERSE_PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            "SENSOR" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            "UNSPECIFIED" -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            "AUTO", "" -> {
                when {
                    engineType.contains("RPG_MAKER", ignoreCase = true) -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    engineType.contains("RENPY", ignoreCase = true) -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    engineType.contains("UNITY", ignoreCase = true) -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    engineType.contains("GODOT", ignoreCase = true) -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    engineType.contains("HTML", ignoreCase = true) -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        try {
            activity.requestedOrientation = targetOrientation
            Log.d(TAG, "Set game orientation to: $targetOrientation for engine: $engineType (setting: $orientationSetting)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set requested orientation", e)
        }

        return savedLauncherOrientation
    }

    fun restoreOrientation(activity: Activity?, originalOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
        if (activity == null) return
        try {
            val restoreTo = if (originalOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                originalOrientation
            } else if (savedLauncherOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                savedLauncherOrientation
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            activity.requestedOrientation = restoreTo
            Log.d(TAG, "Restored launcher orientation to: $restoreTo")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore requested orientation", e)
        }
    }
}
