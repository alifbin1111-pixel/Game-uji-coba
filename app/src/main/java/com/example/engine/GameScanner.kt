package com.example.engine

import android.content.Context
import android.os.Environment
import com.example.model.GameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * GameScanner - Recursively scans directories for game installations
 * Handles:
 * - Recursive directory traversal
 * - Permission-safe file access
 * - Symbolic link detection (avoids infinite loops)
 * - Caching/build folder exclusion
 * - Executable & engine file detection
 */
class GameScanner(private val context: Context) {

    companion object {
        // Directories to skip
        private val SKIP_PATTERNS = listOf(
            "cache", "temp", ".tmp", ".gradle", ".build",
            "__pycache__", "node_modules", ".git", ".svn",
            ".metadata", "build", "dist", "out", "bin",
            "Thumbs.db", ".DS_Store", "System Volume Information"
        )

        // Max recursion depth to avoid performance issues
        private const val MAX_DEPTH = 6

        // Max files to scan
        private const val MAX_FILES = 500
    }

    /**
     * Scan a directory for games
     * Returns list of detected GameEntity objects
     */
    suspend fun scanDirectory(
        dir: File,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): List<GameEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<GameEntity>()
        val scannedFiles = mutableSetOf<String>()
        val visitedPaths = mutableSetOf<String>()

        try {
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                scanDirectoryRecursive(
                    dir,
                    results,
                    scannedFiles,
                    visitedPaths,
                    depth = 0,
                    onProgress = onProgress
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("GameScanner", "Error scanning directory: ${e.message}")
        }

        results
    }

    /**
     * Scan a single directory (not recursive) for game files
     */
    suspend fun scanSingleDirectory(dir: File): GameEntity? = withContext(Dispatchers.IO) {
        try {
            if (!dir.exists() || !dir.isDirectory || !dir.canRead()) {
                return@withContext null
            }

            val fileList = mutableListOf<String>()
            collectRelativePaths(dir, "", fileList, maxDepth = 3, maxFiles = 300)

            val detection = EngineDetector.detect(dir)
            if (detection.engineType.name != "CUSTOM") {
                GameEntity(
                    id = UUID.randomUUID().toString().take(8),
                    title = dir.name.replace("_", " ").replace("-", " "),
                    gamePath = dir.absolutePath,
                    engineType = detection.engineType.name,
                    engineVersion = detection.version,
                    confidence = detection.confidence,
                    executablePath = detection.mainExecutable,
                    fileSizeBytes = calculateDirSize(dir),
                    addedAt = System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("GameScanner", "Error scanning single directory: ${e.message}")
            null
        }
    }

    /**
     * Scan External Storage for games (Documents, Downloads, etc)
     */
    suspend fun scanExternalStorage(
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): List<GameEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<GameEntity>()

        val externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (externalDir.exists() && externalDir.canRead()) {
            scanDirectory(externalDir, onProgress)
        }

        results
    }

    private fun scanDirectoryRecursive(
        dir: File,
        results: MutableList<GameEntity>,
        scannedFiles: MutableSet<String>,
        visitedPaths: MutableSet<String>,
        depth: Int = 0,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) {
        // Stop if max depth reached
        if (depth > MAX_DEPTH) return
        if (results.size >= MAX_FILES) return

        try {
            // Avoid symbolic links and infinite loops
            val canonicalPath = dir.canonicalPath
            if (visitedPaths.contains(canonicalPath)) return
            visitedPaths.add(canonicalPath)

            // Check if directory should be skipped
            if (shouldSkipDirectory(dir)) return

            val files = dir.listFiles() ?: return
            onProgress?.invoke(results.size, MAX_FILES)

            for (file in files) {
                try {
                    when {
                        file.isDirectory && file.canRead() -> {
                            // Check if it's a game directory
                            val gameEntity = scanSingleDirectory(file)
                            if (gameEntity != null) {
                                if (!scannedFiles.contains(gameEntity.gamePath)) {
                                    results.add(gameEntity)
                                    scannedFiles.add(gameEntity.gamePath)
                                    onProgress?.invoke(results.size, MAX_FILES)
                                }
                            } else {
                                // Recurse into subdirectory
                                if (results.size < MAX_FILES) {
                                    scanDirectoryRecursive(
                                        file,
                                        results,
                                        scannedFiles,
                                        visitedPaths,
                                        depth + 1,
                                        onProgress
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Skip files/directories that can't be read
                    continue
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GameScanner", "Error in recursive scan: ${e.message}")
        }
    }

    /**
     * Check if directory should be skipped
     */
    private fun shouldSkipDirectory(dir: File): Boolean {
        val dirName = dir.name.lowercase()
        return SKIP_PATTERNS.any { dirName.contains(it, ignoreCase = true) }
    }

    /**
     * Collect relative file paths for engine detection
     */
    private fun collectRelativePaths(
        dir: File,
        currentPrefix: String,
        result: MutableList<String>,
        maxDepth: Int,
        maxFiles: Int
    ) {
        if (maxDepth <= 0 || result.size >= maxFiles || !dir.exists() || !dir.isDirectory) return

        try {
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (result.size >= maxFiles) break

                val relPath = if (currentPrefix.isEmpty()) f.name else "$currentPrefix/${f.name}"
                result.add(relPath)

                if (f.isDirectory && f.canRead() && !shouldSkipDirectory(f)) {
                    collectRelativePaths(f, relPath, result, maxDepth - 1, maxFiles)
                }
            }
        } catch (e: Exception) {
            // Ignore permission errors
        }
    }

    /**
     * Calculate total directory size recursively
     */
    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        try {
            val files = dir.listFiles() ?: return 0L
            for (f in files) {
                size += if (f.isDirectory) calculateDirSize(f) else f.length()
            }
        } catch (e: Exception) {
            // Ignore errors
        }
        return size
    }

    /**
     * Detect game platform from file paths and engine type
     */
    fun detectPlatform(engineType: String, filePaths: List<String>): String {
        val lowerPaths = filePaths.map { it.lowercase() }
        return when {
            lowerPaths.any { it.endsWith(".exe") } -> "Windows"
            lowerPaths.any { it.endsWith(".apk") } -> "Android"
            lowerPaths.any { it.contains("libunity.so") } -> "Android"
            lowerPaths.any { it.endsWith(".pck") } -> when {
                engineType.contains("HTML5") -> "Web"
                else -> "Unknown"
            }
            lowerPaths.any { it.endsWith("index.html") } -> "Web"
            else -> "Unknown"
        }
    }

    /**
     * Detect architecture from file paths
     */
    fun detectArchitecture(filePaths: List<String>): String {
        val lowerPaths = filePaths.map { it.lowercase() }
        return when {
            lowerPaths.any { it.contains("arm64") || it.contains("aarch64") } -> "ARM64-v8a"
            lowerPaths.any { it.contains("armv7") || it.contains("armeabi") } -> "ARMv7-a"
            lowerPaths.any { it.contains("x86_64") } -> "x86_64"
            lowerPaths.any { it.contains("x86") } -> "x86"
            lowerPaths.any { it.contains("x64") } -> "x86_64"
            else -> "Unknown"
        }
    }
}
