package com.example.engine

import com.example.model.EngineType
import java.io.File

data class EngineDetectionResult(
    val engineType: EngineType,
    val engineName: String,
    val version: String,
    val confidence: Float,
    val mainExecutable: String,
    val architecture: String = "Universal",
    val signatureFiles: List<String> = emptyList(),
    val notes: String = ""
)

interface EngineSignaturePlugin {
    val engineType: EngineType
    fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult?
}

class RpgMakerPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.RPG_MAKER_MV

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }

        // Check RPG Maker MZ
        if (lowerFiles.any { it.contains("rmmz_core.js") } ||
            (lowerFiles.any { it.contains("system.json") } && lowerFiles.any { it.contains("main.js") })) {
            return EngineDetectionResult(
                engineType = EngineType.RPG_MAKER_MZ,
                engineName = "RPG Maker MZ",
                version = "1.8.x (Chromium/Pixi.js)",
                confidence = 0.98f,
                mainExecutable = findIndexHtml(fileList) ?: "index.html",
                architecture = "HTML5 / WebGL / V8",
                signatureFiles = fileList.filter { it.contains("rmmz", ignoreCase = true) || it.contains("System.json", ignoreCase = true) },
                notes = "Native high-performance WebGL / Chromium rendering supported."
            )
        }

        // Check RPG Maker MV
        if (lowerFiles.any { it.contains("rpg_core.js") } ||
            (lowerFiles.any { it.contains("system.json") } && lowerFiles.any { it.contains("rpg_managers.js") })) {
            return EngineDetectionResult(
                engineType = EngineType.RPG_MAKER_MV,
                engineName = "RPG Maker MV",
                version = "1.6.x (Chromium/Pixi.js)",
                confidence = 0.98f,
                mainExecutable = findIndexHtml(fileList) ?: "index.html",
                architecture = "HTML5 / WebGL / Pixi.js",
                signatureFiles = fileList.filter { it.contains("rpg_core", ignoreCase = true) || it.contains("System.json", ignoreCase = true) },
                notes = "Full touch control, virtual gamepad, and live translation hook supported."
            )
        }

        // Check RPG Maker VX Ace
        if (lowerFiles.any { it.endsWith("game.rvdata2") } || lowerFiles.any { it.contains("rgss301") }) {
            return EngineDetectionResult(
                engineType = EngineType.RPG_MAKER_VXACE,
                engineName = "RPG Maker VX Ace",
                version = "RGSS3",
                confidence = 0.95f,
                mainExecutable = "Game.exe",
                architecture = "x86 PE Binary (Ruby 1.9)",
                signatureFiles = fileList.filter { it.endsWith(".rvdata2", ignoreCase = true) || it.contains("rgss3", ignoreCase = true) },
                notes = "Requires RGSS3 compatibility runtime or mkxp-z translation layer."
            )
        }

        // Check RPG Maker VX
        if (lowerFiles.any { it.endsWith("game.rvdata") } || lowerFiles.any { it.contains("rgss202") }) {
            return EngineDetectionResult(
                engineType = EngineType.RPG_MAKER_VX,
                engineName = "RPG Maker VX",
                version = "RGSS2",
                confidence = 0.95f,
                mainExecutable = "Game.exe",
                architecture = "x86 PE Binary (Ruby 1.8)",
                signatureFiles = fileList.filter { it.endsWith(".rvdata", ignoreCase = true) || it.contains("rgss2", ignoreCase = true) },
                notes = "Requires RGSS2 compatibility runtime."
            )
        }

        // Check RPG Maker XP
        if (lowerFiles.any { it.endsWith("game.rxdata") } || lowerFiles.any { it.contains("rgss104") }) {
            return EngineDetectionResult(
                engineType = EngineType.RPG_MAKER_XP,
                engineName = "RPG Maker XP",
                version = "RGSS1",
                confidence = 0.95f,
                mainExecutable = "Game.exe",
                architecture = "x86 PE Binary (Ruby 1.8)",
                signatureFiles = fileList.filter { it.endsWith(".rxdata", ignoreCase = true) || it.contains("rgss1", ignoreCase = true) },
                notes = "Requires RGSS1 compatibility runtime / mkxp translation layer."
            )
        }

        return null
    }

    private fun findIndexHtml(fileList: List<String>): String? {
        return fileList.firstOrNull { it.equals("index.html", ignoreCase = true) || it.endsWith("/index.html", ignoreCase = true) || it.endsWith("\\index.html", ignoreCase = true) }
    }
}

class RenPyPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.RENPY

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }
        val hasRpy = lowerFiles.any { it.endsWith(".rpy") || it.endsWith(".rpyc") || it.endsWith(".rpa") }
        val hasRenpyDir = lowerFiles.any { it.contains("renpy/") || it.contains("renpy\\") || it.contains("options.rpy") }

        if (hasRpy || hasRenpyDir) {
            val isPy3 = lowerFiles.any { it.contains("py3") }
            return EngineDetectionResult(
                engineType = EngineType.RENPY,
                engineName = "Ren'Py Visual Novel Engine",
                version = if (isPy3) "Ren'Py 8 (Python 3)" else "Ren'Py 7 (Python 2.7)",
                confidence = 0.96f,
                mainExecutable = fileList.firstOrNull { it.endsWith(".py", ignoreCase = true) || it.endsWith(".sh", ignoreCase = true) || it.endsWith(".exe", ignoreCase = true) } ?: "game/",
                architecture = "Python / Cython / SDL2",
                signatureFiles = fileList.filter { it.endsWith(".rpy", ignoreCase = true) || it.endsWith(".rpyc", ignoreCase = true) || it.endsWith(".rpa", ignoreCase = true) }.take(5),
                notes = "Ren'Py scripts and archive packages (.rpa) identified. Save and dialogue extraction adapter enabled."
            )
        }
        return null
    }
}

class UnityPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.UNITY

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }
        val hasUnityPlayer = lowerFiles.any { it.contains("unityplayer.dll") || it.contains("libunity.so") }
        val hasUnityData = lowerFiles.any { it.contains("globalgamemanagers") || it.contains("data.unity3d") || it.contains("unity default resources") }
        val hasManaged = lowerFiles.any { it.contains("unityengine.dll") || it.contains("assembly-csharp.dll") }

        if (hasUnityPlayer || hasUnityData || hasManaged) {
            val isAndroidNative = lowerFiles.any { it.contains("libunity.so") || it.endsWith(".apk") }
            val isIl2cpp = lowerFiles.any { it.contains("libil2cpp.so") || it.contains("gameassembly.dll") }
            val arch = when {
                isAndroidNative -> if (lowerFiles.any { it.contains("arm64") }) "Android ARM64-v8a" else "Android ARMv7"
                lowerFiles.any { it.contains("x86_64") } -> "Windows x86_64"
                else -> "Windows x86 / PE Executable"
            }

            return EngineDetectionResult(
                engineType = EngineType.UNITY,
                engineName = "Unity Engine",
                version = if (isIl2cpp) "Unity (IL2CPP Ahead-of-Time)" else "Unity (Mono Scripting Backend)",
                confidence = 0.99f,
                mainExecutable = fileList.firstOrNull { it.endsWith(".exe", ignoreCase = true) || it.endsWith(".apk", ignoreCase = true) } ?: "UnityGame",
                architecture = arch,
                signatureFiles = fileList.filter { it.contains("unity", ignoreCase = true) || it.contains("Assembly-CSharp", ignoreCase = true) }.take(5),
                notes = if (isAndroidNative) "Android native Unity package detected - ready for direct execution."
                else "Windows Unity build detected. Requires Box86/Wine or ARM64 translated runtime environment."
            )
        }
        return null
    }
}

class GodotPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.GODOT

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }
        val hasGodotProject = lowerFiles.any { it.endsWith("project.godot") || it.endsWith("engine.cfg") }
        val hasPck = lowerFiles.any { it.endsWith(".pck") }

        if (hasGodotProject || hasPck) {
            val pckFile = fileList.firstOrNull { it.endsWith(".pck", ignoreCase = true) } ?: "project.godot"
            return EngineDetectionResult(
                engineType = EngineType.GODOT,
                engineName = "Godot Engine",
                version = if (lowerFiles.any { it.endsWith("engine.cfg") }) "Godot 2.x" else "Godot 3.x / 4.x",
                confidence = 0.95f,
                mainExecutable = pckFile,
                architecture = "Godot Bytecode / PCK Archive",
                signatureFiles = fileList.filter { it.endsWith(".pck", ignoreCase = true) || it.endsWith(".godot", ignoreCase = true) },
                notes = "Godot resource package (.pck) detected. Compatible with Godot mobile runtime wrapper."
            )
        }
        return null
    }
}

