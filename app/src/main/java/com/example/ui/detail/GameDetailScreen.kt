package com.example.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CompatibilityRating
import com.example.model.EngineType
import com.example.model.GameSettingsEntity
import com.example.runtime.RuntimeManager
import com.example.ui.theme.DeepVioletOnPrimary
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.LavenderVioletBrush
import com.example.ui.theme.MutedRose
import com.example.ui.theme.SoftAmber
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.SoftMint
import com.example.ui.theme.SophisticatedBadge
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedCard
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.GameBridgeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    gameId: String,
    viewModel: GameBridgeViewModel,
    onBack: () -> Unit,
    onLaunchGame: (String) -> Unit,
    onOpenControllerEditor: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allGames by viewModel.allGames.collectAsState()
    val game = allGames.find { it.id == gameId }
    val settingsState by viewModel.getSettingsFlow(gameId).collectAsState(initial = null)
    val settings = settingsState ?: GameSettingsEntity(gameId = gameId)
    val backups by viewModel.getBackupsForGame(gameId).collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Settings", "Translation", "Controller", "Saves", "Diagnostics")

    val runtime = remember(game) {
        game?.let { RuntimeManager.getRuntimeForGame(it) } ?: RuntimeManager.availableRuntimes.first()
    }
    val diagnostic = remember(game) {
        game?.let { runtime.getDiagnostic(context, it) }
    }

    if (game == null) {
        Box(modifier = Modifier.fillMaxSize().background(SophisticatedBg), contentAlignment = Alignment.Center) {
            Text("Game not found.", color = TextPrimary)
        }
        return
    }

    val engine = remember(game.engineType) { EngineType.fromString(game.engineType) }

    Scaffold(
        containerColor = SophisticatedBg,
        topBar = {
            TopAppBar(
                title = { Text(game.title, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_from_detail")) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SophisticatedBg)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // 1. Hero Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(SophisticatedSurfaceVariant)
                                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(LavenderVioletBrush)
                                )
                                Text(
                                    text = when (engine) {
                                        EngineType.RENPY -> "🎭"
                                        EngineType.HTML5 -> "🕹️"
                                        EngineType.GODOT -> "⚡"
                                        EngineType.UNITY -> "🏔️"
                                        else -> "👾"
                                    },
                                    fontSize = 30.sp
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = game.title,
                                    color = TextPrimary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SophisticatedBadge)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = engine.displayName.uppercase(),
                                            color = LavenderPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "v${game.engineVersion}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(Modifier.height(4.dp))
                                val sizeMB = game.fileSizeBytes / (1024 * 1024.0)
                                Text(
                                    text = "Size: ${String.format(Locale.getDefault(), "%.1f MB", sizeMB)} • Accuracy: ${(game.confidence * 100).toInt()}%",
                                    color = TextSecondary.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Launch Button
                        Button(
                            onClick = { onLaunchGame(game.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_detail_launch_game"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LavenderPrimary,
                                contentColor = DeepVioletOnPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = DeepVioletOnPrimary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("LAUNCH GAME", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // 2. Tab Navigation
            item {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = SophisticatedCard,
                    contentColor = LavenderPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = LavenderPrimary
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) LavenderPrimary else TextSecondary
                                )
                            }
                        )
                    }
                }
            }

            // 3. Tab Content
            when (selectedTabIndex) {
                0 -> item { GameSettingsTab(settings = settings, onUpdate = { viewModel.saveSettings(it) }) }
                1 -> item { TranslationSettingsTab(settings = settings, onUpdate = { viewModel.saveSettings(it) }) }
                2 -> item { ControllerConfigTab(settings = settings, onUpdate = { viewModel.saveSettings(it) }, onOpenEditor = onOpenControllerEditor) }
                3 -> item {
                    SaveManagerTab(
                        backups = backups,
                        onCreateBackup = {
                            scope.launch {
                                viewModel.createSaveBackup(game.id, "Backup ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())}")
                            }
                        },
                        onDeleteBackup = { viewModel.deleteBackup(it) }
                    )
                }
                4 -> item {
                    if (diagnostic != null) {
                        DiagnosticsTab(diagnostic = diagnostic)
                    }
                }
            }
        }
    }
}

