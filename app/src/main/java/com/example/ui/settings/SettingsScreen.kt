package com.example.ui.settings

import android.os.Build
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GameBridgeViewModel,
    onBack: () -> Unit
) {
    val allGames by viewModel.allGames.collectAsState()
    val totalTranslations by viewModel.totalTranslations.collectAsState()

    Scaffold(
        containerColor = SophisticatedBg,
        topBar = {
            TopAppBar(
                title = { Text("Settings & Diagnostics", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_from_settings")) {
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
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // About App
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SophisticatedBadge),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Info, null, tint = LavenderPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("About GameBridge", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "GameBridge is an open modular game launcher, multi-runtime compatibility environment, and real-time live localization bridge (JoiPlay + MTool concept) designed for cross-platform local indie gaming on Android.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SophisticatedBadge)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Version: 1.0.0 (Sophisticated Edition)", color = LavenderPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Device Telemetry
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SophisticatedBadge),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhoneAndroid, null, tint = LavenderPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("Device Specifications & Telemetry", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SophisticatedSurfaceVariant)
                                .border(1.dp, SophisticatedBorder, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoRow("Hardware Model:", "${Build.MANUFACTURER} ${Build.MODEL}")
                                InfoRow("Android OS Version:", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                                InfoRow("Primary CPU Architecture:", Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a")
                                InfoRow("Installed Library Titles:", "${allGames.size} Games")
                                InfoRow("Cached Translation Strings:", "$totalTranslations Entries")
                            }
                        }
                    }
                }
            }

            // Global Live Localization Settings
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SophisticatedBadge),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Info, null, tint = LavenderPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("MTool Live Localization Engine", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Text(
                            text = "Intercepts RPG Maker (MV/MZ), HTML5 Canvas 2D/WebGL, and DOM render calls dynamically to replace Japanese text in-place with Indonesian or English.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SophisticatedSurfaceVariant)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                InfoRow("Total Cached Translation Pairs:", "$totalTranslations Entries")
                                InfoRow("Supported Text Contexts:", "Dialog, Menus, Battle UI, Canvas, DOM")
                                InfoRow("Translation Latency (L1 Cache):", "0 ms (Instant in-memory)")
                            }
                        }

                        Button(
                            onClick = { viewModel.clearTranslationCache(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SophisticatedSurfaceVariant,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_clear_all_trans_cache")
                        ) {
                            Text("Clear All Local Translation Database", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Storage Maintenance
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SophisticatedBadge),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storage, null, tint = LavenderPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("Storage & Sample Management", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = { viewModel.resetSampleGames() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SophisticatedSurfaceVariant,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_reset_samples_in_settings")
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = LavenderPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Reinstall Built-in Sample Titles", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