class Html5Plugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.HTML5

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }
        val indexHtml = fileList.firstOrNull { it.equals("index.html", ignoreCase = true) || it.endsWith("/index.html", ignoreCase = true) || it.endsWith("\\index.html", ignoreCase = true) }

        if (indexHtml != null) {
            val isPhaser = lowerFiles.any { it.contains("phaser") }
            val isPixi = lowerFiles.any { it.contains("pixi") }
            val isConstruct = lowerFiles.any { it.contains("c2runtime") || it.contains("c3runtime") }

            val variant = when {
                isConstruct -> "Construct 2 / 3 HTML5"
                isPhaser -> "Phaser.io Game Engine"
                isPixi -> "PixiJS Canvas / WebGL"
                else -> "HTML5 / WebGL / JavaScript"
            }

            return EngineDetectionResult(
                engineType = EngineType.HTML5,
                engineName = variant,
                version = "Standard Web Standards",
                confidence = 0.90f,
                mainExecutable = indexHtml,
                architecture = "Universal (JavaScript / WebGL)",
                signatureFiles = listOf(indexHtml) + fileList.filter { it.endsWith(".js", ignoreCase = true) }.take(3),
                notes = "Direct native WebView / Chromium high-speed Canvas execution ready."
            )
        }
        return null
    }
}

class GameMakerPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.GAMEMAKER

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }
        if (lowerFiles.any { it.endsWith("data.win") || it.endsWith("audiogroup1.dat") || it.endsWith("game.unx") }) {
            return EngineDetectionResult(
                engineType = EngineType.GAMEMAKER,
                engineName = "GameMaker Studio",
                version = "GMS 1.4 / 2.x",
                confidence = 0.94f,
                mainExecutable = fileList.firstOrNull { it.endsWith(".exe", ignoreCase = true) } ?: "data.win",
                architecture = "x86 PE Binary / GMS Bytecode",
                signatureFiles = fileList.filter { it.endsWith("data.win", ignoreCase = true) || it.endsWith(".dat", ignoreCase = true) },
                notes = "Requires GameMaker Runner runtime or Wine layer."
            )
        }
        return null
    }
}

object EngineDetector {
    private val plugins: List<EngineSignaturePlugin> = listOf(
        RpgMakerPlugin(),
        RenPyPlugin(),
        UnityPlugin(),
        GodotPlugin(),
        GameMakerPlugin(),
        Html5Plugin()
    )

    fun detect(gameDir: File): EngineDetectionResult {
        val fileList = mutableListOf<String>()
        collectRelativePaths(gameDir, "", fileList, maxDepth = 4, maxFiles = 300)

        for (plugin in plugins) {
            val result = plugin.detect(gameDir, fileList)
            if (result != null) {
                return result
            }
        }

        // Fallback for custom or unknown
        val executable = fileList.firstOrNull { it.endsWith(".exe", ignoreCase = true) || it.endsWith(".apk", ignoreCase = true) || it.endsWith(".html", ignoreCase = true) } ?: "unknown"
        return EngineDetectionResult(
            engineType = EngineType.CUSTOM,
            engineName = "Custom / Native Executable",
            version = "Generic",
            confidence = 0.50f,
            mainExecutable = executable,
            architecture = "Unknown",
            signatureFiles = fileList.take(3),
            notes = "No standard engine signatures detected. Manual adapter configuration required."
        )
    }

    private fun collectRelativePaths(dir: File, currentPrefix: String, result: MutableList<String>, maxDepth: Int, maxFiles: Int) {
        if (maxDepth <= 0 || result.size >= maxFiles || !dir.exists() || !dir.isDirectory) return

        val files = dir.listFiles() ?: return
        for (f in files) {
            val relPath = if (currentPrefix.isEmpty()) f.name else "$currentPrefix/${f.name}"
            result.add(relPath)
            if (f.isDirectory) {
                collectRelativePaths(f, relPath, result, maxDepth - 1, maxFiles)
            }
            if (result.size >= maxFiles) break
        }
    }
}
