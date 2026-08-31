package com.example.engine

import com.example.model.EngineType
import com.example.model.GameInfo
import java.io.File

data class EngineDetectionResult(
    val engineType: EngineType,
    val engineName: String,
    val version: String,
    val platform: String,
    val confidence: Float,
    val mainExecutable: String,
    val detectedFiles: List<String>,
    val runtimeRequired: String,
    val isDirectlyPlayable: Boolean,
    val notes: String = ""
) {
    fun toGameInfo(gamePath: String, title: String, totalSizeBytes: Long = 0L): GameInfo {
        return GameInfo(
            path = gamePath,
            title = title,
            engine = engineType,
            engineName = engineName,
            version = version,
            platform = platform,
            confidence = confidence,
            detectedFiles = detectedFiles,
            runtimeRequired = runtimeRequired,
            isDirectlyPlayable = isDirectlyPlayable,
            executablePath = mainExecutable,
            fileSizeBytes = totalSizeBytes,
            notes = notes
        )
    }
}

interface EngineSignaturePlugin {
    val engineType: EngineType
    fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult?
}

// 1. RPG MAKER MV / MZ DETECTOR
class RpgMakerHtmlPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.RPG_MAKER_MV

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }
        val matchedFiles = mutableListOf<String>()

        // RPG Maker MZ check: rmmz_core.js, rmmz_managers.js, rmmz_scenes.js, main.js with MZ structure
        val hasRmmzCore = lowerFiles.any { it.contains("rmmz_core.js") }
        val hasRmmzManagers = lowerFiles.any { it.contains("rmmz_managers.js") }
        val hasSystemJson = lowerFiles.any { it.endsWith("system.json") }
        val hasMainJs = lowerFiles.any { it.endsWith("main.js") || it.contains("js/main.js") }
        val hasMzPlugins = lowerFiles.any { it.contains("rmmz_") }

        if (hasRmmzCore || hasMzPlugins || (hasSystemJson && hasRmmzManagers)) {
            val indexHtml = findIndexHtml(fileList) ?: "index.html"
            fileList.filterTo(matchedFiles) {
                it.contains("rmmz", ignoreCase = true) ||
                it.contains("System.json", ignoreCase = true) ||
                it.endsWith("package.json", ignoreCase = true) ||
                it.endsWith("index.html", ignoreCase = true)
            }
            return EngineDetectionResult(
                engineType = EngineType.RPG_MAKER_MZ,
                engineName = "RPG Maker MZ",
                version = "MZ (Chromium / PixiJS v5+)",
                platform = "Universal Web / Chromium WebGL",
                confidence = 0.98f,
                mainExecutable = indexHtml,
                detectedFiles = matchedFiles.distinct().take(6),
                runtimeRequired = "RPG Maker MV/MZ Web Runtime",
                isDirectlyPlayable = true,
                notes = "Full touch controls, virtual gamepad, and live dialogue translation hooks supported."
            )
        }

        // RPG Maker MV check: rpg_core.js, rpg_managers.js, www folder structure
        val hasRpgCore = lowerFiles.any { it.contains("rpg_core.js") }
        val hasRpgManagers = lowerFiles.any { it.contains("rpg_managers.js") }
        val hasWwwDir = lowerFiles.any { it.startsWith("www/") || it.startsWith("www\\") }
        val hasPackageJson = lowerFiles.any { it.endsWith("package.json") }

        if (hasRpgCore || (hasSystemJson && hasRpgManagers) || (hasWwwDir && hasPackageJson && hasSystemJson)) {
            val indexHtml = findIndexHtml(fileList) ?: "www/index.html"
            fileList.filterTo(matchedFiles) {
                it.contains("rpg_core", ignoreCase = true) ||
                it.contains("rpg_managers", ignoreCase = true) ||
                it.contains("System.json", ignoreCase = true) ||
                it.endsWith("package.json", ignoreCase = true) ||
                it.endsWith("index.html", ignoreCase = true) ||
                it.startsWith("www", ignoreCase = true)
            }
            return EngineDetectionResult(
                engineType = EngineType.RPG_MAKER_MV,
                engineName = "RPG Maker MV",
                version = "MV (Chromium / PixiJS v4)",
                platform = "Universal Web / Chromium WebGL",
                confidence = 0.98f,
                mainExecutable = indexHtml,
                detectedFiles = matchedFiles.distinct().take(6),
                runtimeRequired = "RPG Maker MV/MZ Web Runtime",
                isDirectlyPlayable = true,
                notes = "Full touch controls, audio WebAudio playback, and MTool live translation hooks supported."
            )
        }

        return null
    }

    private fun findIndexHtml(fileList: List<String>): String? {
        return fileList.firstOrNull { it.equals("www/index.html", ignoreCase = true) }
            ?: fileList.firstOrNull { it.equals("index.html", ignoreCase = true) || it.endsWith("/index.html", ignoreCase = true) }
    }
}

