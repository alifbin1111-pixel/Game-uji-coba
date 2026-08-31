package com.example.runtime

import android.content.Context
import android.os.Build
import com.example.model.CompatibilityRating
import com.example.model.EngineType
import com.example.model.GameEntity
import com.example.model.RuntimeState
import com.example.model.RuntimeStatus
import java.io.File

/**
 * Detailed diagnostic information for a specific game and its engine runtime.
 */
data class RuntimeDiagnostic(
    val runtimeId: String,
    val runtimeName: String,
    val engine: String,
    val detectedArchitecture: String,
    val deviceArchitecture: String,
    val androidVersion: String,
    val compatibility: CompatibilityRating,
    val status: RuntimeStatus,
    val runtimeState: RuntimeState,
    val memoryAvailableMB: Long,
    val technicalDetails: String,
    val solutionOrRequirement: String
)

/**
 * Runtime execution lifecycle states.
 */
enum class RuntimeProcessState {
    IDLE,
    STARTING,
    RUNNING,
    PAUSED,
    STOPPING,
    STOPPED,
    ERROR
}

/**
 * Base contract for all game engine runtime providers.
 */
interface GameRuntimeProvider {
    val id: String
    val name: String
    val version: String
    val description: String
    val supportedEngines: List<EngineType>
    val runtimeState: RuntimeState
    val isPlayableDirectly: Boolean
    val architecture: String
    val processState: RuntimeProcessState

    fun canRunDirectly(game: GameEntity): Boolean
    fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating
    fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic
    fun launch(context: Context, game: GameEntity): LaunchResult
    fun stop(gameId: String)
    fun pause(gameId: String)
    fun resume(gameId: String)
}

// Backward-compatible alias for existing references
typealias RuntimeProvider = GameRuntimeProvider

/**
 * 1. RPG Maker MV/MZ Native Chromium/WebGL Runtime Provider
 */
class RpgMakerWebRuntimeProvider : GameRuntimeProvider {
    override val id: String = "rpgmaker-web-v1"
    override val name: String = "RPG Maker MV/MZ Web Runtime"
    override val version: String = "2.3.0"
    override val description: String = "Hardware-accelerated Chromium WebGL2 & Canvas engine with MTool live dialogue interception, virtual gamepad, and local persistence."
    override val supportedEngines: List<EngineType> = listOf(EngineType.RPG_MAKER_MV, EngineType.RPG_MAKER_MZ)
    override val runtimeState: RuntimeState = RuntimeState.INSTALLED
    override val isPlayableDirectly: Boolean = true
    override val architecture: String = "JavaScript / WebAudio / WebGL2"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = true

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = game.engineType,
            detectedArchitecture = "HTML5 / WebGL / PixiJS (JavaScript)",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.SUPPORTED,
            status = RuntimeStatus.AVAILABLE_READY,
            runtimeState = RuntimeState.INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Chromium WebGL/Canvas runtime active. AudioContext, touch mapping, virtual gamepad overlay, and live translation hook fully available.",
            solutionOrRequirement = "Ready for immediate play."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        processState = RuntimeProcessState.RUNNING
        val gameDir = File(game.gamePath)
        val entry = findHtmlEntry(gameDir, game.executablePath)
        return if (entry != null && entry.exists()) {
            LaunchResult.LaunchInWebView(game.id, entry.toURI().toString())
        } else {
            LaunchResult.Unsupported(game.title, "index.html not found in: ${game.gamePath}")
        }
    }

    override fun stop(gameId: String) {
        processState = RuntimeProcessState.STOPPED
    }

    override fun pause(gameId: String) {
        processState = RuntimeProcessState.PAUSED
    }

    override fun resume(gameId: String) {
        processState = RuntimeProcessState.RUNNING
    }

    private fun findHtmlEntry(gameDir: File, preferred: String): File? {
        if (preferred.isNotBlank()) {
            val f = File(gameDir, preferred)
            if (f.exists()) return f
        }
        val wwwIndex = File(gameDir, "www/index.html")
        if (wwwIndex.exists()) return wwwIndex
        val rootIndex = File(gameDir, "index.html")
        if (rootIndex.exists()) return rootIndex
        return gameDir.walkTopDown().firstOrNull { it.name.equals("index.html", ignoreCase = true) }
    }
}

/**
 * 2. HTML5 & WebGL Native Core Provider
 */
