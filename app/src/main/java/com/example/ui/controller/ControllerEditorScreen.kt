package com.example.ui.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ControllerProfileEntity
import com.example.ui.theme.DeepVioletOnPrimary
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.SoftMint
import com.example.ui.theme.SophisticatedBadge
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedCard
import com.example.ui.theme.SophisticatedSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.GameBridgeViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerEditorScreen(
    viewModel: GameBridgeViewModel,
    onBack: () -> Unit
) {
    val profiles by viewModel.controllerProfiles.collectAsState()
    val initialProfile = profiles.firstOrNull() ?: ControllerProfileEntity(id = "default", name = "Standard")

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Themes & Style, 1: Sliders & Layout, 2: Buttons & Features

    var themeName by remember { mutableStateOf(initialProfile.controllerTheme) }
    var dpadMode by remember { mutableStateOf(initialProfile.dpadMode) }
    var opacity by remember { mutableFloatStateOf(initialProfile.opacity) }
    var scale by remember { mutableFloatStateOf(initialProfile.scale) }
    var buttonSpacing by remember { mutableFloatStateOf(initialProfile.buttonSpacing) }
    var haptic by remember { mutableStateOf(initialProfile.hapticFeedback) }
    var showShoulderL2R2 by remember { mutableStateOf(initialProfile.showShoulderL2R2) }
    var showTurbo by remember { mutableStateOf(initialProfile.showTurbo) }
    var showQuickSave by remember { mutableStateOf(initialProfile.showQuickSave) }

    var lastPressedButton by remember { mutableStateOf("No input detected") }
    var analogTelemetry by remember { mutableStateOf("X: 0.00, Y: 0.00") }

    val currentProfile = remember(
        themeName, dpadMode, opacity, scale, buttonSpacing, haptic, showShoulderL2R2, showTurbo, showQuickSave
    ) {
        initialProfile.copy(
            controllerTheme = themeName,
            dpadMode = dpadMode,
            opacity = opacity,
            scale = scale,
            buttonSpacing = buttonSpacing,
            hapticFeedback = haptic,
            showShoulderL2R2 = showShoulderL2R2,
            showTurbo = showTurbo,
            showQuickSave = showQuickSave
        )
    }

    Scaffold(
        containerColor = SophisticatedBg,
        topBar = {
            TopAppBar(
                title = { Text("Virtual Controller Customizer", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_from_controller_editor")) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = {
                            themeName = "SOPHISTICATED"
                            dpadMode = "CROSS"
                            opacity = 0.70f
                            scale = 1.0f
                            buttonSpacing = 1.0f
                            haptic = true
                            showShoulderL2R2 = false
                            showTurbo = true
                            showQuickSave = true
                        },
                        modifier = Modifier.padding(end = 6.dp).testTag("btn_reset_controller_defaults"),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder)
                    ) {
                        Icon(Icons.Default.RestartAlt, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset", color = TextSecondary, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.saveControllerProfile(currentProfile)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LavenderPrimary,
                            contentColor = DeepVioletOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp).testTag("btn_save_controller_profile")
                    ) {
                        Icon(Icons.Default.Save, null, tint = DeepVioletOnPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SophisticatedBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Segmented Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SophisticatedSurfaceVariant,
                        contentColor = LavenderPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = LavenderPrimary,
                                height = 3.dp
                            )
                        },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Style & Themes", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Size & Spacing", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Buttons & Triggers", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    when (selectedTab) {
                        0 -> {
                            // Theme & D-Pad Mode Selection
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Visual Theme Skin:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ThemeChip(
                                        title = "Sophisticated Slate",
                                        subtitle = "Lavender Frost",
                                        accentColor = Color(0xFFD0BCFF),
                                        isSelected = themeName == "SOPHISTICATED",
                                        onClick = { themeName = "SOPHISTICATED" }
                                    )
                                    ThemeChip(
                                        title = "Cyber Neon",
                                        subtitle = "Electric Cyan & Pink",
                                        accentColor = Color(0xFF00E5FF),
                                        isSelected = themeName == "CYBER_NEON",
                                        onClick = { themeName = "CYBER_NEON" }
                                    )
                                    ThemeChip(
                                        title = "Retro Arcade",
                                        subtitle = "Gold & Crimson",
                                        accentColor = Color(0xFFFFD54F),
                                        isSelected = themeName == "RETRO_ARCADE",
                                        onClick = { themeName = "RETRO_ARCADE" }
                                    )
                                    ThemeChip(
                                        title = "Crystal Glass",
                                        subtitle = "Frosted Minimal",
                                        accentColor = Color(0xFFFFFFFF),
                                        isSelected = themeName == "CRYSTAL_GLASS",
                                        onClick = { themeName = "CRYSTAL_GLASS" }
                                    )
                                }

                                Spacer(Modifier.height(4.dp))
                                Text("Directional Control Type:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DpadModeChip(
                                        title = "Cross D-Pad",
                                        subtitle = "8-Way Precision",
                                        isSelected = dpadMode == "CROSS",
                                        modifier = Modifier.weight(1f),
                                        onClick = { dpadMode = "CROSS" }
                                    )
                                    DpadModeChip(
                                        title = "Analog Stick",
                                        subtitle = "360° Joystick",
                                        isSelected = dpadMode == "ANALOG",
                                        modifier = Modifier.weight(1f),
                                        onClick = { dpadMode = "ANALOG" }
                                    )
                                    DpadModeChip(
                                        title = "Split Keys",
                                        subtitle = "Discrete 4-Way",
                                        isSelected = dpadMode == "SPLIT",
                                        modifier = Modifier.weight(1f),
                                        onClick = { dpadMode = "SPLIT" }
                                    )
                                }
                            }
                        }
                        1 -> {
                            // Sliders: Opacity, Scale, Spacing
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Opacity
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Button Opacity", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("${(opacity * 100).toInt()}%", color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Slider(
                                    value = opacity,
                                    onValueChange = { opacity = it },
                                    valueRange = 0.2f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = LavenderPrimary, activeTrackColor = LavenderPrimary)
                                )

                                // Scale
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Button Size Scale", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(String.format(Locale.getDefault(), "%.2fx", scale), color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Slider(
                                    value = scale,
                                    onValueChange = { scale = it },
                                    valueRange = 0.6f..1.4f,
                                    colors = SliderDefaults.colors(thumbColor = LavenderPrimary, activeTrackColor = LavenderPrimary)
                                )

                                // Button Spacing
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Action Button Spread / Spacing", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(String.format(Locale.getDefault(), "%.2fx", buttonSpacing), color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Slider(
                                    value = buttonSpacing,
                                    onValueChange = { buttonSpacing = it },
                                    valueRange = 0.8f..1.3f,
                                    colors = SliderDefaults.colors(thumbColor = LavenderPrimary, activeTrackColor = LavenderPrimary)
                                )
                            }
                        }
                        2 -> {
                            // Toggles: L2/R2, Turbo, QuickSave, Haptic
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Haptic Feedback (Tactile Pulse)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Vibrate gently on button press", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = haptic,
                                        onCheckedChange = { haptic = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DeepVioletOnPrimary, checkedTrackColor = LavenderPrimary)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Show L2 & R2 Triggers", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Secondary top shoulder triggers", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = showShoulderL2R2,
                                        onCheckedChange = { showShoulderL2R2 = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DeepVioletOnPrimary, checkedTrackColor = LavenderPrimary)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Show Turbo Speed Fast-Forward", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Fast text/dialogue speedup button", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = showTurbo,
                                        onCheckedChange = { showTurbo = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DeepVioletOnPrimary, checkedTrackColor = LavenderPrimary)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Show Quick Save Button", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Direct save snapshot trigger on HUD", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = showQuickSave,
                                        onCheckedChange = { showQuickSave = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DeepVioletOnPrimary, checkedTrackColor = LavenderPrimary)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Real-Time Telemetry Indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SophisticatedSurfaceVariant)
                            .border(1.dp, SophisticatedBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Input: $lastPressedButton", color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(analogTelemetry, color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Interactive Live Gamepad Sandbox Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF131518))
                    .border(1.dp, SophisticatedBorder)
            ) {
                VirtualGamepadOverlay(
                    profile = currentProfile,
                    isEditMode = true,
                    onButtonPress = { btn ->
                        lastPressedButton = "Button [$btn] PRESSED"
                    },
                    onButtonRelease = { btn ->
                        lastPressedButton = "Button [$btn] RELEASED"
                    },
                    onDpadChange = { x, y ->
                        analogTelemetry = String.format(Locale.getDefault(), "X: %.2f, Y: %.2f", x, y)
                    }
                )

                // Drag hint banner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SophisticatedBadge.copy(alpha = 0.8f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("💡 Drag D-Pad or ABXY to customize placement in real-time", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(
    title: String,
    subtitle: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SophisticatedBadge else SophisticatedSurfaceVariant)
            .border(
                1.5.dp,
                if (isSelected) LavenderPrimary else SophisticatedBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Column {
                Text(title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun DpadModeChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SophisticatedBadge else SophisticatedSurfaceVariant)
            .border(
                1.5.dp,
                if (isSelected) LavenderPrimary else SophisticatedBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = if (isSelected) LavenderPrimary else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 9.sp)
        }
    }
}
