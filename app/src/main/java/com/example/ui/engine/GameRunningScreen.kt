package com.example.ui.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.AppDatabase
import com.example.data.local.GameRepository
import com.example.model.GameEntity
import com.example.runtime.RuntimeManager
import com.example.translation.OCRManager
import com.example.translation.TranslationManager
import com.example.viewmodel.GameBridgeViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * GameRunningScreen - Handles game execution via WebView for HTML5/RPG Maker games
 * Supports:
 * - HTML5/WebGL rendering
 * - RPG Maker MV/MZ execution
 * - Touch input & keyboard events
 * - Virtual gamepad integration
 * - Live translation hooks
 * - Auto landscape orientation
 * - Save/load game state via LocalStorage/IndexedDB
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameRunningScreen(
    gameId: String,
    viewModel: GameBridgeViewModel,
    onExitGame: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var game by remember { mutableStateOf<GameEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Load game data
    LaunchedEffect(gameId) {
        coroutineScope.launch {
            game = viewModel.repository.getGameById(gameId)
            if (game == null) {
                errorMessage = "Game not found: $gameId"
            }
            isLoading = false
            
            // Update last played timestamp
            viewModel.updateLastPlayed(gameId)
        }
    }
    
    // Set landscape orientation when screen loads
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        onDispose {
            // Restore previous orientation when exiting
            activity?.requestedOrientation = previousOrientation
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = androidx.compose.foundation.layout.Center
        ) {
            Text("Loading game...", color = Color.White)
        }
        return
    }

    if (errorMessage != null || game == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = androidx.compose.foundation.layout.Center
        ) {
            Text(errorMessage ?: "Unknown error", color = Color.Red)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game!!.title) },
                navigationIcon = {
                    IconButton(onClick = onExitGame) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit game")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            GameWebViewContent(
                game = game!!,
                context = context,
                viewModel = viewModel,
                onExit = onExitGame,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GameWebViewContent(
    game: GameEntity,
    context: Context,
    viewModel: GameBridgeViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    val ocrManager = remember { OCRManager() }
    val translationManager = remember { TranslationManager(viewModel.repository) }

    // Determine game runtime
    val runtime = remember {
        RuntimeManager.getRuntimeForGame(game)
    }

    // Load game settings
    var settings by remember { mutableStateOf(viewModel.repository.getControllerProfile("default")) }
    
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            settings = viewModel.repository.getSettings(game.id)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        url: String?
                    ): Boolean {
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Inject virtual gamepad bridge
                        injectGamepadBridge(view)
                        // Inject translation hook
                        injectTranslationBridge(view)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(
                        consoleMessage: android.webkit.ConsoleMessage?
                    ): Boolean {
                        consoleMessage?.let {
                            android.util.Log.d(
                                "GameWebView",
                                "${it.message()} (${it.lineNumber()}:${it.sourceId()})"
                            )
                        }
                        return true
                    }
                }

                // Add JavaScript interface for native communication
                addJavascriptInterface(
                    GameBridgeJsInterface(
                        context = ctx,
                        game = game,
                        viewModel = viewModel,
                        ocrManager = ocrManager,
                        translationManager = translationManager,
                        onExit = onExit
                    ),
                    "GameBridgeNative"
                )

                webView = this
            }
        },
        update = { webViewInstance ->
            val gamePath = File(game.gamePath)
            val indexHtmlPath = File(gamePath, game.executablePath)

            if (indexHtmlPath.exists()) {
                val fileUri = Uri.fromFile(indexHtmlPath).toString()
                webViewInstance.loadUrl(fileUri)
            } else {
                // Fallback: try loading index.html from game directory
                val indexFallback = File(gamePath, "index.html")
                if (indexFallback.exists()) {
                    webViewInstance.loadUrl(Uri.fromFile(indexFallback).toString())
                } else {
                    webViewInstance.loadData(
                        "<html><body><h1>Error</h1><p>index.html not found in game directory</p></body></html>",
                        "text/html",
                        "UTF-8"
                    )
                }
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.loadUrl("about:blank")
            webView?.destroy()
            ocrManager.close()
        }
    }
}