class Html5WebRuntimeProvider : GameRuntimeProvider {
    override val id: String = "html5-core-v1"
    override val name: String = "HTML5 & WebGL Native Core"
    override val version: String = "1.5.0"
    override val description: String = "Universal browser runtime for Phaser, PixiJS, Construct 2/3, and standard web games."
    override val supportedEngines: List<EngineType> = listOf(EngineType.HTML)
    override val runtimeState: RuntimeState = RuntimeState.INSTALLED
    override val isPlayableDirectly: Boolean = true
    override val architecture: String = "HTML5 / Canvas / WebGL2"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = true

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "HTML5 / WebGL",
            detectedArchitecture = "HTML5 / Canvas / WebGL2",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.SUPPORTED,
            status = RuntimeStatus.AVAILABLE_READY,
            runtimeState = RuntimeState.INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Direct Chromium hardware acceleration ready.",
            solutionOrRequirement = "Directly executable with high performance."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        processState = RuntimeProcessState.RUNNING
        val gameDir = File(game.gamePath)
        val index = if (game.executablePath.isNotBlank()) File(gameDir, game.executablePath) else File(gameDir, "index.html")
        return if (index.exists()) {
            LaunchResult.LaunchInWebView(game.id, index.toURI().toString())
        } else {
            LaunchResult.Unsupported(game.title, "index.html not found.")
        }
    }

    override fun stop(gameId: String) { processState = RuntimeProcessState.STOPPED }
    override fun pause(gameId: String) { processState = RuntimeProcessState.PAUSED }
    override fun resume(gameId: String) { processState = RuntimeProcessState.RUNNING }
}

/**
 * 3. Unity Android Native Runtime Provider
 */
class UnityAndroidRuntimeProvider : GameRuntimeProvider {
    override val id: String = "unity-android-v1"
    override val name: String = "Unity Android Native Provider"
    override val version: String = "1.0.0"
    override val description: String = "Discovers, installs, and launches native Android Unity APK packages."
    override val supportedEngines: List<EngineType> = listOf(EngineType.UNITY)
    override val runtimeState: RuntimeState = RuntimeState.INSTALLED
    override val isPlayableDirectly: Boolean = true
    override val architecture: String = "ARM64-v8a / ARMv7-a Native Binary"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean {
        val path = game.executablePath.lowercase()
        return path.endsWith(".apk") || path.contains("libunity.so")
    }

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "Unity (Android Native)",
            detectedArchitecture = "ARM64-v8a / ARMv7-a Native APK",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.SUPPORTED,
            status = RuntimeStatus.AVAILABLE_READY,
            runtimeState = RuntimeState.INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Android-native Unity package detected. Ready for installation or direct launch.",
            solutionOrRequirement = "Direct launch or PackageInstaller supported."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        val apkFile = File(game.gamePath, game.executablePath).let {
            if (it.exists()) it else File(game.gamePath)
        }
        val apkInfo = ApkPackageHelper.parseApk(context, apkFile)
        return if (apkInfo != null) {
            if (apkInfo.isInstalled && apkInfo.launchIntent != null) {
                LaunchResult.LaunchInstalledApp(apkInfo.packageName, apkInfo.launchIntent)
            } else {
                val installIntent = ApkPackageHelper.createInstallIntent(context, apkFile)
                LaunchResult.LaunchApkInstall(installIntent.data!!, apkInfo.packageName)
            }
        } else {
            LaunchResult.Unsupported(game.title, "File APK Unity tidak valid atau tidak ditemukan.")
        }
    }

    override fun stop(gameId: String) { processState = RuntimeProcessState.STOPPED }
    override fun pause(gameId: String) { processState = RuntimeProcessState.PAUSED }
    override fun resume(gameId: String) { processState = RuntimeProcessState.RUNNING }
}

/**
 * 4. Unity WebGL Runtime Provider
 */
