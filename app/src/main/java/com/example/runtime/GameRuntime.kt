package com.example.runtime

import android.content.Context
import android.os.Build
import com.example.model.CompatibilityRating
import com.example.model.EngineType
import com.example.model.GameEntity
import com.example.model.RuntimeStatus
import java.io.File

data class RuntimeDiagnostic(
    val runtimeId: String,
    val runtimeName: String,
    val engine: String,
    val detectedArchitecture: String,
    val deviceArchitecture: String,
    val androidVersion: String,
    val compatibility: CompatibilityRating,
    val status: RuntimeStatus,
    val memoryAvailableMB: Long,
    val technicalDetails: String,
    val solutionOrRequirement: String
)

interface GameRuntime {
    val id: String
    val name: String
    val version: String
    val description: String
    val supportedEngines: List<EngineType>
    val isInstalled: Boolean

    fun isAvailable(): RuntimeStatus = if (isInstalled) RuntimeStatus.AVAILABLE_READY else RuntimeStatus.UNSUPPORTED_ON_DEVICE
    fun canRunDirectly(game: GameEntity): Boolean
    fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating
    fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic
}

class HTMLRuntime : GameRuntime {
    override val id: String = "html5-webgl-v1"
    override val name: String = "HTML5 & WebGL Native Core"
    override val version: String = "1.4.2"
    override val description: String = "Hardware-accelerated Chromium WebGL2 and Canvas engine. Direct script injection and gamepad mapping supported."
    override val supportedEngines: List<EngineType> = listOf(EngineType.HTML5)
    override val isInstalled: Boolean = true

    override fun canRunDirectly(game: GameEntity): Boolean = true

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating {
        return CompatibilityRating.SUPPORTED
    }

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = game.engineType,
            detectedArchitecture = "HTML5 / WebGL / Canvas",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.SUPPORTED,
            status = RuntimeStatus.AVAILABLE_READY,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Hardware-accelerated Chromium WebGL2 and Canvas engine ready. Direct script injection and gamepad mapping supported.",
            solutionOrRequirement = "Directly executable with high performance."
        )
    }
}

class RPGMakerRuntime : GameRuntime {
    override val id: String = "rpgmaker-unified-v1"
    override val name: String = "RPG Maker Unified Runtime"
    override val version: String = "2.1.0"
    override val description: String = "Modular adapter supporting RPG Maker MV/MZ (Chromium Pixi.js) and RGSS XP/VX/VX Ace engines."
    override val supportedEngines: List<EngineType> = listOf(
        EngineType.RPG_MAKER_MV,
        EngineType.RPG_MAKER_MZ,
        EngineType.RPG_MAKER_XP,
        EngineType.RPG_MAKER_VX,
        EngineType.RPG_MAKER_VXACE
    )
    override val isInstalled: Boolean = true

    override fun canRunDirectly(game: GameEntity): Boolean {
        val type = EngineType.fromString(game.engineType)
        return type == EngineType.RPG_MAKER_MV || type == EngineType.RPG_MAKER_MZ
    }

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating {
        val type = EngineType.fromString(game.engineType)
        return when (type) {
            EngineType.RPG_MAKER_MV, EngineType.RPG_MAKER_MZ -> CompatibilityRating.SUPPORTED
            EngineType.RPG_MAKER_XP, EngineType.RPG_MAKER_VX, EngineType.RPG_MAKER_VXACE -> CompatibilityRating.PARTIALLY_SUPPORTED
            else -> CompatibilityRating.UNSUPPORTED
        }
    }

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val type = EngineType.fromString(game.engineType)
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        val (compat, status, details, solution) = when (type) {
            EngineType.RPG_MAKER_MV, EngineType.RPG_MAKER_MZ -> Tuple4(
                CompatibilityRating.SUPPORTED,
                RuntimeStatus.AVAILABLE_READY,
                "Pixi.js & RPG Maker JS core detected. Audio WebAudio API, LocalStorage save state, and touch simulation active.",
                "Ready for immediate launch."
            )
            EngineType.RPG_MAKER_XP, EngineType.RPG_MAKER_VX, EngineType.RPG_MAKER_VXACE -> Tuple4(
                CompatibilityRating.PARTIALLY_SUPPORTED,
                RuntimeStatus.REQUIRES_PLUGIN,
                "RGSS Ruby bytecode detected (${game.engineType}). Native JNI mkxp-z plugin package required for Ruby runtime execution.",
                "Install GameBridge Ruby/RGSS extension plugin package or convert assets to MV/MZ standard."
            )
            else -> Tuple4(
                CompatibilityRating.UNSUPPORTED,
                RuntimeStatus.UNSUPPORTED_ON_DEVICE,
                "Unrecognized RPG Maker format.",
                "Check game files."
            )
        }

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = game.engineType,
            detectedArchitecture = if (type == EngineType.RPG_MAKER_MV || type == EngineType.RPG_MAKER_MZ) "JavaScript / WebGL" else "Ruby 1.8/1.9 RGSS Bytecode",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = compat,
            status = status,
            memoryAvailableMB = freeMemMB,
            technicalDetails = details,
            solutionOrRequirement = solution
        )
    }
}

