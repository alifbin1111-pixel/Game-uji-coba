package com.example.runtime

import com.example.model.CompatibilityRating
import com.example.model.EngineType
import com.example.model.GameEntity
import com.example.model.RuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRuntimeTest {

    @Test
    fun testRpgMakerMvRuntimeProvider() {
        val game = GameEntity(
            id = "mv_1",
            title = "Test MV Game",
            engineType = EngineType.RPG_MAKER_MV.name,
            engineVersion = "MV 1.6.2",
            gamePath = "/fake/mv",
            executablePath = "index.html",
            confidence = 1.0f
        )
        val provider = RuntimeManager.getRuntimeForGame(game)
        assertNotNull(provider)
        assertTrue(provider is RpgMakerWebRuntimeProvider)
        assertEquals(RuntimeState.INSTALLED, provider.runtimeState)
        assertTrue(provider.canRunDirectly(game))
    }

    @Test
    fun testUnityWindowsRuntimeHonesty() {
        val game = GameEntity(
            id = "unity_win",
            title = "Unity Windows Game",
            engineType = EngineType.UNITY.name,
            engineVersion = "Unity 2022.3",
            gamePath = "/fake/unity",
            executablePath = "Game.exe",
            confidence = 0.98f
        )
        val provider = RuntimeManager.getRuntimeForGame(game)
        assertNotNull(provider)
        assertTrue(provider is UnityWindowsRuntimeProvider)
        assertEquals(RuntimeState.NOT_INSTALLED, provider.runtimeState)
        assertFalse(provider.canRunDirectly(game))
    }

    @Test
    fun testRenPyWindowsRuntimeHonesty() {
        val game = GameEntity(
            id = "renpy_win",
            title = "RenPy VN",
            engineType = EngineType.RENPY.name,
            engineVersion = "Ren'Py 8.1",
            gamePath = "/fake/renpy",
            executablePath = "game/script.rpyc",
            confidence = 0.95f
        )
        val provider = RuntimeManager.getRuntimeForGame(game)
        assertNotNull(provider)
        assertTrue(provider is RenPyWindowsRuntimeProvider)
        assertEquals(RuntimeState.NOT_INSTALLED, provider.runtimeState)
        assertFalse(provider.canRunDirectly(game))
    }

    @Test
    fun testRgss1XpRuntimeHonesty() {
        val game = GameEntity(
            id = "rgss1_game",
            title = "Pokemon Essentials Game",
            engineType = EngineType.RPG_MAKER_RGSS.name,
            engineVersion = "RGSS1 (Ruby 1.8)",
            gamePath = "/fake/xp",
            executablePath = "Game.exe",
            confidence = 0.95f
        )
        val provider = RuntimeManager.getRuntimeForGame(game)
        assertNotNull(provider)
        assertTrue(provider is Rgss1RuntimeProvider)
        assertEquals(RuntimeState.NOT_INSTALLED, provider.runtimeState)
        assertFalse(provider.canRunDirectly(game))
    }

    @Test
    fun testRgss3VxAceRuntimeHonesty() {
        val game = GameEntity(
            id = "rgss3_game",
            title = "VX Ace Game",
            engineType = EngineType.RPG_MAKER_RGSS.name,
            engineVersion = "RGSS3 (Ruby 1.9)",
            gamePath = "/fake/vxace",
            executablePath = "Game.exe",
            confidence = 0.96f
        )
        val provider = RuntimeManager.getRuntimeForGame(game)
        assertNotNull(provider)
        assertTrue(provider is Rgss3RuntimeProvider)
        assertEquals(RuntimeState.NOT_INSTALLED, provider.runtimeState)
        assertFalse(provider.canRunDirectly(game))
    }

    @Test
    fun testUnityAndroidApkProvider() {
        val game = GameEntity(
            id = "unity_apk",
            title = "Unity Mobile Game",
            engineType = EngineType.UNITY.name,
            engineVersion = "Unity 2021",
            gamePath = "/fake/unity.apk",
            executablePath = "unity.apk",
            confidence = 0.99f
        )
        val provider = RuntimeManager.getRuntimeForGame(game)
        assertNotNull(provider)
        assertTrue(provider is UnityAndroidRuntimeProvider)
        assertEquals(RuntimeState.INSTALLED, provider.runtimeState)
        assertTrue(provider.canRunDirectly(game))
    }

    @Test
    fun testRgss2VxRuntimeHonesty() {
        val game = GameEntity(
            id = "rgss2_game",
            title = "VX Game",
            engineType = EngineType.RPG_MAKER_RGSS.name,
            engineVersion = "RGSS2 (Ruby 1.8)",
            gamePath = "/fake/vx",
            executablePath = "Game.exe",
            confidence = 0.95f
        )
        val provider = RuntimeManager.getRuntimeForGame(game)
        assertNotNull(provider)
        assertTrue(provider is Rgss2RuntimeProvider)
        assertEquals(RuntimeState.NOT_INSTALLED, provider.runtimeState)
        assertFalse(provider.canRunDirectly(game))
    }

    @Test
    fun testUnityWebGLRuntimeProvider() {
        val game = GameEntity(
            id = "unity_webgl",
            title = "Unity Web Game",
            engineType = EngineType.UNITY.name,
            engineVersion = "Unity WebGL",
            gamePath = "/fake/unity_web",
            executablePath = "index.html",
            confidence = 0.97f
        )
        val provider = RuntimeManager.getRuntimeForGame(game)
        assertNotNull(provider)
        assertTrue(provider is UnityWebRuntimeProvider)
        assertEquals(RuntimeState.INSTALLED, provider.runtimeState)
        assertTrue(provider.canRunDirectly(game))
    }

    @Test
    fun testWindowsCompatibilityRuntimeHonesty() {
        val game = GameEntity(
            id = "win_exe",
            title = "Generic Windows Executable",
            engineType = EngineType.WINDOWS_UNKNOWN.name,
            engineVersion = "Win32 PE",
            gamePath = "/fake/winexe",
            executablePath = "Game.exe",
            confidence = 0.5f
        )
        val provider = RuntimeManager.getRuntimeForGame(game)
        assertNotNull(provider)
        assertTrue(provider is WindowsCompatibilityRuntimeProvider)
        assertEquals(RuntimeState.NOT_INSTALLED, provider.runtimeState)
        assertFalse(provider.canRunDirectly(game))
    }
}