class UnityWebRuntimeProvider : GameRuntimeProvider {
    override val id: String = "unity-webgl-v1"
    override val name: String = "Unity WebGL WebAssembly Runtime"
    override val version: String = "1.0.0"
    override val description: String = "Executes Unity WebGL export builds using WebAssembly, WebGL2, and WebAudio in WebView."
    override val supportedEngines: List<EngineType> = listOf(EngineType.UNITY)
    override val runtimeState: RuntimeState = RuntimeState.INSTALLED
    override val isPlayableDirectly: Boolean = true
    override val architecture: String = "WebAssembly / WebGL2"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = game.executablePath.endsWith(".html", ignoreCase = true)

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "Unity (WebGL)",
            detectedArchitecture = "WebAssembly / WebGL2",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.SUPPORTED,
            status = RuntimeStatus.AVAILABLE_READY,
            runtimeState = RuntimeState.INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Unity WebGL WebAssembly build detected. Executable in hardware-accelerated web runtime.",
            solutionOrRequirement = "Direct web play enabled."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        processState = RuntimeProcessState.RUNNING
        val htmlFile = File(game.gamePath, game.executablePath)
        return if (htmlFile.exists()) {
            LaunchResult.LaunchInWebView(game.id, htmlFile.toURI().toString())
        } else {
            LaunchResult.Unsupported(game.title, "Export WebGL Unity tidak ditemukan.")
        }
    }

    override fun stop(gameId: String) { processState = RuntimeProcessState.STOPPED }
    override fun pause(gameId: String) { processState = RuntimeProcessState.PAUSED }
    override fun resume(gameId: String) { processState = RuntimeProcessState.RUNNING }
}

/**
 * 5. Unity Windows Runtime Provider
 */
class UnityWindowsRuntimeProvider : GameRuntimeProvider {
    override val id: String = "unity-windows-v1"
    override val name: String = "Unity Windows Runtime Provider"
    override val version: String = "1.0.0"
    override val description: String = "Windows compatibility container provider for x86/x64 Unity standalone executables."
    override val supportedEngines: List<EngineType> = listOf(EngineType.UNITY)
    override val runtimeState: RuntimeState = RuntimeState.NOT_INSTALLED
    override val isPlayableDirectly: Boolean = false
    override val architecture: String = "x86_64 PE Binary (UnityPlayer.dll / GameAssembly.dll)"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = false

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.UNSUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "Unity (Windows x86/x64 Standalone)",
            detectedArchitecture = "x86/x64 Windows PE Executable (UnityPlayer.dll)",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.UNSUPPORTED,
            status = RuntimeStatus.UNSUPPORTED_ON_DEVICE,
            runtimeState = RuntimeState.NOT_INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Game is compiled for Windows OS (UnityPlayer.dll / GameAssembly.dll). Android ARM processors cannot run Windows x86 binaries without translation layers (Box64 + Wine).",
            solutionOrRequirement = "Windows Compatibility Runtime Required. Gunakan environment Wine/Box64 atau dapatkan build Android APK dari game ini."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        return LaunchResult.RuntimeRequired(
            gameTitle = game.title,
            engineName = "Unity (Windows Standalone)",
            runtimeRequired = "Windows Compatibility Runtime (Box64/Wine)",
            message = "Runtime belum tersedia. Game tidak dapat dijalankan sampai runtime Windows compatibility terpasang.",
            technicalDetails = "Game Unity ini dikompilasi untuk arsitektur Windows x86/x64 (${game.executablePath}). Android ARM tidak dapat menjalankan file .exe secara native."
        )
    }

    override fun stop(gameId: String) {}
    override fun pause(gameId: String) {}
    override fun resume(gameId: String) {}
}

/**
 * 6. Ren'Py Android Native Runtime Provider
 */
class RenPyAndroidRuntimeProvider : GameRuntimeProvider {
    override val id: String = "renpy-android-v1"
    override val name: String = "Ren'Py Android Native Provider"
    override val version: String = "1.0.0"
    override val description: String = "Discovers, installs, and launches packaged Ren'Py Android APKs."
    override val supportedEngines: List<EngineType> = listOf(EngineType.RENPY)
    override val runtimeState: RuntimeState = RuntimeState.INSTALLED
    override val isPlayableDirectly: Boolean = true
    override val architecture: String = "Android Native (librenpy.so / SDL2)"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = game.executablePath.endsWith(".apk", ignoreCase = true)

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "Ren'Py (Android APK)",
            detectedArchitecture = "Android ART + librenpy.so (ARM64/v7a)",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.SUPPORTED,
            status = RuntimeStatus.AVAILABLE_READY,
            runtimeState = RuntimeState.INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Packaged Ren'Py APK detected. Ready for installation or direct launch.",
            solutionOrRequirement = "Direct launch enabled."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        val apkFile = File(game.gamePath, game.executablePath).let {
            if (it.exists()) it else File(game.gamePath)
        }
        val apkInfo = ApkPackageHelper.parseApk(context, apkFile)
        return if (apkInfo != null) {
            if (apkInfo.isInstalled && apkInfo.launchIntent != null) {
                LaunchResult.LaunchInstalledApp(apkInfo.packageName, apkInfo.launchIntent)
            } else {
                val installIntent = ApkPackageHelper.createInstallIntent(context, apkFile)
                LaunchResult.LaunchApkInstall(installIntent.data!!, apkInfo.packageName)
            }
        } else {
            LaunchResult.Unsupported(game.title, "File APK Ren'Py tidak valid atau tidak ditemukan.")
        }
    }

    override fun stop(gameId: String) {}
    override fun pause(gameId: String) {}
    override fun resume(gameId: String) {}
}