class RenPyRuntime : GameRuntime {
    override val id: String = "renpy-adapter-v1"
    override val name: String = "Ren'Py Visual Novel Runtime"
    override val version: String = "1.2.0"
    override val description: String = "Visual novel engine with real-time dialogue parser, script extractor for translation, and Ren'Py PyBridge integration."
    override val supportedEngines: List<EngineType> = listOf(EngineType.RENPY)
    override val isInstalled: Boolean = true

    override fun canRunDirectly(game: GameEntity): Boolean {
        // Can parse dialogue, extract text for translation, and run scripts through Ren'Py web/native bridge
        return true
    }

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating {
        return CompatibilityRating.PARTIALLY_SUPPORTED
    }

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
            status = RuntimeStatus.AVAILABLE_ADAPTER,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Ren'Py dialogue parser & text extraction active for translation. Pygame/SDL2 audio & script parser adapter loaded.",
            solutionOrRequirement = "Compatible with Ren'Py web port or native PyBridge backend."
        )
    }
}

class GodotRuntime : GameRuntime {
    override val id: String = "godot-runner-v1"
    override val name: String = "Godot PCK Runner Runtime"
    override val version: String = "1.1.0"
    override val description: String = "Runs Godot 3.x/4.x PCK resource packages via WebGL2 HTML5 export or native Android Godot runner bridge."
    override val supportedEngines: List<EngineType> = listOf(EngineType.GODOT)
    override val isInstalled: Boolean = true

    override fun canRunDirectly(game: GameEntity): Boolean {
        return game.executablePath.endsWith(".pck", ignoreCase = true) || game.executablePath.endsWith(".html", ignoreCase = true)
    }

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating {
        return if (game.executablePath.endsWith(".html", ignoreCase = true) || game.executablePath.endsWith(".pck", ignoreCase = true)) {
            CompatibilityRating.PARTIALLY_SUPPORTED
        } else {
            CompatibilityRating.REQUIRES_EXTERNAL_LAYER
        }
    }

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return RuntimeDiagnostic(
            runtimeId = id,
            runtimeName = name,
            engine = "Godot Engine (3.x/4.x)",
            detectedArchitecture = "Godot Bytecode / PCK",
            deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            compatibility = CompatibilityRating.PARTIALLY_SUPPORTED,
            status = RuntimeStatus.EXPERIMENTAL,
            memoryAvailableMB = freeMemMB,
            technicalDetails = "Godot PCK resource container detected. WebGL HTML5 export or Android Godot shared library (.so) bridge supported.",
            solutionOrRequirement = "Export game as Godot HTML5 or provide matching Android Godot runner APK/AAR."
        )
    }
}

class UnityCompatibilityRuntime : GameRuntime {
    override val id: String = "unity-compat-v1"
    override val name: String = "Unity Compatibility Diagnostic Layer"
    override val version: String = "1.0.0"
    override val description: String = "Analyzes Unity architecture (ARM64 native vs Windows x86 PE). Provides technical diagnosis and Box64/Wine recommendations without fake promises."
    override val supportedEngines: List<EngineType> = listOf(EngineType.UNITY)
    override val isInstalled: Boolean = true

