package com.example.ui.runtimes

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Gamepad
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RuntimeState
import com.example.runtime.RuntimeManager
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuntimeManagerScreen(
    onBack: () -> Unit
) {
    val runtimes = RuntimeManager.providers

    Scaffold(
        containerColor = SophisticatedBg,
        topBar = {
            TopAppBar(
                title = { Text("Compatibility Runtimes", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_from_runtimes")) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                Text(
                    text = "ENGINE RUNTIMES & COMPATIBILITY STATUS",
                    color = LavenderPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "GameBridge menganalisis struktur file game secara realistis. Game berbasis web (RPG Maker MV/MZ & HTML5) dan Android Native dapat langsung dijalankan, sedangkan game Windows .exe dan RGSS memerlukan runtime compatibility container khusus.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            items(runtimes, key = { it.id }) { runtime ->
                val state = runtime.runtimeState
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("runtime_card_${runtime.id}"),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SophisticatedBadge),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Gamepad, null, tint = LavenderPrimary, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(runtime.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("v${runtime.version}", color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            // Status Tag
                            val statusLabel = when (state) {
                                RuntimeState.INSTALLED -> "INSTALLED"
                                RuntimeState.NOT_INSTALLED -> "NOT INSTALLED"
                                RuntimeState.UNSUPPORTED -> "UNSUPPORTED"
                            }
                            val statusColor = when (state) {
                                RuntimeState.INSTALLED -> SoftMint
                                RuntimeState.NOT_INSTALLED -> SoftAmber
                                RuntimeState.UNSUPPORTED -> MutedRose
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = statusLabel,
                                    color = statusColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = runtime.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        // Supported Engines
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            runtime.supportedEngines.forEach { eng ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SophisticatedSurfaceVariant)
                                        .border(1.dp, SophisticatedBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(eng.displayName, color = LavenderPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