// 2. RPG MAKER XP / VX / VX ACE (RGSS1, RGSS2, RGSS3) DETECTOR
class RpgMakerRgssPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.RPG_MAKER_RGSS

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }
        val matchedFiles = mutableListOf<String>()

        val hasGameIni = lowerFiles.any { it.endsWith("game.ini") }
        val hasGameExe = lowerFiles.any { it.endsWith("game.exe") }
        val hasRvdata2 = lowerFiles.any { it.endsWith(".rvdata2") || it.contains("scripts.rvdata2") }
        val hasRvdata = lowerFiles.any { it.endsWith(".rvdata") || it.contains("scripts.rvdata") }
        val hasRxdata = lowerFiles.any { it.endsWith(".rxdata") || it.contains("scripts.rxdata") }
        val hasRgssDll = lowerFiles.any { it.contains("rgss") && it.endsWith(".dll") }

        // RGSS3 = RPG Maker VX Ace
        if (hasRvdata2 || (hasGameIni && hasRgssDll && lowerFiles.any { it.contains("rgss3") })) {
            fileList.filterTo(matchedFiles) {
                it.endsWith("game.ini", ignoreCase = true) ||
                it.endsWith("game.exe", ignoreCase = true) ||
                it.endsWith(".rvdata2", ignoreCase = true) ||
                it.contains("rgss", ignoreCase = true)
            }
            return EngineDetectionResult(
                engineType = EngineType.RPG_MAKER_RGSS,
                engineName = "RPG Maker VX Ace",
                version = "RGSS3 (Ruby 1.9)",
                platform = "Windows x86 PE Binary (Ruby 1.9 Bytecode)",
                confidence = 0.96f,
                mainExecutable = "Game.exe",
                detectedFiles = matchedFiles.distinct().take(6),
                runtimeRequired = "RGSS3 Compatibility Runtime Required",
                isDirectlyPlayable = false,
                notes = "RGSS3 scripts and rvdata2 database found. Requires native RGSS compatibility layer (mkxp-z) to execute on Android."
            )
        }

        // RGSS2 = RPG Maker VX
        if (hasRvdata || (hasGameIni && hasRgssDll && lowerFiles.any { it.contains("rgss2") })) {
            fileList.filterTo(matchedFiles) {
                it.endsWith("game.ini", ignoreCase = true) ||
                it.endsWith("game.exe", ignoreCase = true) ||
                it.endsWith(".rvdata", ignoreCase = true) ||
                it.contains("rgss", ignoreCase = true)
            }
            return EngineDetectionResult(
                engineType = EngineType.RPG_MAKER_RGSS,
                engineName = "RPG Maker VX",
                version = "RGSS2 (Ruby 1.8)",
                platform = "Windows x86 PE Binary (Ruby 1.8 Bytecode)",
                confidence = 0.95f,
                mainExecutable = "Game.exe",
                detectedFiles = matchedFiles.distinct().take(6),
                runtimeRequired = "RGSS2 Compatibility Runtime Required",
                isDirectlyPlayable = false,
                notes = "RGSS2 scripts and rvdata database found. Requires RGSS2 compatibility runtime."
            )
        }

        // RGSS1 = RPG Maker XP
        if (hasRxdata || (hasGameIni && hasRgssDll && lowerFiles.any { it.contains("rgss1") })) {
            fileList.filterTo(matchedFiles) {
                it.endsWith("game.ini", ignoreCase = true) ||
                it.endsWith("game.exe", ignoreCase = true) ||
                it.endsWith(".rxdata", ignoreCase = true) ||
                it.contains("rgss", ignoreCase = true)
            }
            return EngineDetectionResult(
                engineType = EngineType.RPG_MAKER_RGSS,
                engineName = "RPG Maker XP",
                version = "RGSS1 (Ruby 1.8)",
                platform = "Windows x86 PE Binary (Ruby 1.8 Bytecode)",
                confidence = 0.95f,
                mainExecutable = "Game.exe",
                detectedFiles = matchedFiles.distinct().take(6),
                runtimeRequired = "RGSS1 Compatibility Runtime Required",
                isDirectlyPlayable = false,
                notes = "RGSS1 scripts and rxdata database found. Requires RGSS1 compatibility runtime."
            )
        }

        return null
    }
}

