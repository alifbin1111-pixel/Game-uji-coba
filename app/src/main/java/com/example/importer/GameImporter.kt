package com.example.importer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.GameRepository
import com.example.engine.EngineDetector
import com.example.engine.GameScanner
import com.example.model.GameEntity
import com.example.model.GameInfo
import com.example.model.GameSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class GameImporter(private val context: Context, private val repository: GameRepository) {

    /**
     * Imports a Game Archive (.zip) by extracting and running recursive scanning
     */
    suspend fun importFromZipStream(inputStream: InputStream, originalFileName: String): GameEntity = withContext(Dispatchers.IO) {
        val gameId = UUID.randomUUID().toString().take(8)
        val gamesDir = File(context.getExternalFilesDir(null), "games")
        if (!gamesDir.exists()) gamesDir.mkdirs()

        val destDir = File(gamesDir, "game_$gameId")
        destDir.mkdirs()

        val (gameInfo, totalBytes) = GameScanner.scanAndExtractZip(context, inputStream, originalFileName, destDir)

        val game = GameEntity(
            id = gameId,
            title = gameInfo.title,
            gamePath = destDir.absolutePath,
            engineType = gameInfo.engine.name,
            engineVersion = gameInfo.version,
            confidence = gameInfo.confidence,
            executablePath = gameInfo.executablePath,
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

    /**
     * Imports a local folder (File directory)
     */
    suspend fun importFromDirectory(directory: File): GameEntity = withContext(Dispatchers.IO) {
        val gameId = UUID.randomUUID().toString().take(8)
        val gameInfo = GameScanner.scanDirectory(directory)

        val game = GameEntity(
            id = gameId,
            title = gameInfo.title,
            gamePath = directory.absolutePath,
            engineType = gameInfo.engine.name,
            engineVersion = gameInfo.version,
            confidence = gameInfo.confidence,
            executablePath = gameInfo.executablePath,
            fileSizeBytes = gameInfo.fileSizeBytes,
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

    /**
     * Imports a single chosen file (.exe, .apk, .html, .json, .pck, etc.)
     */
    suspend fun importSingleFile(inputStream: InputStream, fileName: String): GameEntity = withContext(Dispatchers.IO) {
        val gameId = UUID.randomUUID().toString().take(8)
        val gamesDir = File(context.getExternalFilesDir(null), "games")
        if (!gamesDir.exists()) gamesDir.mkdirs()

        val destDir = File(gamesDir, "game_$gameId")
        destDir.mkdirs()

        val targetFile = File(destDir, fileName)
        var totalBytes = 0L
        FileOutputStream(targetFile).use { fos ->
            val buffer = ByteArray(8192)
            var len: Int
            while (inputStream.read(buffer).also { len = it } > 0) {
                fos.write(buffer, 0, len)
                totalBytes += len
            }
        }

        // If it's a zip file, unpack it
        val finalGameInfo = if (fileName.endsWith(".zip", ignoreCase = true)) {
            val (zipInfo, zipBytes) = GameScanner.scanAndExtractZip(
                context,
                targetFile.inputStream(),
                fileName,
                destDir
            )
            targetFile.delete()
            zipInfo
        } else {
            GameScanner.scanSingleFile(context, targetFile, fileName)
        }

        val game = GameEntity(
            id = gameId,
            title = finalGameInfo.title,
            gamePath = destDir.absolutePath,
            engineType = finalGameInfo.engine.name,
            engineVersion = finalGameInfo.version,
            confidence = finalGameInfo.confidence,
            executablePath = finalGameInfo.executablePath.ifBlank { fileName },
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

    /**
     * Imports from an Android Storage Access Framework (SAF) folder tree
     */
    suspend fun importFromTreeUri(treeUri: Uri, folderName: String): GameEntity = withContext(Dispatchers.IO) {
        val gameId = UUID.randomUUID().toString().take(8)
        val gamesDir = File(context.getExternalFilesDir(null), "games")
        if (!gamesDir.exists()) gamesDir.mkdirs()

        val destDir = File(gamesDir, "game_$gameId")
        destDir.mkdirs()

        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
        var totalBytes = 0L

        if (rootDoc != null) {
            totalBytes = copyDocumentFolder(rootDoc, destDir)
        }

        val gameInfo = GameScanner.scanDirectory(destDir, folderName)

        val game = GameEntity(
            id = gameId,
            title = folderName.ifBlank { gameInfo.title },
            gamePath = destDir.absolutePath,
            engineType = gameInfo.engine.name,
            engineVersion = gameInfo.version,
            confidence = gameInfo.confidence,
            executablePath = gameInfo.executablePath,
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

    private fun copyDocumentFolder(srcDoc: DocumentFile, destDir: File): Long {
        var bytesCopied = 0L
        val files = srcDoc.listFiles()
        for (f in files) {
            val name = f.name ?: continue
            if (f.isDirectory) {
                val subDir = File(destDir, name)
                subDir.mkdirs()
                bytesCopied += copyDocumentFolder(f, subDir)
            } else {
                val destFile = File(destDir, name)
                destFile.parentFile?.mkdirs()
                try {
                    context.contentResolver.openInputStream(f.uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (input.read(buffer).also { len = it } > 0) {
                                output.write(buffer, 0, len)
                                bytesCopied += len
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore single file read errors
                }
            }
        }
        return bytesCopied
    }
}
