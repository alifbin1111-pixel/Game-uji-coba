package com.example.importer

import android.content.Context
import android.net.Uri
import com.example.data.local.GameRepository
import com.example.engine.EngineDetector
import com.example.model.GameEntity
import com.example.model.GameSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class GameImporter(private val context: Context, private val repository: GameRepository) {

    suspend fun importFromZipStream(inputStream: InputStream, originalFileName: String): GameEntity = withContext(Dispatchers.IO) {
        val gameId = UUID.randomUUID().toString().take(8)
        val gamesDir = File(context.getExternalFilesDir(null), "games")
        if (!gamesDir.exists()) gamesDir.mkdirs()

        val destDir = File(gamesDir, "game_$gameId")
        destDir.mkdirs()

        // Extract ZIP
        var totalBytes = 0L
        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)
                // Zip Slip vulnerability protection
                if (!newFile.canonicalPath.startsWith(destDir.canonicalPath)) {
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

        // Detect engine
        val detection = EngineDetector.detect(destDir)
        val cleanTitle = originalFileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")

        val game = GameEntity(
            id = gameId,
            title = cleanTitle.ifBlank { detection.engineName },
            gamePath = destDir.absolutePath,
            engineType = detection.engineType.name,
            engineVersion = detection.version,
            confidence = detection.confidence,
            executablePath = detection.mainExecutable,
            fileSizeBytes = totalBytes,
            addedAt = System.currentTimeMillis()
        )

        repository.insertGame(game)
        repository.saveSettings(
            GameSettingsEntity(
                gameId = gameId,
                translationEnabled = true,
                virtualControllerEnabled = true
            )
        )

        game
    }

    suspend fun importFromDirectory(directory: File): GameEntity = withContext(Dispatchers.IO) {
        val gameId = UUID.randomUUID().toString().take(8)
        val detection = EngineDetector.detect(directory)
        val totalSize = calculateDirSize(directory)

        val game = GameEntity(
            id = gameId,
            title = directory.name.replace("_", " ").replace("-", " "),
            gamePath = directory.absolutePath,
            engineType = detection.engineType.name,
            engineVersion = detection.version,
            confidence = detection.confidence,
            executablePath = detection.mainExecutable,
            fileSizeBytes = totalSize,
            addedAt = System.currentTimeMillis()
        )

        repository.insertGame(game)
        repository.saveSettings(
            GameSettingsEntity(
                gameId = gameId,
                translationEnabled = true,
                virtualControllerEnabled = true
            )
        )

        game
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) calculateDirSize(f) else f.length()
        }
        return size
    }
}
