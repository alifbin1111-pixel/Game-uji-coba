package com.example.ui.controller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.LavenderVioletBrush
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

    var opacity by remember { mutableFloatStateOf(initialProfile.opacity) }
    var scale by remember { mutableFloatStateOf(initialProfile.scale) }
    var haptic by remember { mutableStateOf(initialProfile.hapticFeedback) }
    var lastPressedButton by remember { mutableStateOf("No input pressed") }

    val currentProfile = remember(opacity, scale, haptic) {
        initialProfile.copy(opacity = opacity, scale = scale, hapticFeedback = haptic)
    }

    Scaffold(
        containerColor = SophisticatedBg,
        topBar = {
            TopAppBar(
                title = { Text("Virtual Controller Editor", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_from_controller_editor")) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
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
                        Icon(Icons.Default.Save, null, tint = DeepVioletOnPrimary)
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
            // Controls Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Opacity
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Button Opacity", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${(opacity * 100).toInt()}%", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = 0.2f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = LavenderPrimary, activeTrackColor = LavenderPrimary)
                    )

                    // Scale
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Button Scale", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(String.format(Locale.getDefault(), "%.1fx", scale), color = LavenderPrimary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.6f..1.4f,
                        colors = SliderDefaults.colors(thumbColor = LavenderPrimary, activeTrackColor = LavenderPrimary)
                    )

                    // Haptic
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Haptic Feedback (Vibration)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = haptic,
                            onCheckedChange = { haptic = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = DeepVioletOnPrimary, checkedTrackColor = LavenderPrimary)
                        )
                    }

                    // Test Status
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SophisticatedSurfaceVariant)
                            .border(1.dp, SophisticatedBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Last Input Event: $lastPressedButton", color = SoftMint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Virtual Gamepad Sandbox Canvas
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
                        lastPressedButton = "Button $btn PRESSED"
                    }
                )
            }
        }
    }
}

