package com.example.engine

import com.example.model.EngineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineDetectorTest {

    @Test
    fun testRpgMakerMvDetection() {
        val files = listOf(
            "www/index.html",
            "www/package.json",
            "www/js/rpg_core.js",
            "www/js/rpg_managers.js",
            "www/data/System.json",
            "www/img/characters/Actor1.png",
            "www/audio/bgm/Theme1.ogg"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.RPG_MAKER_MV, result.engineType)
        assertTrue(result.isDirectlyPlayable)
        assertEquals("RPG Maker MV/MZ Web Runtime", result.runtimeRequired)
    }

    @Test
    fun testRpgMakerMzDetection() {
        val files = listOf(
            "index.html",
            "package.json",
            "js/rmmz_core.js",
            "js/rmmz_managers.js",
            "js/main.js",
            "data/System.json"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.RPG_MAKER_MZ, result.engineType)
        assertTrue(result.isDirectlyPlayable)
    }

    @Test
    fun testRpgMakerXpRgss1Detection() {
        val files = listOf(
            "Game.exe",
            "Game.ini",
            "Data/Scripts.rxdata",
            "Data/System.rxdata",
            "Graphics/Characters/001-Fighter01.png"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.RPG_MAKER_RGSS, result.engineType)
        assertEquals("RPG Maker XP", result.engineName)
        assertFalse(result.isDirectlyPlayable)
        assertTrue(result.runtimeRequired.contains("RGSS1"))
    }

    @Test
    fun testRpgMakerVxAceRgss3Detection() {
        val files = listOf(
            "Game.exe",
            "Game.ini",
            "Data/Scripts.rvdata2",
            "Data/System.rvdata2",
            "RGSS301.dll"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.RPG_MAKER_RGSS, result.engineType)
        assertEquals("RPG Maker VX Ace", result.engineName)
        assertFalse(result.isDirectlyPlayable)
        assertTrue(result.runtimeRequired.contains("RGSS3"))
    }

    @Test
    fun testRenPyDetection() {
        val files = listOf(
            "game/script.rpy",
            "game/script.rpyc",
            "game/options.rpyc",
            "game/screens.rpyc",
            "renpy/common/00start.rpy"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.RENPY, result.engineType)
        assertFalse(result.isDirectlyPlayable)
        assertTrue(result.runtimeRequired.contains("Ren'Py"))
    }

    @Test
    fun testUnityWindowsDetection() {
        val files = listOf(
            "MyGame.exe",
            "UnityPlayer.dll",
            "MyGame_Data/globalgamemanagers",
            "MyGame_Data/resources.assets",
            "MyGame_Data/Managed/Assembly-CSharp.dll"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.UNITY, result.engineType)
        assertFalse(result.isDirectlyPlayable)
        assertTrue(result.runtimeRequired.contains("Windows"))
    }

    @Test
    fun testUnityWindowsX64GameAssemblyDetection() {
        val files = listOf(
            "Game.exe",
            "GameAssembly.dll",
            "UnityPlayer.dll",
            "Game_Data/il2cpp_data/Metadata/global-metadata.dat"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.UNITY, result.engineType)
        assertFalse(result.isDirectlyPlayable)
        assertTrue(result.runtimeRequired.contains("Windows"))
    }

    @Test
    fun testUnityWebGLDetection() {
        val files = listOf(
            "index.html",
            "Build/UnityLoader.js",
            "Build/game.json",
            "Build/game.data.unityweb",
            "Build/game.wasm.unityweb"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.UNITY, result.engineType)
        assertTrue(result.isDirectlyPlayable)
        assertEquals("Unity (WebGL)", result.engineName)
    }

    @Test
    fun testGodotWindowsDetection() {
        val files = listOf(
            "project.godot",
            "game.pck",
            "game.exe"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.GODOT, result.engineType)
        assertFalse(result.isDirectlyPlayable)
    }

    @Test
    fun testGenericHtml5Detection() {
        val files = listOf(
            "index.html",
            "js/game.js",
            "assets/sprites.png"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.HTML, result.engineType)
        assertTrue(result.isDirectlyPlayable)
    }

    @Test
    fun testRenPyAndroidApkDetection() {
        val files = listOf(
            "game.apk",
            "game/script.rpyc",
            "librenpy.so"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.RENPY, result.engineType)
        assertTrue(result.isDirectlyPlayable)
        assertEquals("Ren'Py (Android Native)", result.engineName)
    }

    @Test
    fun testUnityAndroidApkDetection() {
        val files = listOf(
            "mygame.apk",
            "libunity.so",
            "assets/bin/Data/resources.assets"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.UNITY, result.engineType)
        assertTrue(result.isDirectlyPlayable)
        assertEquals("Unity (Android Native)", result.engineName)
    }

    @Test
    fun testRpgMakerVxRgss2Detection() {
        val files = listOf(
            "Game.exe",
            "Game.ini",
            "Data/Scripts.rvdata",
            "Data/System.rvdata",
            "RGSS202E.dll"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.RPG_MAKER_RGSS, result.engineType)
        assertEquals("RPG Maker VX", result.engineName)
        assertFalse(result.isDirectlyPlayable)
        assertTrue(result.runtimeRequired.contains("RGSS2"))
    }

    @Test
    fun testUnknownFilesDetection() {
        val files = listOf(
            "readme.txt",
            "document.pdf",
            "notes.docx"
        )
        val result = EngineDetector.detectFromFiles(files)
        assertNotNull(result)
        assertEquals(EngineType.UNKNOWN, result.engineType)
        assertFalse(result.isDirectlyPlayable)
    }
}
