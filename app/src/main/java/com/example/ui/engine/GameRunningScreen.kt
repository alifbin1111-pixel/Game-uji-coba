package com.example.ui.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.runtime.RuntimeManager
import com.example.ui.controller.VirtualGamepadOverlay
import com.example.ui.theme.DeepVioletOnPrimary
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.MutedRose
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
import java.io.File
import java.io.FileInputStream

class GameBridgeNativeBridge(
    private val onTextReceived: (String, String) -> Unit
) {
    @JavascriptInterface
    fun onTextExtracted(text: String, lang: String) {
        onTextReceived(text, lang)
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
    val settings by viewModel.getSettingsFlow(gameId).collectAsState(initial = null)
    val profiles by viewModel.controllerProfiles.collectAsState(initial = emptyList())
    val activeProfile = profiles.firstOrNull() ?: ControllerProfileEntity(id = "default", name = "Default")

    var showQuickMenu by remember { mutableStateOf(false) }
    var showGamepad by remember { mutableStateOf(true) }
    var showTranslationOverlay by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var originalDialogueText by remember { mutableStateOf("") }
    var translatedDialogueText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }

    val runtime = remember(game) {
        game?.let { RuntimeManager.getRuntimeForGame(it) } ?: RuntimeManager.availableRuntimes.first()
    }
    val diagnostic = remember(game) {
        game?.let { runtime.getDiagnostic(context, it) }
    }

    LaunchedEffect(gameId) {
        viewModel.updateLastPlayed(gameId)
    }

    BackHandler {
        if (showQuickMenu) {
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

    // Check if game is unsupported (e.g. Windows Unity x86 executable)
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
        // 1. GAME RENDERING SURFACE (Native Android WebView / Surface)
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

                    // Native JavaScript Bridge
                    addJavascriptInterface(
                        GameBridgeNativeBridge { text, lang ->
                            originalDialogueText = text
                            isTranslating = true
                            scope.launch {
                                val translated = viewModel.translate(
                                    gameId = game.id,
                                    text = text,
                                    sourceLang = settings?.sourceLanguage ?: "ja",
                                    targetLang = settings?.targetLanguage ?: "id",
                                    provider = settings?.translationProvider ?: "GEMINI"
                                )
                                translatedDialogueText = translated
                                isTranslating = false
                            }
                        },
                        "GameBridgeNative"
                    )

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url ?: return null
                            if (url.scheme == "file" || url.toString().startsWith("file://")) {
                                try {
                                    val path = url.path ?: return null
                                    val localFile = File(path)
                                    if (localFile.exists()) {
                                        val mime = when {
                                            path.endsWith(".html") -> "text/html"
                                            path.endsWith(".js") -> "application/javascript"
                                            path.endsWith(".json") -> "application/json"
                                            path.endsWith(".png") -> "image/png"
                                            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
                                            path.endsWith(".ogg") -> "audio/ogg"
                                            path.endsWith(".mp3") -> "audio/mpeg"
                                            path.endsWith(".wasm") -> "application/wasm"
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
                        loadUrl("file://${launchFile.absolutePath}")
                    } else {
                        loadDataWithBaseURL(null, "<html><body style='background:#111;color:#fff;display:flex;align-items:center;justify-content:center;height:100vh;'><h2>Game file not found</h2></body></html>", "text/html", "UTF-8", null)
                    }
                }
            },
            update = { view ->
                webViewRef = view
            }
        )

        // 2. VIRTUAL GAMEPAD OVERLAY
        if (showGamepad && (settings?.virtualControllerEnabled != false)) {
            VirtualGamepadOverlay(
                profile = activeProfile,
                onButtonPress = { btn ->
                    val jsCall = when (btn) {
                        "UP" -> "if(window.GameBridgeController) window.GameBridgeController.pressUp(); else window.dispatchEvent(new KeyboardEvent('keydown', {key:'ArrowUp'}));"
                        "DOWN" -> "if(window.GameBridgeController) window.GameBridgeController.pressDown(); else window.dispatchEvent(new KeyboardEvent('keydown', {key:'ArrowDown'}));"
                        "LEFT" -> "if(window.GameBridgeController) window.GameBridgeController.pressLeft(); else window.dispatchEvent(new KeyboardEvent('keydown', {key:'ArrowLeft'}));"
                        "RIGHT" -> "if(window.GameBridgeController) window.GameBridgeController.pressRight(); else window.dispatchEvent(new KeyboardEvent('keydown', {key:'ArrowRight'}));"
                        "A" -> "if(window.GameBridgeController) window.GameBridgeController.pressA(); else window.dispatchEvent(new KeyboardEvent('keydown', {key:'Enter'}));"
                        "B" -> "if(window.GameBridgeController) window.GameBridgeController.pressB(); else window.dispatchEvent(new KeyboardEvent('keydown', {key:'Escape'}));"
                        "X" -> "if(window.GameBridgeController) window.GameBridgeController.pressX(); else window.dispatchEvent(new KeyboardEvent('keydown', {key:'x'}));"
                        "Y" -> "if(window.GameBridgeController) window.GameBridgeController.pressY(); else window.dispatchEvent(new KeyboardEvent('keydown', {key:'y'}));"
                        "START" -> "if(window.GameBridgeController) window.GameBridgeController.pressA();"
                        "SELECT" -> "if(window.GameBridgeController) window.GameBridgeController.pressB();"
                        else -> ""
                    }
                    if (jsCall.isNotBlank()) {
                        webViewRef?.evaluateJavascript(jsCall, null)
                    }
                }
            )
        }

        // 3. LIVE TRANSLATION OVERLAY
        if (showTranslationOverlay && (settings?.translationEnabled != false)) {
            LiveTranslationOverlay(
                originalText = originalDialogueText,
                translatedText = translatedDialogueText,
                sourceLang = settings?.sourceLanguage ?: "ja",
                targetLang = settings?.targetLanguage ?: "id",
                isTranslating = isTranslating,
                onTriggerOCR = {
                    scope.launch {
                        isTranslating = true
                        val ocrText = "勇者よ、村の宝箱を開けてみよ！(Wahai pahlawan, bukalah peti harta karun desa!)"
                        originalDialogueText = ocrText
                        val trans = viewModel.translate(
                            gameId = game.id,
                            text = ocrText,
                            sourceLang = settings?.sourceLanguage ?: "ja",
                            targetLang = settings?.targetLanguage ?: "id"
                        )
                        translatedDialogueText = trans
                        isTranslating = false
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
                .background(SophisticatedCard.copy(alpha = 0.9f))
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
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(24.dp))
                    .testTag("in_game_menu_dialog"),
                color = SophisticatedBg.copy(alpha = 0.97f)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(game.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Engine: ${game.engineType}", color = LavenderPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { showQuickMenu = false }) {
                            Icon(Icons.Default.Close, "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Menu Options
                    QuickMenuRow(
                        icon = Icons.Default.Gamepad,
                        label = if (showGamepad) "Hide Virtual Gamepad" else "Show Virtual Gamepad",
                        tint = LavenderPrimary
                    ) {
                        showGamepad = !showGamepad
                        showQuickMenu = false
                    }

                    QuickMenuRow(
                        icon = Icons.Default.Translate,
                        label = if (showTranslationOverlay) "Hide Live Translation" else "Show Live Translation",
                        tint = LavenderPrimary
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
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SophisticatedCard)
            .border(1.dp, SophisticatedBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SophisticatedBadge),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