// 3. REN'PY DETECTOR
class RenPyPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.RENPY

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }
        val matchedFiles = mutableListOf<String>()

        val hasRpy = lowerFiles.any { it.endsWith(".rpy") || it.endsWith(".rpyc") }
        val hasRpa = lowerFiles.any { it.endsWith(".rpa") }
        val hasRenpyFolder = lowerFiles.any { it.startsWith("renpy/") || it.startsWith("renpy\\") || it.contains("renpy/common") }
        val hasGameFolder = lowerFiles.any { it.startsWith("game/") || it.startsWith("game\\") }
        val hasLibRenpySo = lowerFiles.any { it.contains("librenpy.so") }
        val isApk = lowerFiles.any { it.endsWith(".apk") } && (hasRpy || hasRpa || hasRenpyFolder || hasLibRenpySo)
        val isWeb = lowerFiles.any { it.endsWith("index.html") || it.contains("pyodide") || it.contains("renpy-pre") } && (hasRpa || hasGameFolder)

        if (isApk || hasLibRenpySo) {
            fileList.filterTo(matchedFiles) {
                it.endsWith(".apk", ignoreCase = true) ||
                it.endsWith(".rpy", ignoreCase = true) ||
                it.endsWith(".rpyc", ignoreCase = true) ||
                it.endsWith(".rpa", ignoreCase = true) ||
                it.contains("librenpy.so", ignoreCase = true)
            }
            return EngineDetectionResult(
                engineType = EngineType.RENPY,
                engineName = "Ren'Py (Android Native)",
                version = "Ren'Py Android Native Package",
                platform = "Android Native (librenpy.so)",
                confidence = 0.99f,
                mainExecutable = fileList.firstOrNull { it.endsWith(".apk", ignoreCase = true) } ?: "librenpy.so",
                detectedFiles = matchedFiles.distinct().take(6),
                runtimeRequired = "Android Package Installer",
                isDirectlyPlayable = true,
                notes = "Ren'Py native Android APK package detected. Can be installed or launched directly."
            )
        }

        if (isWeb && lowerFiles.any { it.endsWith("index.html") }) {
            fileList.filterTo(matchedFiles) {
                it.endsWith("index.html", ignoreCase = true) ||
                it.endsWith(".rpa", ignoreCase = true) ||
                it.contains("game", ignoreCase = true)
            }
            val indexHtml = fileList.firstOrNull { it.endsWith("index.html", ignoreCase = true) } ?: "index.html"
            return EngineDetectionResult(
                engineType = EngineType.RENPY,
                engineName = "Ren'Py (WebGL)",
                version = "Ren'Py Web (Pyodide / WebAssembly)",
                platform = "Universal Web / WebAssembly",
                confidence = 0.97f,
                mainExecutable = indexHtml,
                detectedFiles = matchedFiles.distinct().take(6),
                runtimeRequired = "HTML5 / WebGL Native Core",
                isDirectlyPlayable = true,
                notes = "Ren'Py Web export detected. Playable directly via Web runtime."
            )
        }

        if (hasRpy || hasRpa || (hasRenpyFolder && hasGameFolder)) {
            fileList.filterTo(matchedFiles) {
                it.endsWith(".rpy", ignoreCase = true) ||
                it.endsWith(".rpyc", ignoreCase = true) ||
                it.endsWith(".rpa", ignoreCase = true) ||
                it.startsWith("renpy", ignoreCase = true) ||
                it.startsWith("game", ignoreCase = true)
            }
            val isPy3 = lowerFiles.any { it.contains("py3") }
            val pyVer = if (isPy3) "Ren'Py 8 (Python 3.x)" else "Ren'Py 7 (Python 2.7)"
            val mainExe = fileList.firstOrNull { it.endsWith(".py", ignoreCase = true) || it.endsWith(".exe", ignoreCase = true) || it.endsWith(".sh", ignoreCase = true) } ?: "game/"

            return EngineDetectionResult(
                engineType = EngineType.RENPY,
                engineName = "Ren'Py",
                version = pyVer,
                platform = "Python / Cython / SDL2",
                confidence = 0.95f,
                mainExecutable = mainExe,
                detectedFiles = matchedFiles.distinct().take(6),
                runtimeRequired = "Ren'Py Native Runtime Required",
                isDirectlyPlayable = false,
                notes = "Ren'Py script bytecode (.rpyc) and archive packages (.rpa) detected. Requires Ren'Py Android runtime provider."
            )
        }
        return null
    }
}

