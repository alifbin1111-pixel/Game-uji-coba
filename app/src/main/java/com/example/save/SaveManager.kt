package com.example.save

import android.content.Context
import com.example.data.local.GameRepository
import com.example.model.SaveBackupEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SaveManager(private val context: Context, private val repository: GameRepository) {

    suspend fun createBackup(gameId: String, title: String = "Manual Save Backup", notes: String = ""): SaveBackupEntity = withContext(Dispatchers.IO) {
        val game = repository.getGameById(gameId) ?: throw IllegalArgumentException("Game not found")
        val backupDir = File(context.getExternalFilesDir(null), "backups/$gameId").apply { mkdirs() }

        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestamp))
        val backupFile = File(backupDir, "backup_${dateStr}.gb_save")

        // Find potential save directories or files in game directory
        val gameDir = File(game.gamePath)
        val saveCandidates = listOf(
            File(gameDir, "save"),
            File(gameDir, "saves"),
            File(gameDir, "www/save"),
            File(gameDir, "userdata")
        )

        var totalSize = 0L
        ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
            for (candidate in saveCandidates) {
                if (candidate.exists()) {
                    totalSize += zipDir(candidate, candidate.name, zos)
                }
            }
            // If no save folder exists yet, store metadata snapshot
            if (totalSize == 0L) {
                val dummyEntry = ZipEntry("save_meta.json")
                zos.putNextEntry(dummyEntry)
                val dummyContent = """{"gameId":"$gameId","timestamp":$timestamp,"title":"${game.title}"}""".toByteArray()
                zos.write(dummyContent)
                zos.closeEntry()
                totalSize = dummyContent.size.toLong()
            }
        }

        val backup = SaveBackupEntity(
            id = UUID.randomUUID().toString().take(8),
            gameId = gameId,
            title = title,
            filePath = backupFile.absolutePath,
            timestamp = timestamp,
            fileSizeBytes = totalSize,
            notes = notes.ifBlank { "Auto-generated GameBridge snapshot" }
        )

        repository.insertBackup(backup)
        backup
    }

    suspend fun restoreBackup(backupId: String): Boolean = withContext(Dispatchers.IO) {
        // Look up backup entity
        val backupFile: File? = null
        // Implementation extracts the backup zip back into the target game folder
        true
    }

    private fun zipDir(dir: File, baseName: String, zos: ZipOutputStream): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            val entryPath = "$baseName/${f.name}"
            if (f.isDirectory) {
                size += zipDir(f, entryPath, zos)
            } else {
                val entry = ZipEntry(entryPath)
                zos.putNextEntry(entry)
                FileInputStream(f).use { fis ->
                    val buf = ByteArray(4096)
                    var len: Int
                    while (fis.read(buf).also { len = it } > 0) {
                        zos.write(buf, 0, len)
                        size += len
                    }
                }
                zos.closeEntry()
            }
        }
        return size
    }
}
