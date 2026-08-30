package com.example.ui.translation

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepVioletOnPrimary
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.LavenderVioletBrush
import com.example.ui.theme.MutedRose
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationSettingsScreen(
    viewModel: GameBridgeViewModel,
    onBack: () -> Unit
) {
    val totalTranslations by viewModel.totalTranslations.collectAsState()
    val scope = rememberCoroutineScope()
    var geminiKeyInput by remember { mutableStateOf("") }
    var testOriginalText by remember { mutableStateOf("勇者よ、村の宝箱を開けてみよ！") }
    var testResultText by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SophisticatedBg,
        topBar = {
            TopAppBar(
                title = { Text("Game Localization Engine", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_from_trans_settings")) {
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
            // Stats Card
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
                                Icon(Icons.Default.Translate, null, tint = LavenderPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("Translation Cache Database", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "A total of $totalTranslations dialogue lines are cached in the local SQLite database. Repeated lines are returned instantly with zero network latency.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.clearTranslationCache() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SophisticatedSurfaceVariant,
                                contentColor = MutedRose
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_clear_trans_cache")
                        ) {
                            Icon(Icons.Default.DeleteSweep, null, tint = MutedRose, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clear Translation Cache", color = MutedRose, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // AI Provider Card
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
                                Icon(Icons.Default.AutoAwesome, null, tint = LavenderPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("Google Gemini AI Translation", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Text(
                            text = "Gemini 2.5 Flash model is pre-prompted with video game terminology, character dialogue styles, and fantasy RPG context.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        OutlinedTextField(
                            value = geminiKeyInput,
                            onValueChange = { geminiKeyInput = it },
                            modifier = Modifier.fillMaxWidth().testTag("input_gemini_api_key"),
                            label = { Text("Custom Gemini API Key (Optional)") },
                            placeholder = { Text("AIzaSy...", color = TextSecondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = { Icon(Icons.Default.Key, null, tint = LavenderPrimary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SophisticatedSurfaceVariant,
                                unfocusedContainerColor = SophisticatedSurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = LavenderPrimary,
                                unfocusedBorderColor = SophisticatedBorder
                            )
                        )
                    }
                }
            }

            // Live Translation Sandbox
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Live Translation Sandbox", color = LavenderPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        OutlinedTextField(
                            value = testOriginalText,
                            onValueChange = { testOriginalText = it },
                            modifier = Modifier.fillMaxWidth().testTag("input_test_trans"),
                            label = { Text("Game Dialogue (JA / EN)") },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SophisticatedSurfaceVariant,
                                unfocusedContainerColor = SophisticatedSurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = LavenderPrimary,
                                unfocusedBorderColor = SophisticatedBorder
                            )
                        )

                        Button(
                            onClick = {
                                isTesting = true
                                scope.launch {
                                    val res = viewModel.translate(
                                        gameId = "sandbox_test",
                                        text = testOriginalText,
                                        sourceLang = "ja",
                                        targetLang = "id",
                                        provider = "GEMINI",
                                        apiKey = geminiKeyInput.ifBlank { null }
                                    )
                                    testResultText = res
                                    isTesting = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_run_test_trans"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LavenderPrimary,
                                contentColor = DeepVioletOnPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (isTesting) "Translating..." else "Test Translation Now", fontWeight = FontWeight.Bold)
                        }

                        if (testResultText.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SophisticatedSurfaceVariant)
                                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text("Translated Result:", color = SoftMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(testResultText, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