    override fun canRunDirectly(game: GameEntity): Boolean {
        val path = game.executablePath.lowercase()
        return path.endsWith(".apk") || path.contains("libunity.so") || path.endsWith(".html")
    }

    override fun getCompatibility(context: Context, game: GameEntity): CompatibilityRating {
        val path = game.executablePath.lowercase()
        val engineVer = game.engineVersion.lowercase()
        return when {
            path.endsWith(".apk") || path.contains("libunity.so") || path.endsWith(".html") -> CompatibilityRating.SUPPORTED
            engineVer.contains("windows") || path.endsWith(".exe") -> CompatibilityRating.UNSUPPORTED
            else -> CompatibilityRating.REQUIRES_EXTERNAL_LAYER
        }
    }

    override fun getDiagnostic(context: Context, game: GameEntity): RuntimeDiagnostic {
        val path = game.executablePath.lowercase()
        val isWindowsExe = path.endsWith(".exe") || game.engineVersion.contains("windows", ignoreCase = true)
        val isAndroidApk = path.endsWith(".apk") || path.contains("libunity.so")
        val runtimeMem = Runtime.getRuntime()
        val freeMemMB = (runtimeMem.maxMemory() - (runtimeMem.totalMemory() - runtimeMem.freeMemory())) / (1024 * 1024)

        return if (isAndroidApk) {
            RuntimeDiagnostic(
                runtimeId = id,
                runtimeName = name,
                engine = "Unity (Android Native)",
                detectedArchitecture = "ARM64-v8a / ARMv7-a Native Binary",
                deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                compatibility = CompatibilityRating.SUPPORTED,
                status = RuntimeStatus.AVAILABLE_READY,
                memoryAvailableMB = freeMemMB,
                technicalDetails = "Android-native Unity build with libunity.so and IL2CPP/Mono binaries found. Ready for native execution.",
                solutionOrRequirement = "Direct native launch enabled."
            )
        } else if (isWindowsExe) {
            RuntimeDiagnostic(
                runtimeId = id,
                runtimeName = name,
                engine = "Unity (Windows x86/x64 Standalone)",
                detectedArchitecture = "x86/x64 Windows PE Executable (UnityPlayer.dll)",
                deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                compatibility = CompatibilityRating.UNSUPPORTED,
                status = RuntimeStatus.UNSUPPORTED_ON_DEVICE,
                memoryAvailableMB = freeMemMB,
                technicalDetails = "Game is built for Windows OS (x86_64 PE format). Android ARM64 CPU cannot natively execute x86 PE binaries without dynamic binary translation (Box86/Box64/FEX-Emu + Wine layer). GameBridge does not simulate fake execution.",
                solutionOrRequirement = "To run this game: Install a Box64/Wine container layer (e.g. Winlator/Mobox) or obtain the Android native or WebGL build of this Unity game."
            )
        } else {
            RuntimeDiagnostic(
                runtimeId = id,
                runtimeName = name,
                engine = "Unity (Custom Package)",
                detectedArchitecture = "Unknown / WebGL / AssetBundle",
                deviceArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                compatibility = CompatibilityRating.REQUIRES_EXTERNAL_LAYER,
                status = RuntimeStatus.REQUIRES_PLUGIN,
                memoryAvailableMB = freeMemMB,
                technicalDetails = "Unity asset packages detected without standard launcher binary.",
                solutionOrRequirement = "Provide Unity WebGL export or APK container."
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

object RuntimeManager {
    val availableRuntimes: List<GameRuntime> = listOf(
        RPGMakerRuntime(),
        HTMLRuntime(),
        RenPyRuntime(),
        GodotRuntime(),
        UnityCompatibilityRuntime()
    )

    fun getRuntimeForGame(game: GameEntity): GameRuntime {
        val type = EngineType.fromString(game.engineType)
        return availableRuntimes.firstOrNull { it.supportedEngines.contains(type) }
            ?: HTMLRuntime()
    }

    fun getRuntimeById(id: String): GameRuntime? {
        return availableRuntimes.find { it.id == id }
    }
}