/**
 * 7. Ren'Py Web Runtime Provider
 */
class RenPyWebRuntimeProvider : GameRuntimeProvider {
    override val id: String = "renpy-web-v1"
    override val name: String = "Ren'Py WebAssembly Runtime"
    override val version: String = "1.0.0"
    override val description: String = "Executes Ren'Py Web (Pyodide/Emscripten WebGL) builds."
    override val supportedEngines: List<EngineType> = listOf(EngineType.RENPY)
    override val runtimeState: RuntimeState = RuntimeState.INSTALLED
    override val isPlayableDirectly: Boolean = true
    override val architecture: String = "WebAssembly / Python WebGL"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = game.executablePath.endsWith(".html", ignoreCase = true)

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "Ren'Py (Web)",
            detectedArchitecture = "WebAssembly / Python Web",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.SUPPORTED,
            status = RuntimeStatus.AVAILABLE_READY,
            runtimeState = RuntimeState.INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Ren'Py Web build detected.",
            solutionOrRequirement = "Direct web execution enabled."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        processState = RuntimeProcessState.RUNNING
        val htmlFile = File(game.gamePath, game.executablePath)
        return if (htmlFile.exists()) {
            LaunchResult.LaunchInWebView(game.id, htmlFile.toURI().toString())
        } else {
            LaunchResult.Unsupported(game.title, "File web Ren'Py tidak ditemukan.")
        }
    }

    override fun stop(gameId: String) {}
    override fun pause(gameId: String) {}
    override fun resume(gameId: String) {}
}

/**
 * 8. Ren'Py Windows / Packaged Script Provider
 */
class RenPyWindowsRuntimeProvider : GameRuntimeProvider {
    override val id: String = "renpy-windows-v1"
    override val name: String = "Ren'Py Script & Native Provider"
    override val version: String = "1.2.0"
    override val description: String = "Dialogue script parser, translation extractor, and native Ren'Py PyBridge integration."
    override val supportedEngines: List<EngineType> = listOf(EngineType.RENPY)
    override val runtimeState: RuntimeState = RuntimeState.NOT_INSTALLED
    override val isPlayableDirectly: Boolean = false
    override val architecture: String = "Python / Cython / SDL2 / RPA Archives"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = false

    fun extractDialogue(gameDir: File): List<String> {
        val texts = mutableListOf<String>()
        gameDir.walkTopDown().maxDepth(3).filter { it.name.endsWith(".rpy", ignoreCase = true) }.forEach { f ->
            try {
                f.useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length > 2) {
                            texts.add(trimmed.removeSurrounding("\""))
                        }
                    }
                }
            } catch (e: Exception) {}
        }
        return texts.take(100)
    }

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.PARTIALLY_SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "Ren'Py Visual Novel",
            detectedArchitecture = "Python / Cython / SDL2 / RPA Archives",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.PARTIALLY_SUPPORTED,
            status = RuntimeStatus.REQUIRES_PLUGIN,
            runtimeState = RuntimeState.NOT_INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Ren'Py scripts (.rpy/.rpyc) and archive packages (.rpa) identified. Dialogue extraction for translation is supported; interactive gameplay requires Ren'Py Python/SDL2 Android engine backend.",
            solutionOrRequirement = "Runtime Required. Memerlukan native Ren'Py Python runtime untuk gameplay penuh."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        return LaunchResult.RuntimeRequired(
            gameTitle = game.title,
            engineName = "Ren'Py Visual Novel Engine",
            runtimeRequired = "Ren'Py Android Native Provider",
            message = "Runtime belum tersedia. Game tidak dapat dijalankan sampai runtime yang sesuai dipasang.",
            technicalDetails = "Game Ren'Py memerlukan runtime Python/SDL2 backend untuk mengeksekusi script .rpyc. Anda tetap dapat mengekstrak teks untuk terjemahan."
        )
    }

    override fun stop(gameId: String) {}
    override fun pause(gameId: String) {}
    override fun resume(gameId: String) {}
}

