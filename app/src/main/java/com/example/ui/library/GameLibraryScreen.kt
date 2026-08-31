package com.example.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EngineType
import com.example.model.GameEntity
import com.example.runtime.GameLauncher
import com.example.runtime.GameRuntimeProvider
import com.example.runtime.LaunchResult
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
import com.example.ui.theme.SophisticatedCardActive
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.GameBridgeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(
    viewModel: GameBridgeViewModel,
    onSelectGame: (String) -> Unit,
    onLaunchGame: (String) -> Unit,
    onOpenRuntimes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTranslationSettings: () -> Unit,
    onOpenControllerEditor: () -> Unit
) {
    val context = LocalContext.current
    val allGames by viewModel.allGames.collectAsState()
    val filteredGames by viewModel.filteredGames.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedEngineFilter by viewModel.selectedEngineFilter.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val launchDialog by viewModel.launchDialog.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showRenameDialogForGame by remember { mutableStateOf<GameEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showImportMenu by remember { mutableStateOf(false) }

    // 1. Single File Picker (.zip, .exe, .apk, .html, .json, .pck, etc.)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val input = context.contentResolver.openInputStream(it)
                val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "game_file"
                if (input != null) {
                    if (fileName.endsWith(".zip", ignoreCase = true)) {
                        viewModel.importZipFile(input, fileName)
                    } else {
                        viewModel.importSingleFile(input, fileName)
                    }
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    // 2. Game Folder Picker (SAF Tree Document)
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val folderName = it.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/') ?: "Game Folder"
            viewModel.importFolderTree(it, folderName)
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SophisticatedBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GameBridge",
                        color = LavenderPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    )
                },
                actions = {
                    // Search / Action Circle Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SophisticatedCard)
                            .border(1.dp, SophisticatedBorder, CircleShape)
                            .clickable { /* quick search */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Avatar Circle with Gradient
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SophisticatedBadge)
                            .border(2.dp, LavenderPrimary, CircleShape)
                            .clickable { onOpenSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(LavenderVioletBrush)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SophisticatedBg)
            )
        },
        bottomBar = {
            // Sophisticated Dark Bottom Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(SophisticatedBg)
                    .border(
                        width = 1.dp,
                        color = SophisticatedBorder,
                        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Library (Active)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { /* active */ }
                            .padding(vertical = 4.dp)
                            .testTag("nav_item_library")
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(SophisticatedBadge)
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Gamepad,
                                contentDescription = "Library",
                                tint = LavenderPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text("Library", color = LavenderPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }

                    // 2. Runtimes
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenRuntimes() }
                            .padding(vertical = 4.dp)
                            .testTag("nav_item_runtimes")
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = "Runtimes",
                            tint = TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Runtimes", color = TextSecondary.copy(alpha = 0.7f), fontSize = 10.sp)
                    }

                    // 3. Translations
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenTranslationSettings() }
                            .padding(vertical = 4.dp)
                            .testTag("nav_item_translation")
                    ) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = "Translate",
                            tint = TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Translate", color = TextSecondary.copy(alpha = 0.7f), fontSize = 10.sp)
                    }

                    // 4. Settings
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenSettings() }
                            .padding(vertical = 4.dp)
                            .testTag("nav_item_settings")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Settings", color = TextSecondary.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImportMenu = true },
                containerColor = LavenderPrimary,
                contentColor = DeepVioletOnPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("fab_add_game")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Game", modifier = Modifier.size(26.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
        ) {
            // 1. Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_game_input"),
                    placeholder = { Text("Search title, engine, or keywords...", color = TextSecondary, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, "Search", tint = LavenderPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SophisticatedCard,
                        unfocusedContainerColor = SophisticatedCard,
                        focusedBorderColor = LavenderPrimary,
                        unfocusedBorderColor = SophisticatedBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            // 2. Engine Filter Chips
            item {
                val filters = listOf(
                    "ALL" to "All Engines",
                    "RPG_MAKER" to "RPG Maker",
                    "RENPY" to "Ren'Py",
                    "UNITY" to "Unity",
                    "GODOT" to "Godot",
                    "HTML" to "HTML5",
                    "WINDOWS" to "Windows"
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filters) { (key, label) ->
                        val isSelected = selectedEngineFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSelectedEngineFilter(key) },
                            label = {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SophisticatedBadge,
                                selectedLabelColor = LavenderPrimary,
                                containerColor = SophisticatedSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = LavenderPrimary,
                                borderColor = SophisticatedBorder
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("chip_filter_$key")
                        )
                    }
                }
            }

            // Loading Import Banner
            if (isImporting) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LavenderPrimary),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = LavenderPrimary)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Scanning folder & detecting engine...", color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("Analyzing file signatures, executables, and runtime compatibility...", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3. Recently Played Section
            val recentGames = allGames.filter { it.lastPlayed > 0L }.sortedByDescending { it.lastPlayed }.take(6)
            if (searchQuery.isBlank() && selectedEngineFilter == "ALL") {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "RECENTLY PLAYED",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "View all",
                            color = LavenderPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (recentGames.isNotEmpty()) {
                            items(recentGames, key = { it.id }) { game ->
                                RecentGameCard(
                                    game = game,
                                    onPlay = { viewModel.handlePlayClick(game, onDirectLaunch = onLaunchGame) },
                                    onSelect = { onSelectGame(game.id) }
                                )
                            }
                        } else {
                            items(allGames.take(3), key = { "recent_${it.id}" }) { game ->
                                RecentGameCard(
                                    game = game,
                                    onPlay = { viewModel.handlePlayClick(game, onDirectLaunch = onLaunchGame) },
                                    onSelect = { onSelectGame(game.id) }
                                )
                            }
                        }

                        // Quick Import item
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable { showImportMenu = true }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(SophisticatedSurfaceVariant)
                                        .border(1.dp, SophisticatedBorder, RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Import",
                                        tint = TextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Import",
                                    color = TextSecondary.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 4. All Games Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Library",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SophisticatedSurfaceVariant)
                            .border(1.dp, SophisticatedBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Total: ${filteredGames.size}", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            // Empty state
            if (filteredGames.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Gamepad, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No games found", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Choose a game folder or file to scan and detect engine.", color = TextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { showImportMenu = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LavenderPrimary,
                                    contentColor = DeepVioletOnPrimary
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Scan / Import Game", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Game Items List
                items(filteredGames, key = { it.id }) { game ->
                    GameListItem(
                        game = game,
                        onPlay = { viewModel.handlePlayClick(game, onDirectLaunch = onLaunchGame) },
                        onSelect = { onSelectGame(game.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(game.id, game.isFavorite) },
                        onRename = {
                            showRenameDialogForGame = game
                            renameText = game.title
                        },
                        onDelete = { viewModel.deleteGame(game.id) }
                    )
                }
            }
        }
    }

    // Honest Runtime Required Dialog
    if (launchDialog != null) {
        val dialog = launchDialog!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissLaunchDialog() },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SoftAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, null, tint = SoftAmber, modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text(
                    text = "Runtime Belum Tersedia",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = dialog.message,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SophisticatedSurfaceVariant)
                            .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Engine: ${dialog.engineName}", color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Dibutuhkan: ${dialog.runtimeRequired}", color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text(dialog.technicalDetails, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissLaunchDialog()
                        onOpenRuntimes()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LavenderPrimary,
                        contentColor = DeepVioletOnPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Lihat Status Runtimes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLaunchDialog() }) {
                    Text("Tutup", color = TextSecondary)
                }
            },
            containerColor = SophisticatedCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Import Options Dialog (Folder, File, or Samples)
    if (showImportMenu) {
        AlertDialog(
            onDismissRequest = { showImportMenu = false },
            title = { Text("Game Scanner & Import", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Pilih sumber game untuk dipindai engine dan kompatibilitasnya:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    // 1. Folder Picker
                    Button(
                        onClick = {
                            showImportMenu = false
                            folderLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_import_folder"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LavenderPrimary,
                            contentColor = DeepVioletOnPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, null, tint = DeepVioletOnPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Pilih Folder Game (Direktori)", fontWeight = FontWeight.Bold)
                    }

                    // 2. Single File Picker
                    Button(
                        onClick = {
                            showImportMenu = false
                            fileLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_import_file"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SophisticatedSurfaceVariant,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Description, null, tint = SoftMint)
                        Spacer(Modifier.width(8.dp))
                        Text("Pilih File (.exe / .apk / .zip / .html / .json)")
                    }

                    // 3. Reload Sample Games
                    Button(
                        onClick = {
                            showImportMenu = false
                            viewModel.resetSampleGames()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_import_sample"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SophisticatedSurfaceVariant,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = SoftAmber)
                        Spacer(Modifier.width(8.dp))
                        Text("Muat Ulang Demo Games")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImportMenu = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = SophisticatedCard,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Rename Dialog
    if (showRenameDialogForGame != null) {
        val targetGame = showRenameDialogForGame!!
        AlertDialog(
            onDismissRequest = { showRenameDialogForGame = null },
            title = { Text("Rename Game", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier.fillMaxWidth().testTag("input_rename_game"),
                    label = { Text("Game Title") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = LavenderPrimary,
                        unfocusedBorderColor = SophisticatedBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameGame(targetGame.id, renameText)
                        showRenameDialogForGame = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LavenderPrimary,
                        contentColor = DeepVioletOnPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialogForGame = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SophisticatedCard,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun RecentGameCard(
    game: GameEntity,
    onPlay: () -> Unit,
    onSelect: () -> Unit
) {
    val engine = remember(game.engineType) { EngineType.fromString(game.engineType) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(84.dp)
            .clickable(onClick = onSelect)
            .testTag("recent_game_card_${game.id}")
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SophisticatedSurfaceVariant)
                .border(1.dp, SophisticatedBorder, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            val emoji = when (engine) {
                EngineType.RPG_MAKER_MV, EngineType.RPG_MAKER_MZ, EngineType.RPG_MAKER_RGSS -> "🎮"
                EngineType.RENPY -> "🌸"
                EngineType.HTML -> "🕹️"
                EngineType.GODOT -> "👾"
                EngineType.UNITY -> "🏔️"
                EngineType.ANDROID_APK -> "📱"
                EngineType.WINDOWS_UNKNOWN -> "💻"
                else -> "🎲"
            }
            Text(
                text = emoji,
                fontSize = 28.sp,
                modifier = Modifier.padding(4.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = game.title,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GameListItem(
    game: GameEntity,
    onPlay: () -> Unit,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val engine = remember(game.engineType) { EngineType.fromString(game.engineType) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onSelect)
            .testTag("game_item_${game.id}"),
        colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sophisticated Thumbnail with Subtle Gradient & Emoji
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SophisticatedBg)
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    DeepVioletOnPrimary.copy(alpha = 0.3f),
                                    LavenderPrimary.copy(alpha = 0.1f)
                                )
                            )
                        )
                )

                val emoji = when (engine) {
                    EngineType.RPG_MAKER_MV, EngineType.RPG_MAKER_MZ, EngineType.RPG_MAKER_RGSS -> "👾"
                    EngineType.RENPY -> "🎭"
                    EngineType.HTML -> "🕹️"
                    EngineType.GODOT -> "⚡"
                    EngineType.UNITY -> "🏔️"
                    EngineType.ANDROID_APK -> "📱"
                    EngineType.WINDOWS_UNKNOWN -> "💻"
                    else -> "🎲"
                }

                Text(
                    text = emoji,
                    fontSize = 26.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            // Game Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = game.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (game.isFavorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = MutedRose,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(engine.badgeColor).copy(alpha = 0.2f))
                            .border(1.dp, Color(engine.badgeColor).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = engine.displayName.uppercase(),
                            color = Color(engine.badgeColor),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (game.confidence > 0f) {
                        Text(
                            text = "${(game.confidence * 100).toInt()}% Match",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                if (game.lastPlayed > 0L) {
                    val dateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(game.lastPlayed))
                    Text(
                        text = "Played $dateFormatted",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }

            val runtimeProvider = remember(game) { RuntimeManager.getRuntimeForGame(game) }
            val isDirectlyPlayable = runtimeProvider.canRunDirectly(game)
            val isApk = game.executablePath.endsWith(".apk", ignoreCase = true)

            // Dynamic Action Button (Honest UI)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDirectlyPlayable && !isApk -> LavenderPrimary
                            isDirectlyPlayable && isApk -> SoftMint
                            else -> SoftAmber
                        }
                    )
                    .clickable(onClick = onPlay)
                    .testTag("btn_play_${game.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isDirectlyPlayable && !isApk -> Icons.Default.PlayArrow
                        isDirectlyPlayable && isApk -> Icons.Default.Android
                        else -> Icons.Default.Warning
                    },
                    contentDescription = if (isDirectlyPlayable) "Play / Launch" else "Runtime Required",
                    tint = DeepVioletOnPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Options Overflow Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("btn_options_${game.id}")
                ) {
                    Icon(Icons.Default.MoreVert, "Options", tint = TextSecondary)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SophisticatedCard)
                ) {
                    DropdownMenuItem(
                        text = { Text("Details & Settings", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Info, null, tint = LavenderPrimary) },
                        onClick = {
                            showMenu = false
                            onSelect()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (game.isFavorite) "Remove Favorite" else "Add Favorite", color = TextPrimary) },
                        leadingIcon = { Icon(if (game.isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite, null, tint = MutedRose) },
                        onClick = {
                            showMenu = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = SoftAmber) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Game", color = MutedRose) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MutedRose) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
