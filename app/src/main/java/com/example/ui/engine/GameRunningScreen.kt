package com.example.ui.engine

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CompatibilityRating
import com.example.model.ControllerProfileEntity
import com.example.model.GameEntity
import com.example.model.GameSettingsEntity
import com.example.runtime.OrientationManager
import com.example.runtime.RuntimeManager
import com.example.translation.GameTranslationSession
import com.example.translation.MToolLiveHook
import com.example.translation.TranslationManager
import com.example.ui.controller.VirtualGamepadOverlay
import com.example.ui.theme.DeepVioletOnPrimary
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.MutedRose
import com.example.ui.theme.SoftAmber
import com.example.ui.theme.SoftMint
import com.example.ui.theme.SophisticatedBadge
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedCard
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.translation.LiveTranslationOverlay
import com.example.viewmodel.GameBridgeViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

class GameBridgeNativeBridge(
    private val onSingleCapture: (String, String) -> Unit,
    private val onBatchCapture: (String) -> Unit
) {
    @JavascriptInterface
    fun onCaptureText(text: String, contextType: String) {
        onSingleCapture(text, contextType)
    }

    @JavascriptInterface
    fun onBatchCaptureText(jsonArray: String) {
        onBatchCapture(jsonArray)
    }

    @JavascriptInterface
    fun onTextExtracted(text: String, lang: String) {
        onSingleCapture(text, "DIALOG")
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GameRunningScreen(
    gameId: String,
    viewModel: GameBridgeViewModel,
    onExitGame: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allGames by viewModel.allGames.collectAsState(initial = emptyList())
    val game = allGames.find { it.id == gameId }
    val settingsState by viewModel.getSettingsFlow(gameId).collectAsState(initial = null)
    val settings = settingsState ?: GameSettingsEntity(gameId = gameId)
    val profiles by viewModel.controllerProfiles.collectAsState(initial = emptyList())
    val activeProfile = profiles.firstOrNull() ?: ControllerProfileEntity(id = "default", name = "Default")

    var showQuickMenu by remember { mutableStateOf(false) }
    var showTranslationSettingsSheet by remember { mutableStateOf(false) }
    var showGamepad by remember { mutableStateOf(settings.virtualControllerEnabled) }
    var showTranslationOverlay by remember { mutableStateOf(settings.overlayEnabled) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var originalDialogueText by remember { mutableStateOf("") }
    var translatedDialogueText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }

    // 1. AUTOMATIC ORIENTATION MANAGEMENT
    DisposableEffect(gameId, settings.orientation) {
        val activity = context as? Activity
        val savedOrientation = OrientationManager.lockOrientationForGame(
            activity = activity,
            orientationSetting = settings.orientation,
            engineType = game?.engineType ?: ""
        )
        onDispose {
            OrientationManager.restoreOrientation(activity, savedOrientation)
        }
    }

    // 2. LIVE TRANSLATION SESSION
    val translationSession = remember(gameId, settings) {
        viewModel.translationManager.createSession(
            gameId = gameId,
            settings = settings,
            sessionScope = scope,
            onTranslationApplied = { original, translated, contextType ->
                originalDialogueText = original
                translatedDialogueText = translated
                isTranslating = false

                // Inject translated text back into in-game Canvas and RPG Maker UI objects
                webViewRef?.post {
                    val safeOrig = JSONObject.quote(original)
                    val safeTrans = JSONObject.quote(translated)
                    val safeContext = JSONObject.quote(contextType)
                    val script = """
                        if (window.__GB_ReceiveTranslation) {
                            window.__GB_ReceiveTranslation($safeOrig, $safeTrans, $safeContext);
                        }
                    """.trimIndent()
                    webViewRef?.evaluateJavascript(script, null)
                }
            }
        )
    }

    val stats by translationSession.stats.collectAsState()

    val runtime = remember(game) {
        game?.let { RuntimeManager.getRuntimeForGame(it) } ?: RuntimeManager.providers.first()
    }
    val diagnostic = remember(game) {
        game?.let { runtime.getDiagnostic(context, it) }
    }

    LaunchedEffect(gameId) {
        viewModel.updateLastPlayed(gameId)
    }

    BackHandler {
        if (showTranslationSettingsSheet) {
            showTranslationSettingsSheet = false
        } else if (showQuickMenu) {
            showQuickMenu = false
        } else {
            showQuickMenu = true
        }
    }

    if (game == null) {
        Box(modifier = Modifier.fillMaxSize().background(SophisticatedBg), contentAlignment = Alignment.Center) {
            Text("Game not found.", color = TextPrimary)
        }
        return
    }

    if (diagnostic != null && diagnostic.compatibility == CompatibilityRating.UNSUPPORTED) {
        UnsupportedGameDiagnosticDialog(
            game = game,
            diagnostic = diagnostic,
            onBack = onExitGame
        )
        return
    }

    val launchFile = remember(game) {
        val f = File(game.gamePath, game.executablePath)
        if (f.exists()) f else File(game.gamePath, "index.html")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. GAME RENDERING SURFACE (Native Android WebView)
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("game_surface_view"),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    // Native MTool JavaScript Bridge
                    addJavascriptInterface(
                        GameBridgeNativeBridge(
                            onSingleCapture = { text, contextType ->
                                isTranslating = true
                                translationSession.handleCapturedText(text, contextType)
                            },
                            onBatchCapture = { jsonArray ->
                                isTranslating = true
                                translationSession.handleBatchCapturedTexts(jsonArray)
                            }
                        ),
                        "GameBridgeNative"
                    )

                    // Build WebViewAssetLoader for secure local origin (resolves CORS, JSON, WebAudio, localStorage, IndexedDB)
                    val gameFolder = File(game.gamePath)
                    val assetLoader = WebViewAssetLoader.Builder()
                        .setDomain("game.local")
                        .addPathHandler("/game/", InternalStoragePathHandler(ctx, gameFolder))
                        .build()

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Inject Live MTool Localization Hook
                            scope.launch {
                                val initialCache = viewModel.translationManager.getInitialCacheJson(game.id, settings.targetLanguage)
                                val hookScript = MToolLiveHook.getInjectionScript(
                                    initialCacheJson = initialCache,
                                    translateUi = settings.translateUi,
                                    translateDialog = settings.translateDialog,
                                    translateMenu = settings.translateMenu,
                                    translateBattleUi = settings.translateBattleUi
                                )
                                view?.evaluateJavascript(hookScript, null)
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val reqUrl = request?.url ?: return null
                            
                            // 1. Try WebViewAssetLoader for https://game.local/game/...
                            val assetResponse = assetLoader.shouldInterceptRequest(reqUrl)
                            if (assetResponse != null) {
                                return assetResponse
                            }

                            // 2. Fallback relative path resolver for any direct local resources
                            val urlStr = reqUrl.toString()
                            if (urlStr.startsWith("https://game.local/") || urlStr.startsWith("http://game.local/") || reqUrl.scheme == "file") {
                                try {
                                    val relPath = reqUrl.path?.removePrefix("/game/")?.removePrefix("/") ?: return null
                                    val localFile = File(gameFolder, relPath)
                                    if (localFile.exists() && !localFile.isDirectory) {
                                        val mime = when {
                                            relPath.endsWith(".html", true) -> "text/html"
                                            relPath.endsWith(".js", true) -> "application/javascript"
                                            relPath.endsWith(".json", true) -> "application/json"
                                            relPath.endsWith(".png", true) -> "image/png"
                                            relPath.endsWith(".jpg", true) || relPath.endsWith(".jpeg", true) -> "image/jpeg"
                                            relPath.endsWith(".gif", true) -> "image/gif"
                                            relPath.endsWith(".ogg", true) -> "audio/ogg"
                                            relPath.endsWith(".m4a", true) -> "audio/mp4"
                                            relPath.endsWith(".mp3", true) -> "audio/mpeg"
                                            relPath.endsWith(".wav", true) -> "audio/wav"
                                            relPath.endsWith(".wasm", true) -> "application/wasm"
                                            relPath.endsWith(".css", true) -> "text/css"
                                            else -> "application/octet-stream"
                                        }
                                        return WebResourceResponse(mime, "UTF-8", FileInputStream(localFile))
                                    }
                                } catch (e: Exception) {
                                    // fallthrough
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }

                    if (launchFile.exists()) {
                        val relLaunch = launchFile.relativeToOrNull(gameFolder)?.path?.replace('\\', '/') ?: "index.html"
                        loadUrl("https://game.local/game/$relLaunch")
                    } else {
                        loadDataWithBaseURL("https://game.local/", "<html><body style='background:#111;color:#fff;display:flex;align-items:center;justify-content:center;height:100vh;'><h2>Game file not found</h2></body></html>", "text/html", "UTF-8", null)
                    }
                }
            },
            update = { view ->
                webViewRef = view
            },
            onRelease = { view ->
                try {
                    view.stopLoading()
                    view.loadUrl("about:blank")
                    view.destroy()
                } catch (e: Exception) {
                    // ignore
                }
            }
        )

        // 2. VIRTUAL GAMEPAD OVERLAY
        if (showGamepad && settings.virtualControllerEnabled) {
            VirtualGamepadOverlay(
                profile = activeProfile,
                onButtonPress = { btn ->
                    when (btn) {
                        "QUICK_SAVE" -> {
                            scope.launch {
                                viewModel.createSaveBackup(game.id, "Quick Save ${System.currentTimeMillis()}")
                            }
                        }
                        "TURBO" -> {
                            webViewRef?.evaluateJavascript(
                                "if(window.GameBridgeSpeedMultiplier === 4) window.GameBridgeSpeedMultiplier = 1; else window.GameBridgeSpeedMultiplier = 4;",
                                null
                            )
                        }
                        else -> {
                            val key = when (btn) {
                                "UP" -> "ArrowUp"
                                "DOWN" -> "ArrowDown"
                                "LEFT" -> "ArrowLeft"
                                "RIGHT" -> "ArrowRight"
                                "A" -> "Enter"
                                "B" -> "Escape"
                                "X" -> "x"
                                "Y" -> "Shift"
                                "L1", "L" -> "PageUp"
                                "R1", "R" -> "PageDown"
                                "L2" -> "q"
                                "R2" -> "w"
                                "START" -> "Enter"
                                "SELECT" -> "Escape"
                                else -> ""
                            }
                            if (key.isNotEmpty()) {
                                val js = """
                                    if(window.GameBridgeController && window.GameBridgeController.press${btn}) {
                                        window.GameBridgeController.press${btn}();
                                    } else {
                                        window.dispatchEvent(new KeyboardEvent('keydown', {key:'$key', code:'$key', bubbles:true}));
                                    }
                                """.trimIndent()
                                webViewRef?.evaluateJavascript(js, null)
                            }
                        }
                    }
                },
                onButtonRelease = { btn ->
                    val key = when (btn) {
                        "UP" -> "ArrowUp"
                        "DOWN" -> "ArrowDown"
                        "LEFT" -> "ArrowLeft"
                        "RIGHT" -> "ArrowRight"
                        "A" -> "Enter"
                        "B" -> "Escape"
                        "X" -> "x"
                        "Y" -> "Shift"
                        "L1", "L" -> "PageUp"
                        "R1", "R" -> "PageDown"
                        "L2" -> "q"
                        "R2" -> "w"
                        "START" -> "Enter"
                        "SELECT" -> "Escape"
                        else -> ""
                    }
                    if (key.isNotEmpty()) {
                        val js = "window.dispatchEvent(new KeyboardEvent('keyup', {key:'$key', code:'$key', bubbles:true}));"
                        webViewRef?.evaluateJavascript(js, null)
                    }
                }
            )
        }

        // 3. OPTIONAL LIVE TRANSLATION OVERLAY (Draggable HUD)
        if (showTranslationOverlay && settings.translationEnabled) {
            LiveTranslationOverlay(
                originalText = originalDialogueText,
                translatedText = translatedDialogueText,
                sourceLang = settings.sourceLanguage,
                targetLang = settings.targetLanguage,
                isTranslating = isTranslating,
                onTriggerOCR = {
                    scope.launch {
                        isTranslating = true
                        val wv = webViewRef
                        if (wv != null && wv.width > 0 && wv.height > 0) {
                            try {
                                val bmp = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
                                val c = Canvas(bmp)
                                wv.draw(c)
                                val ocrManager = com.example.translation.OCRManager()
                                val detectedBoxes = ocrManager.detectText(bmp, force = true)
                                if (detectedBoxes.isNotEmpty()) {
                                    val fullDetected = detectedBoxes.joinToString(" ") { it.text.trim() }.trim()
                                    if (fullDetected.isNotBlank()) {
                                        originalDialogueText = fullDetected
                                        translationSession.handleCapturedText(fullDetected, "CANVAS_UI")
                                    } else {
                                        isTranslating = false
                                        originalDialogueText = "(No text detected in current frame)"
                                    }
                                } else {
                                    isTranslating = false
                                    originalDialogueText = "(No text detected in current frame)"
                                }
                            } catch (e: Exception) {
                                isTranslating = false
                                originalDialogueText = "(OCR Capture error: ${e.message})"
                            }
                        } else {
                            isTranslating = false
                        }
                    }
                },
                onDismiss = { showTranslationOverlay = false },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // 4. FLOATING QUICK MENU TRIGGER (Top Right Corner)
        IconButton(
            onClick = { showQuickMenu = !showQuickMenu },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(SophisticatedCard.copy(alpha = 0.92f))
                .border(1.dp, SophisticatedBorder, CircleShape)
                .testTag("btn_game_quick_menu")
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Quick Menu",
                tint = LavenderPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        // 5. IN-GAME POPUP QUICK MENU
        AnimatedVisibility(
            visible = showQuickMenu,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(24.dp))
                    .testTag("in_game_menu_dialog"),
                color = SophisticatedBg.copy(alpha = 0.98f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(game.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("Live Localization: ${if (settings.translationEnabled) "Active [${settings.sourceLanguage.uppercase()}➔${settings.targetLanguage.uppercase()}]" else "Disabled"}", color = if (settings.translationEnabled) SoftMint else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { showQuickMenu = false }) {
                            Icon(Icons.Default.Close, "Close", tint = TextSecondary)
                        }
                    }

                    // Live Session Translation Telemetry
                    if (settings.translationEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SophisticatedSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Captured", color = TextSecondary, fontSize = 10.sp)
                                    Text("${stats.totalCaptured}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Translated", color = TextSecondary, fontSize = 10.sp)
                                    Text("${stats.totalTranslated}", color = SoftMint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Cache Hits", color = TextSecondary, fontSize = 10.sp)
                                    Text("${stats.cacheHits}", color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Menu Options
                    QuickMenuRow(
                        icon = Icons.Default.Translate,
                        label = "Translation Quick Settings & Toggles",
                        tint = LavenderPrimary
                    ) {
                        showQuickMenu = false
                        showTranslationSettingsSheet = true
                    }

                    QuickMenuRow(
                        icon = Icons.Default.Gamepad,
                        label = if (showGamepad) "Hide Virtual Gamepad" else "Show Virtual Gamepad",
                        tint = LavenderPrimary
                    ) {
                        showGamepad = !showGamepad
                        showQuickMenu = false
                    }

                    QuickMenuRow(
                        icon = Icons.Default.AutoAwesome,
                        label = if (showTranslationOverlay) "Hide Translation Floating HUD" else "Show Translation Floating HUD",
                        tint = SoftMint
                    ) {
                        showTranslationOverlay = !showTranslationOverlay
                        showQuickMenu = false
                    }

                    QuickMenuRow(
                        icon = Icons.Default.Save,
                        label = "Create Save State Backup",
                        tint = SoftMint
                    ) {
                        scope.launch {
                            viewModel.createSaveBackup(game.id, "Quick Save ${System.currentTimeMillis()}")
                            showQuickMenu = false
                        }
                    }

                    QuickMenuRow(
                        icon = Icons.Default.Refresh,
                        label = "Reload Runtime Engine",
                        tint = LavenderPrimary
                    ) {
                        webViewRef?.reload()
                        showQuickMenu = false
                    }

                    QuickMenuRow(
                        icon = Icons.Default.ArrowBack,
                        label = "Exit Game to Library",
                        tint = MutedRose
                    ) {
                        showQuickMenu = false
                        onExitGame()
                    }
                }
            }
        }

        // 6. IN-GAME TRANSLATION SETTINGS MODAL
        if (showTranslationSettingsSheet) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(24.dp))
                    .align(Alignment.Center)
                    .testTag("translation_settings_in_game"),
                color = SophisticatedBg.copy(alpha = 0.98f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SophisticatedBadge),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Translate, null, tint = LavenderPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("Live Translation Settings", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        IconButton(onClick = { showTranslationSettingsSheet = false }) {
                            Icon(Icons.Default.Close, "Close", tint = TextSecondary)
                        }
                    }

                    // Main Master Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Live Engine Translation", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Real-time UI, Canvas, Dialog & Menu replacement", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.translationEnabled,
                            onCheckedChange = { viewModel.saveSettings(settings.copy(translationEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = DeepVioletOnPrimary, checkedTrackColor = LavenderPrimary)
                        )
                    }

                    Divider(color = SophisticatedBorder)

                    // Granular Toggles
                    Text("Translation Categories", color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    ToggleRow(
                        title = "Translate Gameplay UI & Buttons",
                        desc = "Inventory, status, skills, item names, title screen",
                        checked = settings.translateUi,
                        onCheckedChange = { viewModel.saveSettings(settings.copy(translateUi = it)) }
                    )

                    ToggleRow(
                        title = "Translate Story Dialog & Choices",
                        desc = "Message boxes, speaker names, branching choices",
                        checked = settings.translateDialog,
                        onCheckedChange = { viewModel.saveSettings(settings.copy(translateDialog = it)) }
                    )

                    ToggleRow(
                        title = "Translate Menu & Commands",
                        desc = "Options, pause menu, inventory tabs",
                        checked = settings.translateMenu,
                        onCheckedChange = { viewModel.saveSettings(settings.copy(translateMenu = it)) }
                    )

                    ToggleRow(
                        title = "Translate Battle UI & Actions",
                        desc = "Attack, magic, guard, escape, status effects",
                        checked = settings.translateBattleUi,
                        onCheckedChange = { viewModel.saveSettings(settings.copy(translateBattleUi = it)) }
                    )

                    ToggleRow(
                        title = "Offline Translation Cache",
                        desc = "Store translations locally for instant 0ms latency",
                        checked = settings.translationCacheEnabled,
                        onCheckedChange = { viewModel.saveSettings(settings.copy(translationCacheEnabled = it)) }
                    )

                    Divider(color = SophisticatedBorder)

                    // Provider Picker
                    Text("Translation Engine Provider", color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("LOCAL" to "Offline Dict", "GOOGLE_FREE" to "Google Free", "GEMINI" to "Gemini AI").forEach { (id, label) ->
                            val isSelected = settings.translationProvider == id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) LavenderPrimary else SophisticatedSurfaceVariant)
                                    .border(1.dp, if (isSelected) LavenderPrimary else SophisticatedBorder, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.saveSettings(settings.copy(translationProvider = id)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) DeepVioletOnPrimary else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.clearTranslationCache(game.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SophisticatedSurfaceVariant, contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clear Game Translation Cache", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(desc, color = TextSecondary, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = DeepVioletOnPrimary, checkedTrackColor = LavenderPrimary)
        )
    }
}

@Composable
private fun QuickMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SophisticatedCard)
            .border(1.dp, SophisticatedBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SophisticatedBadge),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun UnsupportedGameDiagnosticDialog(
    game: GameEntity,
    diagnostic: com.example.runtime.RuntimeDiagnostic,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SophisticatedBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SophisticatedCard)
                .border(1.dp, SophisticatedBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
                .testTag("unsupported_game_dialog")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SophisticatedBadge),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Diagnostic",
                        tint = LavenderPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Engine Compatibility Advisory",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = game.title,
                        color = LavenderPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SophisticatedSurfaceVariant)
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Engine Detected: ${diagnostic.engine}", color = LavenderPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("• Target Architecture: ${diagnostic.detectedArchitecture}", color = TextSecondary, fontSize = 12.sp)
                    Text("• Host Architecture: ${diagnostic.deviceArchitecture}", color = TextSecondary, fontSize = 12.sp)
                    Text("• Host OS: ${diagnostic.androidVersion}", color = TextSecondary, fontSize = 12.sp)
                    Text("• Compatibility Status: ${diagnostic.status.name}", color = SoftMint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Reason & Technical Diagnostic:",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = diagnostic.technicalDetails,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Recommended Solution:",
                color = SoftMint,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = diagnostic.solutionOrRequirement,
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().testTag("btn_back_to_library"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LavenderPrimary,
                    contentColor = DeepVioletOnPrimary
                )
            ) {
                Text("Return to Game Library", fontWeight = FontWeight.Bold)
            }
        }
    }
}