/**
 * 9. RPG Maker XP (RGSS1) Runtime Provider
 */
class Rgss1RuntimeProvider : GameRuntimeProvider {
    override val id: String = "rgss1-runtime-v1"
    override val name: String = "RPG Maker XP (RGSS1) Runtime"
    override val version: String = "1.0.0"
    override val description: String = "Native compatibility provider for RGSS1 Ruby 1.8 bytecode (.rxdata)."
    override val supportedEngines: List<EngineType> = listOf(EngineType.RPG_MAKER_RGSS)
    override val runtimeState: RuntimeState = RuntimeState.NOT_INSTALLED
    override val isPlayableDirectly: Boolean = false
    override val architecture: String = "Ruby 1.8 RGSS1 Bytecode (.rxdata)"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = false
    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.PARTIALLY_SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "RPG Maker XP",
            detectedArchitecture = "Ruby 1.8 RGSS1 Bytecode (.rxdata)",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.PARTIALLY_SUPPORTED,
            status = RuntimeStatus.REQUIRES_PLUGIN,
            runtimeState = RuntimeState.NOT_INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Game uses RGSS1 Ruby scripts and Win32 Game.exe. Android OS cannot execute Game.exe natively without native mkxp-z JNI library.",
            solutionOrRequirement = "Runtime Required. Memerlukan native engine mkxp-z (RGSS1) untuk menjalankan game ini di Android."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        return LaunchResult.RuntimeRequired(
            gameTitle = game.title,
            engineName = "RPG Maker XP (RGSS1)",
            runtimeRequired = "RGSS1 Compatibility Runtime (mkxp-z)",
            message = "Runtime belum tersedia. Game tidak dapat dijalankan sampai runtime yang sesuai dipasang.",
            technicalDetails = "Game menggunakan skrip Ruby RGSS1 (.rxdata) dan Windows Game.exe. Android tidak menjalankan Game.exe secara native."
        )
    }

    override fun stop(gameId: String) {}
    override fun pause(gameId: String) {}
    override fun resume(gameId: String) {}
}

/**
 * 10. RPG Maker VX (RGSS2) Runtime Provider
 */
class Rgss2RuntimeProvider : GameRuntimeProvider {
    override val id: String = "rgss2-runtime-v1"
    override val name: String = "RPG Maker VX (RGSS2) Runtime"
    override val version: String = "1.0.0"
    override val description: String = "Native compatibility provider for RGSS2 Ruby 1.8 bytecode (.rvdata)."
    override val supportedEngines: List<EngineType> = listOf(EngineType.RPG_MAKER_RGSS)
    override val runtimeState: RuntimeState = RuntimeState.NOT_INSTALLED
    override val isPlayableDirectly: Boolean = false
    override val architecture: String = "Ruby 1.8 RGSS2 Bytecode (.rvdata)"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = false
    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.PARTIALLY_SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "RPG Maker VX",
            detectedArchitecture = "Ruby 1.8 RGSS2 Bytecode (.rvdata)",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.PARTIALLY_SUPPORTED,
            status = RuntimeStatus.REQUIRES_PLUGIN,
            runtimeState = RuntimeState.NOT_INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Game uses RGSS2 Ruby scripts and Win32 Game.exe. Android OS requires native mkxp-z (RGSS2) library.",
            solutionOrRequirement = "Runtime Required. Memerlukan native engine mkxp-z (RGSS2) untuk menjalankan game ini di Android."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        return LaunchResult.RuntimeRequired(
            gameTitle = game.title,
            engineName = "RPG Maker VX (RGSS2)",
            runtimeRequired = "RGSS2 Compatibility Runtime (mkxp-z)",
            message = "Runtime belum tersedia. Game tidak dapat dijalankan sampai runtime yang sesuai dipasang.",
            technicalDetails = "Game menggunakan skrip Ruby RGSS2 (.rvdata) dan Windows Game.exe. Android tidak menjalankan Game.exe secara native."
        )
    }

    override fun stop(gameId: String) {}
    override fun pause(gameId: String) {}
    override fun resume(gameId: String) {}
}

