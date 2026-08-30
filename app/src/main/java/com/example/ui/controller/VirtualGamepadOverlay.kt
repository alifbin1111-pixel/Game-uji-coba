package com.example.ui.controller

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ControllerProfileEntity
import com.example.ui.theme.GamepadButtonBg
import com.example.ui.theme.GamepadButtonBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import kotlin.math.roundToInt

@Composable
fun VirtualGamepadOverlay(
    profile: ControllerProfileEntity = ControllerProfileEntity(id = "default", name = "Standard Layout"),
    isEditMode: Boolean = false,
    onButtonPress: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    val triggerHaptic = {
        if (profile.hapticFeedback) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(18)
                }
            } catch (e: Exception) {
                // Ignore vibration errors on non-hardware devices
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .alpha(profile.opacity)
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        // 1. D-PAD (Left Hand)
        var dpadOffsetX by remember { mutableFloatStateOf(profile.dpadX * maxWidthPx) }
        var dpadOffsetY by remember { mutableFloatStateOf(profile.dpadY * maxHeightPx) }

        Box(
            modifier = Modifier
                .offset { IntOffset(dpadOffsetX.roundToInt(), dpadOffsetY.roundToInt()) }
                .then(
                    if (isEditMode) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                dpadOffsetX += dragAmount.x
                                dpadOffsetY += dragAmount.y
                            }
                        }
                    } else Modifier
                )
                .size((160 * profile.scale).dp)
                .testTag("gamepad_dpad"),
            contentAlignment = Alignment.Center
        ) {
            // D-Pad Cross Background
            Box(
                modifier = Modifier
                    .size((150 * profile.scale).dp, (54 * profile.scale).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GamepadButtonBg)
                    .border(1.5.dp, GamepadButtonBorder, RoundedCornerShape(12.dp))
            )
            Box(
                modifier = Modifier
                    .size((54 * profile.scale).dp, (150 * profile.scale).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GamepadButtonBg)
                    .border(1.5.dp, GamepadButtonBorder, RoundedCornerShape(12.dp))
            )

            // D-Pad Direction Buttons
            // UP
            GamepadDirButton(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size((50 * profile.scale).dp)
                    .testTag("btn_dpad_up"),
                icon = { Icon(Icons.Default.ArrowDropUp, "Up", tint = NeonCyan) },
                onPress = {
                    triggerHaptic()
                    onButtonPress("UP")
                }
            )
            // DOWN
            GamepadDirButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size((50 * profile.scale).dp)
                    .testTag("btn_dpad_down"),
                icon = { Icon(Icons.Default.ArrowDropDown, "Down", tint = NeonCyan) },
                onPress = {
                    triggerHaptic()
                    onButtonPress("DOWN")
                }
            )
            // LEFT
            GamepadDirButton(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size((50 * profile.scale).dp)
                    .testTag("btn_dpad_left"),
                icon = { Icon(Icons.Default.ArrowLeft, "Left", tint = NeonCyan) },
                onPress = {
                    triggerHaptic()
                    onButtonPress("LEFT")
                }
            )
            // RIGHT
            GamepadDirButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size((50 * profile.scale).dp)
                    .testTag("btn_dpad_right"),
                icon = { Icon(Icons.Default.ArrowRight, "Right", tint = NeonCyan) },
                onPress = {
                    triggerHaptic()
                    onButtonPress("RIGHT")
                }
            )
        }

        // 2. ACTION BUTTONS A, B, X, Y (Right Hand)
        var abxyOffsetX by remember { mutableFloatStateOf(profile.btnAX * maxWidthPx - 140) }
        var abxyOffsetY by remember { mutableFloatStateOf(profile.btnAY * maxHeightPx - 80) }

        Box(
            modifier = Modifier
                .offset { IntOffset(abxyOffsetX.roundToInt(), abxyOffsetY.roundToInt()) }
                .then(
                    if (isEditMode) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                abxyOffsetX += dragAmount.x
                                abxyOffsetY += dragAmount.y
                            }
                        }
                    } else Modifier
                )
                .size((160 * profile.scale).dp)
                .testTag("gamepad_abxy"),
            contentAlignment = Alignment.Center
        ) {
            // Y (Top)
            GamepadActionButton(
                label = "Y",
                color = NeonPurple,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size((52 * profile.scale).dp)
                    .testTag("btn_y"),
                onPress = {
                    triggerHaptic()
                    onButtonPress("Y")
                }
            )
            // A (Right)
            GamepadActionButton(
                label = "A",
                color = NeonCyan,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size((52 * profile.scale).dp)
                    .testTag("btn_a"),
                onPress = {
                    triggerHaptic()
                    onButtonPress("A")
                }
            )
            // B (Bottom)
            GamepadActionButton(
                label = "B",
                color = Color(0xFFFF4081),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size((52 * profile.scale).dp)
                    .testTag("btn_b"),
                onPress = {
                    triggerHaptic()
                    onButtonPress("B")
                }
            )
            // X (Left)
            GamepadActionButton(
                label = "X",
                color = Color(0xFF00E676),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size((52 * profile.scale).dp)
                    .testTag("btn_x"),
                onPress = {
                    triggerHaptic()
                    onButtonPress("X")
                }
            )
        }

        // 3. SHOULDER BUTTONS L & R
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // L Button
            Box(
                modifier = Modifier
                    .size(76.dp, 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GamepadButtonBg)
                    .border(1.5.dp, GamepadButtonBorder, RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            triggerHaptic()
                            onButtonPress("L")
                        })
                    }
                    .testTag("btn_l"),
                contentAlignment = Alignment.Center
            ) {
                Text("L1", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Start & Select
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(54.dp, 28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GamepadButtonBg)
                        .border(1.dp, GamepadButtonBorder, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                triggerHaptic()
                                onButtonPress("SELECT")
                            })
                        }
                        .testTag("btn_select"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SELECT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }

                Box(
                    modifier = Modifier
                        .size(54.dp, 28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GamepadButtonBg)
                        .border(1.dp, GamepadButtonBorder, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                triggerHaptic()
                                onButtonPress("START")
                            })
                        }
                        .testTag("btn_start"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("START", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // R Button
            Box(
                modifier = Modifier
                    .size(76.dp, 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GamepadButtonBg)
                    .border(1.5.dp, GamepadButtonBorder, RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            triggerHaptic()
                            onButtonPress("R")
                        })
                    }
                    .testTag("btn_r"),
                contentAlignment = Alignment.Center
            ) {
                Text("R1", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun GamepadDirButton(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    onPress: () -> Unit
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun GamepadActionButton(
    label: String,
    color: Color,
    modifier: Modifier,
    onPress: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(GamepadButtonBg)
            .border(2.dp, color, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPress()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
    }
}