@SuppressLint("JavascriptInterface")
class GameBridgeJsInterface(
    private val context: Context,
    private val game: GameEntity,
    private val viewModel: GameBridgeViewModel,
    private val ocrManager: OCRManager,
    private val translationManager: TranslationManager,
    private val onExit: () -> Unit
) {
    /**
     * Called from JavaScript when text is extracted from the game
     * Used for live translation capture
     */
    @android.webkit.JavascriptInterface
    fun onTextExtracted(text: String, language: String) {
        // Queue text for translation
        android.util.Log.d("GameBridge", "Text extracted: $text (lang: $language)")
    }

    /**
     * Called from JavaScript for virtual gamepad button press
     */
    @android.webkit.JavascriptInterface
    fun onGamepadInput(action: String, button: String) {
        android.util.Log.d("GameBridge", "Gamepad input: $action - $button")
    }

    /**
     * Called from JavaScript to save game state
     */
    @android.webkit.JavascriptInterface
    fun onGameSaved(saveData: String) {
        android.util.Log.d("GameBridge", "Game saved")
    }

    /**
     * Called from JavaScript to load game state
     */
    @android.webkit.JavascriptInterface
    fun onGameLoaded() {
        android.util.Log.d("GameBridge", "Game loaded")
    }

    /**
     * Called from JavaScript to exit game
     */
    @android.webkit.JavascriptInterface
    fun exitGame() {
        onExit()
    }

    /**
     * Get game settings (language, resolution, etc)
     */
    @android.webkit.JavascriptInterface
    fun getGameSettings(): String {
        return """
            {
                "gameId": "${game.id}",
                "title": "${game.title}",
                "engine": "${game.engineType}",
                "translationEnabled": false,
                "targetLanguage": "id"
            }
        """.trimIndent()
    }
}

/**
 * Injects virtual gamepad bridge into the game
 */
private fun injectGamepadBridge(webView: WebView?) {
    val script = """
        if (!window.GameBridgeController) {
            window.GameBridgeController = {
                pressUp: function() { GameBridgeNative.onGamepadInput('press', 'UP'); },
                pressDown: function() { GameBridgeNative.onGamepadInput('press', 'DOWN'); },
                pressLeft: function() { GameBridgeNative.onGamepadInput('press', 'LEFT'); },
                pressRight: function() { GameBridgeNative.onGamepadInput('press', 'RIGHT'); },
                pressA: function() { GameBridgeNative.onGamepadInput('press', 'A'); },
                pressB: function() { GameBridgeNative.onGamepadInput('press', 'B'); },
                pressX: function() { GameBridgeNative.onGamepadInput('press', 'X'); },
                pressY: function() { GameBridgeNative.onGamepadInput('press', 'Y'); },
                pressStart: function() { GameBridgeNative.onGamepadInput('press', 'START'); },
                pressSelect: function() { GameBridgeNative.onGamepadInput('press', 'SELECT'); }
            };
        }
    """.trimIndent()
    webView?.evaluateJavascript(script, null)
}

/**
 * Injects translation hook into the game for live text capture
 */
private fun injectTranslationBridge(webView: WebView?) {
    val script = """
        if (typeof window.GameBridgeTranslation === 'undefined') {
            window.GameBridgeTranslation = {
                capturedText: [],
                hooks: [],
                
                registerHook: function(fn) {
                    this.hooks.push(fn);
                },
                
                onTextDisplay: function(text, speaker) {
                    if (text && text.trim().length > 0) {
                        GameBridgeNative.onTextExtracted(text, 'ja');
                        this.capturedText.push({ text: text, speaker: speaker, time: Date.now() });
                    }
                },
                
                onChoiceDisplay: function(choices) {
                    if (Array.isArray(choices)) {
                        choices.forEach(function(choice) {
                            if (choice && choice.trim().length > 0) {
                                GameBridgeNative.onTextExtracted(choice, 'ja');
                            }
                        });
                    }
                }
            };
        }
    """.trimIndent()
    webView?.evaluateJavascript(script, null)
}