/**
 * 11. RPG Maker VX Ace (RGSS3) Runtime Provider
 */
class Rgss3RuntimeProvider : GameRuntimeProvider {
    override val id: String = "rgss3-runtime-v1"
    override val name: String = "RPG Maker VX Ace (RGSS3) Runtime"
    override val version: String = "1.0.0"
    override val description: String = "Native compatibility provider for RGSS3 Ruby 1.9 bytecode (.rvdata2)."
    override val supportedEngines: List<EngineType> = listOf(EngineType.RPG_MAKER_RGSS)
    override val runtimeState: RuntimeState = RuntimeState.NOT_INSTALLED
    override val isPlayableDirectly: Boolean = false
    override val architecture: String = "Ruby 1.9 RGSS3 Bytecode (.rvdata2)"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = false
    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.PARTIALLY_SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "RPG Maker VX Ace",
            detectedArchitecture = "Ruby 1.9 RGSS3 Bytecode (.rvdata2)",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.PARTIALLY_SUPPORTED,
            status = RuntimeStatus.REQUIRES_PLUGIN,
            runtimeState = RuntimeState.NOT_INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Game uses RGSS3 Ruby scripts and Win32 Game.exe. Android OS requires native mkxp-z (RGSS3) library.",
            solutionOrRequirement = "Runtime Required. Memerlukan native engine mkxp-z (RGSS3) untuk menjalankan game ini di Android."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        return LaunchResult.RuntimeRequired(
            gameTitle = game.title,
            engineName = "RPG Maker VX Ace (RGSS3)",
            runtimeRequired = "RGSS3 Compatibility Runtime (mkxp-z)",
            message = "Runtime belum tersedia. Game tidak dapat dijalankan sampai runtime yang sesuai dipasang.",
            technicalDetails = "Game menggunakan skrip Ruby RGSS3 (.rvdata2) dan Windows Game.exe. Android tidak menjalankan Game.exe secara native."
        )
    }

    override fun stop(gameId: String) {}
    override fun pause(gameId: String) {}
    override fun resume(gameId: String) {}
}

/**
 * 12. Godot WebGL Runtime Provider
 */
class GodotWebRuntimeProvider : GameRuntimeProvider {
    override val id: String = "godot-web-v1"
    override val name: String = "Godot WebGL Runtime"
    override val version: String = "1.0.0"
    override val description: String = "Executes Godot HTML5 / WebGL exports with WebAssembly."
    override val supportedEngines: List<EngineType> = listOf(EngineType.GODOT)
    override val runtimeState: RuntimeState = RuntimeState.INSTALLED
    override val isPlayableDirectly: Boolean = true
    override val architecture: String = "Godot WASM / WebGL2"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = game.executablePath.endsWith(".html", ignoreCase = true)

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "Godot (WebGL)",
            detectedArchitecture = "Godot WASM / WebGL2",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.SUPPORTED,
            status = RuntimeStatus.AVAILABLE_READY,
            runtimeState = RuntimeState.INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Godot WebGL export detected.",
            solutionOrRequirement = "Ready for direct web execution."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        processState = RuntimeProcessState.RUNNING
        val htmlFile = File(game.gamePath, game.executablePath)
        return if (htmlFile.exists()) {
            LaunchResult.LaunchInWebView(game.id, htmlFile.toURI().toString())
        } else {
            LaunchResult.Unsupported(game.title, "Export HTML5 Godot tidak ditemukan.")
        }
    }

    override fun stop(gameId: String) {}
    override fun pause(gameId: String) {}
    override fun resume(gameId: String) {}
}

/**
 * 13. Godot Windows / PCK Runtime Provider
 */