// 4. UNITY DETECTOR (WINDOWS, ANDROID APK, WEBGL)
class UnityPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.UNITY

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }
        val matchedFiles = mutableListOf<String>()

        val hasUnityPlayerDll = lowerFiles.any { it.contains("unityplayer.dll") }
        val hasUnityData = lowerFiles.any { it.contains("_data") || it.contains("globalgamemanagers") || it.contains("resources.assets") || it.contains("level0") }
        val hasGameAssembly = lowerFiles.any { it.contains("gameassembly.dll") || it.contains("libil2cpp.so") }
        val hasLibUnitySo = lowerFiles.any { it.contains("libunity.so") }
        val hasUnityLoaderJs = lowerFiles.any { it.contains("unityloader.js") || it.contains(".loader.js") || it.contains(".framework.js") }
        val hasUnityWebGl = hasUnityLoaderJs || (lowerFiles.any { it.contains("build/") && it.endsWith(".wasm") } && lowerFiles.any { it.endsWith(".data") || it.endsWith(".unityweb") })

        if (hasUnityWebGl) {
            fileList.filterTo(matchedFiles) {
                it.contains("unity", ignoreCase = true) ||
                it.endsWith(".wasm", ignoreCase = true) ||
                it.endsWith(".data", ignoreCase = true) ||
                it.endsWith(".loader.js", ignoreCase = true) ||
                it.endsWith("index.html", ignoreCase = true)
            }
            val indexHtml = fileList.firstOrNull { it.endsWith("index.html", ignoreCase = true) } ?: "index.html"
            return EngineDetectionResult(
                engineType = EngineType.UNITY,
                engineName = "Unity (WebGL)",
                version = "Unity WebGL (WebAssembly)",
                platform = "Universal Web / WebAssembly",
                confidence = 0.98f,
                mainExecutable = indexHtml,
                detectedFiles = matchedFiles.distinct().take(6),
                runtimeRequired = "HTML5 / WebGL Native Core",
                isDirectlyPlayable = true,
                notes = "Unity WebGL export detected. Directly executable via Web runtime."
            )
        }

        if (hasUnityPlayerDll || hasUnityData || hasGameAssembly || hasLibUnitySo) {
            fileList.filterTo(matchedFiles) {
                it.contains("unity", ignoreCase = true) ||
                it.contains("_data", ignoreCase = true) ||
                it.contains("globalgamemanagers", ignoreCase = true) ||
                it.contains("gameassembly", ignoreCase = true) ||
                it.endsWith(".assets", ignoreCase = true)
            }

            val isAndroidApk = lowerFiles.any { it.endsWith(".apk") } || hasLibUnitySo

            if (isAndroidApk) {
                return EngineDetectionResult(
                    engineType = EngineType.UNITY,
                    engineName = "Unity (Android Native)",
                    version = if (hasGameAssembly) "Unity IL2CPP AOT" else "Unity Mono Runtime",
                    platform = "Android Native (ARM64 / ARMv7)",
                    confidence = 0.99f,
                    mainExecutable = fileList.firstOrNull { it.endsWith(".apk", ignoreCase = true) } ?: "libunity.so",
                    detectedFiles = matchedFiles.distinct().take(6),
                    runtimeRequired = "Android Package Installer",
                    isDirectlyPlayable = true,
                    notes = "Unity native Android package (.apk) detected. Can be installed directly via Android system installer."
                )
            } else {
                return EngineDetectionResult(
                    engineType = EngineType.UNITY,
                    engineName = "Unity (Windows Standalone)",
                    version = if (hasGameAssembly) "Unity IL2CPP (x86_64 PE)" else "Unity Mono (x86_64 PE)",
                    platform = "Windows x86/x64 Standalone",
                    confidence = 0.98f,
                    mainExecutable = fileList.firstOrNull { it.endsWith(".exe", ignoreCase = true) } ?: "UnityPlayer.dll",
                    detectedFiles = matchedFiles.distinct().take(6),
                    runtimeRequired = "Windows Compatibility Runtime Required",
                    isDirectlyPlayable = false,
                    notes = "Unity Windows executable detected. Android ARM CPUs cannot natively execute x86 PE binaries without binary translation (Box86/Box64 + Wine)."
                )
            }
        }
        return null
    }
}