@Composable
fun GameSettingsTab(
    settings: GameSettingsEntity,
    onUpdate: (GameSettingsEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Graphics & Performance Settings", color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            // Touch controls toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Direct Touch Controls", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Allow direct on-screen touch interaction", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.touchControlsEnabled,
                    onCheckedChange = { onUpdate(settings.copy(touchControlsEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = DeepVioletOnPrimary, checkedTrackColor = LavenderPrimary)
                )
            }

            // Virtual Gamepad toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Virtual Controller Overlay", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Display D-Pad and action buttons on screen", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.virtualControllerEnabled,
                    onCheckedChange = { onUpdate(settings.copy(virtualControllerEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = DeepVioletOnPrimary, checkedTrackColor = LavenderPrimary)
                )
            }

            // FPS Limit
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("FPS Target Limit", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("${settings.fpsLimit} FPS", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.fpsLimit.toFloat(),
                    onValueChange = { onUpdate(settings.copy(fpsLimit = it.toInt())) },
                    valueRange = 30f..120f,
                    steps = 2,
                    colors = SliderDefaults.colors(thumbColor = LavenderPrimary, activeTrackColor = LavenderPrimary)
                )
            }

            // Orientation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Screen Orientation", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SophisticatedBadge)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(settings.orientation, color = LavenderPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TranslationSettingsTab(
    settings: GameSettingsEntity,
    onUpdate: (GameSettingsEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Real-Time Translation Settings", color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            // Translation Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Enable Live Translation", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Extract dialogue text & translate in real-time", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.translationEnabled,
                    onCheckedChange = { onUpdate(settings.copy(translationEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = DeepVioletOnPrimary, checkedTrackColor = LavenderPrimary)
                )
            }

            // OCR Scanning Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("OCR Visual Text Scanner", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Extract bitmap text on non-standard engines", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.ocrEnabled,
                    onCheckedChange = { onUpdate(settings.copy(ocrEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = DeepVioletOnPrimary, checkedTrackColor = LavenderPrimary)
                )
            }

            // Language pair
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Source ➔ Target Language", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Translate from Japanese to English / Indonesian", color = TextSecondary, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SophisticatedBadge)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("JA ➔ ID", color = LavenderPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Engine Provider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Translation Engine Provider", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Google Gemini AI / Rapid Translate", color = TextSecondary, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SophisticatedBadge)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(settings.translationProvider, color = LavenderPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ControllerConfigTab(
    settings: GameSettingsEntity,
    onUpdate: (GameSettingsEntity) -> Unit,
    onOpenEditor: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Virtual Controller Configuration", color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Text(
                text = "Customize layout, scale, opacity, and tactile haptic feedback for on-screen touch gamepad controls.",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Button(
                onClick = onOpenEditor,
                modifier = Modifier.fillMaxWidth().testTag("btn_open_controller_editor"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SophisticatedSurfaceVariant,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Gamepad, null, tint = LavenderPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Open Controller Visual Editor", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SaveManagerTab(
    backups: List<com.example.model.SaveBackupEntity>,
    onCreateBackup: () -> Unit,
    onDeleteBackup: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Save State & Backups", color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Button(
                    onClick = onCreateBackup,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LavenderPrimary,
                        contentColor = DeepVioletOnPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_create_save_backup")
                ) {
                    Icon(Icons.Default.Save, null, tint = DeepVioletOnPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (backups.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Text("No save backups recorded for this title.", color = TextSecondary, fontSize = 12.sp)
                }
            } else {
                backups.forEach { b ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SophisticatedSurfaceVariant)
                            .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(b.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(b.timestamp))
                            Text("Created: $dateStr", color = TextSecondary, fontSize = 11.sp)
                        }
                        IconButton(onClick = { onDeleteBackup(b.id) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MutedRose, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticsTab(
    diagnostic: com.example.runtime.RuntimeDiagnostic
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Engine Diagnostics & Architecture", color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SophisticatedSurfaceVariant)
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Compatibility Rating:", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = diagnostic.compatibility.name,
                            color = when (diagnostic.compatibility) {
                                CompatibilityRating.SUPPORTED -> SoftMint
                                CompatibilityRating.PARTIALLY_SUPPORTED -> SoftAmber
                                else -> MutedRose
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Detected Architecture:", color = TextSecondary, fontSize = 12.sp)
                        Text(diagnostic.detectedArchitecture, color = TextPrimary, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Device Architecture:", color = TextSecondary, fontSize = 12.sp)
                        Text(diagnostic.deviceArchitecture, color = TextPrimary, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Target Android Version:", color = TextSecondary, fontSize = 12.sp)
                        Text(diagnostic.androidVersion, color = TextPrimary, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Available Memory:", color = TextSecondary, fontSize = 12.sp)
                        Text("${diagnostic.memoryAvailableMB} MB", color = LavenderPrimary, fontSize = 12.sp)
                    }
                }
            }

            Text("Technical Details:", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(diagnostic.technicalDetails, color = TextSecondary, fontSize = 12.sp)

            Text("Runtime Readiness:", color = SoftMint, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(diagnostic.solutionOrRequirement, color = TextPrimary, fontSize = 12.sp)
        }
    }
}