class GodotWindowsRuntimeProvider : GameRuntimeProvider {
    override val id: String = "godot-windows-v1"
    override val name: String = "Godot PCK Runner Runtime"
    override val version: String = "1.1.0"
    override val description: String = "Handles Godot 3.x/4.x PCK packages and Windows executables."
    override val supportedEngines: List<EngineType> = listOf(EngineType.GODOT)
    override val runtimeState: RuntimeState = RuntimeState.NOT_INSTALLED
    override val isPlayableDirectly: Boolean = false
    override val architecture: String = "Godot Bytecode / PCK Archive"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = false
    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.PARTIALLY_SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "Godot Engine",
            detectedArchitecture = "Godot Bytecode / PCK Archive",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.PARTIALLY_SUPPORTED,
            status = RuntimeStatus.EXPERIMENTAL,
            runtimeState = RuntimeState.NOT_INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Godot PCK resource container detected without WebGL export.",
            solutionOrRequirement = "Runtime Required. Export game ke HTML5 WebGL atau Android APK untuk dimainkan langsung."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        return LaunchResult.RuntimeRequired(
            gameTitle = game.title,
            engineName = "Godot Engine",
            runtimeRequired = "Godot Native Mobile Runner / WebGL Export",
            message = "Runtime belum tersedia. Game tidak dapat dijalankan sampai runtime yang sesuai dipasang.",
            technicalDetails = "File package .pck Godot terdeteksi. Silakan gunakan build HTML5/WebGL untuk dimainkan langsung."
        )
    }

    override fun stop(gameId: String) {}
    override fun pause(gameId: String) {}
    override fun resume(gameId: String) {}
}

/**
 * 14. Android Native Package Provider
 */
class AndroidPackageProvider : GameRuntimeProvider {
    override val id: String = "android-package-v1"
    override val name: String = "Android Native Package Installer"
    override val version: String = "System Standard"
    override val description: String = "Installs and launches native Android APK packages using the OS PackageInstaller."
    override val supportedEngines: List<EngineType> = listOf(EngineType.ANDROID_APK)
    override val runtimeState: RuntimeState = RuntimeState.INSTALLED
    override val isPlayableDirectly: Boolean = true
    override val architecture: String = "Android Dalvik/ART + Native NDK"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = true

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.SUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "Android APK",
            detectedArchitecture = "Android Dalvik/ART + Native NDK",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.SUPPORTED,
            status = RuntimeStatus.AVAILABLE_READY,
            runtimeState = RuntimeState.INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Valid Android APK binary. Ready for installation or launch.",
            solutionOrRequirement = "Tap PLAY to trigger launch or standard Android PackageInstaller."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        val apkFile = File(game.gamePath, game.executablePath).let {
            if (it.exists()) it else File(game.gamePath)
        }
        val apkInfo = ApkPackageHelper.parseApk(context, apkFile)
        return if (apkInfo != null) {
            if (apkInfo.isInstalled && apkInfo.launchIntent != null) {
                LaunchResult.LaunchInstalledApp(apkInfo.packageName, apkInfo.launchIntent)
            } else {
                val installIntent = ApkPackageHelper.createInstallIntent(context, apkFile)
                LaunchResult.LaunchApkInstall(installIntent.data!!, apkInfo.packageName)
            }
        } else {
            LaunchResult.Unsupported(game.title, "File APK tidak ditemukan atau corrupt.")
        }
    }

    override fun stop(gameId: String) {}
    override fun pause(gameId: String) {}
    override fun resume(gameId: String) {}
}

/**
 * 15. Windows Compatibility Layer Provider (Wine / Box64)
 */
class WindowsCompatibilityRuntimeProvider : GameRuntimeProvider {
    override val id: String = "windows-compat-v1"
    override val name: String = "Windows Compatibility Layer (Box64/Wine)"
    override val version: String = "1.0.0"
    override val description: String = "Handles generic Windows .exe/.dll executables with honest status."
    override val supportedEngines: List<EngineType> = listOf(EngineType.WINDOWS_UNKNOWN, EngineType.UNKNOWN)
    override val runtimeState: RuntimeState = RuntimeState.NOT_INSTALLED
    override val isPlayableDirectly: Boolean = false
    override val architecture: String = "x86_64 PE Binary"
    override var processState: RuntimeProcessState = RuntimeProcessState.IDLE
        private set