// 5. GODOT DETECTOR (WINDOWS, ANDROID, WEB)
class GodotPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.GODOT

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }
        val matchedFiles = mutableListOf<String>()

        val hasGodotProject = lowerFiles.any { it.endsWith("project.godot") || it.endsWith("engine.cfg") }
        val hasPck = lowerFiles.any { it.endsWith(".pck") }
        val hasGodotFile = lowerFiles.any { it.endsWith(".godot") }
        val hasHtml = lowerFiles.any { it.endsWith("index.html") }
        val hasWasm = lowerFiles.any { it.endsWith(".wasm") }
        val hasExe = lowerFiles.any { it.endsWith(".exe") }
        val hasApk = lowerFiles.any { it.endsWith(".apk") }

        if (hasGodotProject || hasPck || hasGodotFile) {
            fileList.filterTo(matchedFiles) {
                it.endsWith("project.godot", ignoreCase = true) ||
                it.endsWith(".pck", ignoreCase = true) ||
                it.endsWith(".godot", ignoreCase = true) ||
                it.endsWith("engine.cfg", ignoreCase = true)
            }

            if (hasHtml && hasWasm) {
                val indexHtml = fileList.firstOrNull { it.endsWith("index.html", ignoreCase = true) } ?: "index.html"
                return EngineDetectionResult(
                    engineType = EngineType.GODOT,
                    engineName = "Godot (Web)",
                    version = "Godot WebGL / WebAssembly",
                    platform = "Universal Web / WebAssembly",
                    confidence = 0.97f,
                    mainExecutable = indexHtml,
                    detectedFiles = matchedFiles.distinct().take(6),
                    runtimeRequired = "HTML5 / WebGL Native Core",
                    isDirectlyPlayable = true,
                    notes = "Godot HTML5/WebGL export package detected. Directly executable via Web runtime."
                )
            }

            if (hasApk) {
                val apk = fileList.firstOrNull { it.endsWith(".apk", ignoreCase = true) } ?: "game.apk"
                return EngineDetectionResult(
                    engineType = EngineType.GODOT,
                    engineName = "Godot (Android APK)",
                    version = "Godot Android Native",
                    platform = "Android Native",
                    confidence = 0.99f,
                    mainExecutable = apk,
                    detectedFiles = matchedFiles.distinct().take(6),
                    runtimeRequired = "Android Package Installer",
                    isDirectlyPlayable = true,
                    notes = "Godot Android APK package detected."
                )
            }

            val mainPck = fileList.firstOrNull { it.endsWith(".pck", ignoreCase = true) }
                ?: fileList.firstOrNull { it.endsWith("project.godot", ignoreCase = true) }
                ?: "project.godot"

            return EngineDetectionResult(
                engineType = EngineType.GODOT,
                engineName = if (hasExe) "Godot (Windows)" else "Godot Engine",
                version = if (lowerFiles.any { it.endsWith("engine.cfg") }) "Godot 2.x" else "Godot 3.x / 4.x",
                platform = if (hasExe) "Windows x86/x64 Standalone" else "Godot Bytecode / PCK Archive",
                confidence = 0.95f,
                mainExecutable = mainPck,
                detectedFiles = matchedFiles.distinct().take(6),
                runtimeRequired = if (hasExe) "Windows Compatibility Runtime Required" else "Godot Native Mobile Runner Required",
                isDirectlyPlayable = false,
                notes = "Godot PCK archive or project file found. Export as HTML5/WebGL or Android APK for direct execution."
            )
        }
        return null
    }
}

