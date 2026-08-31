package com.example.runtime

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.model.GameEntity
import java.io.File

sealed class LaunchResult {
    data class LaunchInWebView(val gameId: String, val fileUrl: String) : LaunchResult()
    data class LaunchInstalledApp(val packageName: String, val intent: Intent) : LaunchResult()
    data class LaunchApkInstall(val apkUri: Uri, val packageName: String? = null) : LaunchResult()
    data class RuntimeRequired(
        val gameTitle: String,
        val engineName: String,
        val runtimeRequired: String,
        val message: String,
        val technicalDetails: String
    ) : LaunchResult()
    data class Unsupported(
        val gameTitle: String,
        val reason: String
    ) : LaunchResult()
}

object GameLauncher {

    /**
     * Determines how to launch the game based on honest runtime capabilities
     */
    fun evaluateLaunch(context: Context, game: GameEntity): LaunchResult {
        val provider = RuntimeManager.getRuntimeForGame(game)
        return provider.launch(context, game)
    }

    fun resolveHtmlEntry(gameDir: File, preferredPath: String): File? {
        if (preferredPath.isNotBlank()) {
            val candidate = File(gameDir, preferredPath)
            if (candidate.exists()) return candidate
        }
        val wwwIndex = File(gameDir, "www/index.html")
        if (wwwIndex.exists()) return wwwIndex
        val rootIndex = File(gameDir, "index.html")
        if (rootIndex.exists()) return rootIndex

        // Recursive search for any index.html
        return gameDir.walkTopDown().firstOrNull { it.name.equals("index.html", ignoreCase = true) }
    }
}
