package com.example.engine

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.model.GameInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object GameScanner {

    // Ignored system folders during recursive scanning
    private val IGNORED_DIRECTORIES = setOf(
        ".git", ".svn", ".gradle", "android/data", "android/obb", "cache", ".thumbnails", ".trash"
    )

    /**
     * Recursively scans a local File directory and returns detailed GameInfo
     */
    suspend fun scanDirectory(dir: File, customTitle: String? = null): GameInfo = withContext(Dispatchers.IO) {
        val totalSize = calculateDirSize(dir)
        val detection = EngineDetector.detect(dir)
        val extractedTitle = extractTitleFromMetadata(dir)
        val finalTitle = customTitle ?: extractedTitle ?: dir.name.replace("_", " ").replace("-", " ")

        detection.toGameInfo(
            gamePath = dir.absolutePath,
            title = finalTitle,
            totalSizeBytes = totalSize
        )
    }

    /**
     * Scans an imported ZIP stream, extracts to target destination, and performs engine analysis
     */
    suspend fun scanAndExtractZip(
        context: Context,
        inputStream: InputStream,
        originalFileName: String,
        targetDir: File
    ): Pair<GameInfo, Long> = withContext(Dispatchers.IO) {
        if (!targetDir.exists()) targetDir.mkdirs()

        var totalBytes = 0L
        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                // Zip Slip protection
                if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    throw SecurityException("Zip entry is outside of target directory: ${entry.name}")
                }

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                            totalBytes += len
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val detection = EngineDetector.detect(targetDir)
        val extractedTitle = extractTitleFromMetadata(targetDir)
        val cleanTitle = extractedTitle ?: originalFileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
        val gameInfo = detection.toGameInfo(
            gamePath = targetDir.absolutePath,
            title = cleanTitle.ifBlank { detection.engineName },
            totalSizeBytes = totalBytes
        )

        Pair(gameInfo, totalBytes)
    }

    /**
     * Scans a single chosen file (e.g. .apk, .exe, .html, .json, .pck)
     */
    suspend fun scanSingleFile(
        context: Context,
        file: File,
        originalFileName: String
    ): GameInfo = withContext(Dispatchers.IO) {
        val relList = listOf(file.name)
        val detection = EngineDetector.detectFromFiles(relList)
        val cleanTitle = originalFileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")

        detection.toGameInfo(
            gamePath = file.absolutePath,
            title = cleanTitle.ifBlank { originalFileName },
            totalSizeBytes = file.length()
        )
    }

    /**
     * Recursively scans DocumentFile tree from Android Storage Access Framework (SAF)
     */
    suspend fun scanDocumentTree(
        context: Context,
        treeUri: Uri
    ): List<String> = withContext(Dispatchers.IO) {
        val fileList = mutableListOf<String>()
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        collectDocumentPaths(rootDoc, "", fileList, maxDepth = 5, maxFiles = 300)
        fileList
    }

    private fun collectDocumentPaths(
        doc: DocumentFile,
        currentPrefix: String,
        result: MutableList<String>,
        maxDepth: Int,
        maxFiles: Int
    ) {
        if (maxDepth <= 0 || result.size >= maxFiles || !doc.exists()) return

        val files = doc.listFiles()
        for (f in files) {
            val name = f.name ?: continue
            if (IGNORED_DIRECTORIES.contains(name.lowercase())) continue
            val relPath = if (currentPrefix.isEmpty()) name else "$currentPrefix/$name"
            result.add(relPath)
            if (f.isDirectory) {
                collectDocumentPaths(f, relPath, result, maxDepth - 1, maxFiles)
            }
            if (result.size >= maxFiles) break
        }
    }

    private fun extractTitleFromMetadata(dir: File): String? {
        try {
            // 1. Check RPG Maker data/System.json
            val systemJsonFile = File(dir, "data/System.json").takeIf { it.exists() }
                ?: File(dir, "www/data/System.json").takeIf { it.exists() }
            if (systemJsonFile != null) {
                val json = JSONObject(systemJsonFile.readText(Charsets.UTF_8))
                val title = json.optString("gameTitle", "")
                if (title.isNotBlank()) return title
            }

            // 2. Check package.json
            val packageJsonFile = File(dir, "package.json").takeIf { it.exists() }
                ?: File(dir, "www/package.json").takeIf { it.exists() }
            if (packageJsonFile != null) {
                val json = JSONObject(packageJsonFile.readText(Charsets.UTF_8))
                val windowObj = json.optJSONObject("window")
                val windowTitle = windowObj?.optString("title", "")
                if (!windowTitle.isNullOrBlank()) return windowTitle
                val name = json.optString("name", "")
                if (name.isNotBlank()) return name
            }

            // 3. Check Game.ini (RGSS)
            val gameIniFile = File(dir, "Game.ini").takeIf { it.exists() }
                ?: File(dir, "game.ini").takeIf { it.exists() }
            if (gameIniFile != null) {
                gameIniFile.useLines { lines ->
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("Title=", ignoreCase = true)) {
                            val title = trimmed.substringAfter("=").trim()
                            if (title.isNotBlank()) return title
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return null
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            if (IGNORED_DIRECTORIES.contains(f.name.lowercase())) continue
            size += if (f.isDirectory) calculateDirSize(f) else f.length()
        }
        return size
    }
}