// 6. ANDROID APK DETECTOR
class AndroidApkPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.ANDROID_APK

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }
        val apkFile = fileList.firstOrNull { it.endsWith(".apk", ignoreCase = true) }
        val hasManifest = lowerFiles.any { it.endsWith("androidmanifest.xml") }

        if (apkFile != null || hasManifest) {
            val detected = mutableListOf<String>()
            if (apkFile != null) detected.add(apkFile)
            if (hasManifest) detected.add(fileList.first { it.endsWith("androidmanifest.xml", ignoreCase = true) })

            return EngineDetectionResult(
                engineType = EngineType.ANDROID_APK,
                engineName = "Android Application Package (APK)",
                version = "Android Native (Dalvik/ART + Native NDK)",
                platform = "Android Native",
                confidence = 0.99f,
                mainExecutable = apkFile ?: "AndroidManifest.xml",
                detectedFiles = detected,
                runtimeRequired = "Android Package Installer",
                isDirectlyPlayable = true,
                notes = "Standard Android APK package. Can be installed and executed via Android OS PackageInstaller."
            )
        }
        return null
    }
}

// 7. HTML / WEBGL DETECTOR
class HtmlPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.HTML

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val lowerFiles = fileList.map { it.lowercase() }
        val indexHtml = fileList.firstOrNull { it.equals("index.html", ignoreCase = true) || it.endsWith("/index.html", ignoreCase = true) }

        if (indexHtml != null) {
            val matched = mutableListOf(indexHtml)
            fileList.filterTo(matched) { it.endsWith(".js", ignoreCase = true) || it.endsWith(".json", ignoreCase = true) }

            val isPhaser = lowerFiles.any { it.contains("phaser") }
            val isPixi = lowerFiles.any { it.contains("pixi") }
            val isConstruct = lowerFiles.any { it.contains("c2runtime") || it.contains("c3runtime") }

            val variant = when {
                isConstruct -> "Construct 2/3 HTML5"
                isPhaser -> "Phaser.io HTML5 Game"
                isPixi -> "PixiJS WebGL Canvas"
                else -> "HTML5 / WebGL Game"
            }

            return EngineDetectionResult(
                engineType = EngineType.HTML,
                engineName = variant,
                version = "HTML5 / WebGL2 / ECMAScript 6",
                platform = "Universal Web / Chromium",
                confidence = 0.92f,
                mainExecutable = indexHtml,
                detectedFiles = matched.distinct().take(6),
                runtimeRequired = "HTML5 / WebGL Native Core",
                isDirectlyPlayable = true,
                notes = "Hardware-accelerated Chromium WebGL2 and Canvas engine ready for direct execution."
            )
        }
        return null
    }
}