    override fun canRunDirectly(game: GameEntity): Boolean = false

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating = CompatibilityRating.UNSUPPORTED

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "Windows Executable (.exe)",
            detectedArchitecture = "Win32 / Win64 PE Binary",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.UNSUPPORTED,
            status = RuntimeStatus.UNSUPPORTED_ON_DEVICE,
            runtimeState = RuntimeState.NOT_INSTALLED,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Aplikasi mendeteksi file executable Windows (.exe). Sistem operasi Android tidak dapat menjalankan file .exe secara langsung.",
            solutionOrRequirement = "Windows Compatibility Runtime Required. Game Windows memerlukan emulator/container x86 terpisah (seperti Winlator) dengan Wine dan Box64."
        )
    }

    override fun launch(context: Context, game: GameEntity): LaunchResult {
        return LaunchResult.RuntimeRequired(
            gameTitle = game.title,
            engineName = "Windows Executable (.exe)",
            runtimeRequired = "Windows Compatibility Layer (Wine/Box64)",
            message = "Runtime belum tersedia. Game tidak dapat dijalankan sampai runtime yang sesuai dipasang.",
            technicalDetails = "Android tidak dapat menjalankan file Windows executable (.exe) tanpa layer emulasi/terjemahan x86 seperti Wine & Box64."
        )
    }

    override fun stop(gameId: String) {}
    override fun pause(gameId: String) {}
    override fun resume(gameId: String) {}
}

/**
 * Central registry for all game engine runtimes.
 */
object RuntimeManager {
    val providers: List<GameRuntimeProvider> = listOf(
        RpgMakerWebRuntimeProvider(),
        Html5WebRuntimeProvider(),
        UnityAndroidRuntimeProvider(),
        UnityWebRuntimeProvider(),
        UnityWindowsRuntimeProvider(),
        RenPyAndroidRuntimeProvider(),
        RenPyWebRuntimeProvider(),
        RenPyWindowsRuntimeProvider(),
        Rgss1RuntimeProvider(),
        Rgss2RuntimeProvider(),
        Rgss3RuntimeProvider(),
        GodotWebRuntimeProvider(),
        GodotWindowsRuntimeProvider(),
        AndroidPackageProvider(),
        WindowsCompatibilityRuntimeProvider()
    )

    fun getRuntimeForGame(game: GameEntity): GameRuntimeProvider {
        val type = EngineType.fromString(game.engineType)
        val exec = game.executablePath.lowercase()

        return when (type) {
            EngineType.RPG_MAKER_MV, EngineType.RPG_MAKER_MZ -> {
                providers.filterIsInstance<RpgMakerWebRuntimeProvider>().first()
            }
            EngineType.HTML -> {
                providers.filterIsInstance<Html5WebRuntimeProvider>().first()
            }
            EngineType.UNITY -> {
                when {
                    exec.endsWith(".apk") || exec.contains("libunity.so") -> providers.filterIsInstance<UnityAndroidRuntimeProvider>().first()
                    exec.endsWith(".html") -> providers.filterIsInstance<UnityWebRuntimeProvider>().first()
                    else -> providers.filterIsInstance<UnityWindowsRuntimeProvider>().first()
                }
            }
            EngineType.RENPY -> {
                when {
                    exec.endsWith(".apk") || exec.contains("librenpy.so") -> providers.filterIsInstance<RenPyAndroidRuntimeProvider>().first()
                    exec.endsWith(".html") -> providers.filterIsInstance<RenPyWebRuntimeProvider>().first()
                    else -> providers.filterIsInstance<RenPyWindowsRuntimeProvider>().first()
                }
            }
            EngineType.RPG_MAKER_RGSS -> {
                when {
                    game.engineVersion.contains("RGSS1", ignoreCase = true) || game.engineVersion.contains("XP", ignoreCase = true) ->
                        providers.filterIsInstance<Rgss1RuntimeProvider>().first()
                    game.engineVersion.contains("RGSS2", ignoreCase = true) || game.engineVersion.contains("VX", ignoreCase = true) && !game.engineVersion.contains("Ace", ignoreCase = true) ->
                        providers.filterIsInstance<Rgss2RuntimeProvider>().first()
                    else ->
                        providers.filterIsInstance<Rgss3RuntimeProvider>().first()
                }
            }
            EngineType.GODOT -> {
                if (exec.endsWith(".html")) providers.filterIsInstance<GodotWebRuntimeProvider>().first()
                else providers.filterIsInstance<GodotWindowsRuntimeProvider>().first()
            }
            EngineType.ANDROID_APK -> {
                providers.filterIsInstance<AndroidPackageProvider>().first()
            }
            EngineType.WINDOWS_UNKNOWN, EngineType.UNKNOWN -> {
                providers.filterIsInstance<WindowsCompatibilityRuntimeProvider>().first()
            }
        }
    }

    fun getRuntimeById(id: String): GameRuntimeProvider? {
        return providers.find { it.id == id }
    }
}