// 8. GENERIC WINDOWS EXECUTABLE DETECTOR
class GenericWindowsPlugin : EngineSignaturePlugin {
    override val engineType: EngineType = EngineType.WINDOWS_UNKNOWN

    override fun detect(gameDir: File, fileList: List<String>): EngineDetectionResult? {
        val exeFiles = fileList.filter { it.endsWith(".exe", ignoreCase = true) }
        val dllFiles = fileList.filter { it.endsWith(".dll", ignoreCase = true) }

        if (exeFiles.isNotEmpty() || dllFiles.isNotEmpty()) {
            val detected = (exeFiles + dllFiles).take(6)
            val mainExe = exeFiles.firstOrNull() ?: dllFiles.first()

            return EngineDetectionResult(
                engineType = EngineType.WINDOWS_UNKNOWN,
                engineName = "Windows Executable (.exe)",
                version = "Win32 / Win64 PE Binary",
                platform = "Windows x86/x64",
                confidence = 0.90f,
                mainExecutable = mainExe,
                detectedFiles = detected,
                runtimeRequired = "Windows Compatibility Runtime Required",
                isDirectlyPlayable = false,
                notes = "Windows binary (.exe/.dll) detected. Android cannot run Windows PE executables natively without x86 emulation and Win32 translation layers."
            )
        }
        return null
    }
}

object EngineDetector {
    private val plugins: List<EngineSignaturePlugin> = listOf(
        RpgMakerHtmlPlugin(),
        RpgMakerRgssPlugin(),
        RenPyPlugin(),
        UnityPlugin(),
        GodotPlugin(),
        AndroidApkPlugin(),
        HtmlPlugin(),
        GenericWindowsPlugin()
    )

    fun detect(gameDir: File): EngineDetectionResult {
        val fileList = mutableListOf<String>()
        collectRelativePaths(gameDir, "", fileList, maxDepth = 5, maxFiles = 500)

        for (plugin in plugins) {
            val result = plugin.detect(gameDir, fileList)
            if (result != null) {
                return result
            }
        }

        val firstFile = fileList.firstOrNull() ?: "unknown"
        return EngineDetectionResult(
            engineType = EngineType.UNKNOWN,
            engineName = "Unknown Game / Project",
            version = "Unrecognized Format",
            platform = "Unknown",
            confidence = 0.35f,
            mainExecutable = firstFile,
            detectedFiles = fileList.take(4),
            runtimeRequired = "Unsupported / Custom Adapter Required",
            isDirectlyPlayable = false,
            notes = "No recognized game engine signatures detected. Please verify game directory structure."
        )
    }

    fun detectFromFiles(fileList: List<String>): EngineDetectionResult {
        for (plugin in plugins) {
            val result = plugin.detect(File("."), fileList)
            if (result != null) {
                return result
            }
        }
        return EngineDetectionResult(
            engineType = EngineType.UNKNOWN,
            engineName = "Unknown Game / Project",
            version = "Unrecognized Format",
            platform = "Unknown",
            confidence = 0.35f,
            mainExecutable = fileList.firstOrNull() ?: "unknown",
            detectedFiles = fileList.take(4),
            runtimeRequired = "Unsupported / Custom Adapter Required",
            isDirectlyPlayable = false,
            notes = "No recognized game engine signatures detected."
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
