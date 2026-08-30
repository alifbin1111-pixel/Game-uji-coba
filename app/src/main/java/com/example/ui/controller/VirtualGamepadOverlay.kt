package com.example.ui.controller

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ControllerProfileEntity
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class GamepadThemeColors(
    val baseBackground: Color,
    val surfaceGradientTop: Color,
    val surfaceGradientBottom: Color,
    val borderColor: Color,
    val activeBorderColor: Color,
    val pressedGlowColor: Color,
    val textPrimary: Color,
    val accentA: Color,
    val accentB: Color,
    val accentX: Color,
    val accentY: Color,
    val shoulderColor: Color,
    val utilityColor: Color
)

fun getGamepadThemeColors(themeName: String): GamepadThemeColors {
    return when (themeName.uppercase()) {
        "CYBER_NEON" -> GamepadThemeColors(
            baseBackground = Color(0xB30A0E18),
            surfaceGradientTop = Color(0xFF1E2638),
            surfaceGradientBottom = Color(0xFF0F1524),
            borderColor = Color(0x9900E5FF),
            activeBorderColor = Color(0xFF00FFFF),
            pressedGlowColor = Color(0x6600E5FF),
            textPrimary = Color(0xFFE0F7FA),
            accentA = Color(0xFF00E5FF),
            accentB = Color(0xFFFF007F),
            accentX = Color(0xFF00E676),
            accentY = Color(0xFFFFD600),
            shoulderColor = Color(0xFF00E5FF),
            utilityColor = Color(0xFF00E5FF)
        )
        "RETRO_ARCADE" -> GamepadThemeColors(
            baseBackground = Color(0xB3222326),
            surfaceGradientTop = Color(0xFF383A3F),
            surfaceGradientBottom = Color(0xFF1E2024),
            borderColor = Color(0x88FFB300),
            activeBorderColor = Color(0xFFFFD54F),
            pressedGlowColor = Color(0x66FFB300),
            textPrimary = Color(0xFFFFF8E1),
            accentA = Color(0xFFE53935),
            accentB = Color(0xFFC62828),
            accentX = Color(0xFF1E88E5),
            accentY = Color(0xFFFB8C00),
            shoulderColor = Color(0xFFFFB300),
            utilityColor = Color(0xFFFFD54F)
        )
        "CRYSTAL_GLASS" -> GamepadThemeColors(
            baseBackground = Color(0x33FFFFFF),
            surfaceGradientTop = Color(0x44FFFFFF),
            surfaceGradientBottom = Color(0x1AFFFFFF),
            borderColor = Color(0x66FFFFFF),
            activeBorderColor = Color(0xCCFFFFFF),
            pressedGlowColor = Color(0x44FFFFFF),
            textPrimary = Color(0xFFFFFFFF),
            accentA = Color(0xEEFFFFFF),
            accentB = Color(0xEEFFFFFF),
            accentX = Color(0xEEFFFFFF),
            accentY = Color(0xEEFFFFFF),
            shoulderColor = Color(0xDDFFFFFF),
            utilityColor = Color(0xDDFFFFFF)
        )
        else -> GamepadThemeColors( // SOPHISTICATED
            baseBackground = Color(0x9923252A),
            surfaceGradientTop = Color(0xFF34373E),
            surfaceGradientBottom = Color(0xFF222429),
            borderColor = Color(0x884D515B),
            activeBorderColor = Color(0xFFD0BCFF),
            pressedGlowColor = Color(0x55D0BCFF),
            textPrimary = Color(0xFFE2E2E6),
            accentA = Color(0xFFD0BCFF),
            accentB = Color(0xFFFFB4AB),
            accentX = Color(0xFFA8E6CF),
            accentY = Color(0xFFFFDCC1),
            shoulderColor = Color(0xFFD0BCFF),
            utilityColor = Color(0xFFEADDFF)
        )
    }
}

@Composable
fun VirtualGamepadOverlay(
    profile: ControllerProfileEntity = ControllerProfileEntity(id = "default", name = "Standard Layout"),
    isEditMode: Boolean = false,
    onButtonPress: (String) -> Unit = {},
    onButtonRelease: (String) -> Unit = {},
    onDpadChange: (x: Float, y: Float) -> Unit = { _, _ -> },
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
                    vibrator?.vibrate(VibrationEffect.createOneShot(16, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(16)
                }
            } catch (e: Exception) {
                // Ignore vibration errors on unsupported devices
            }
        }
    }

    val themeColors = remember(profile.controllerTheme) {
        getGamepadThemeColors(profile.controllerTheme)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .alpha(profile.opacity)
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        // 1. DIRECTIONAL CONTROLS (Left Hand: Cross D-Pad / Analog Stick / Split)
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
                .size((168 * profile.scale).dp)
                .testTag("gamepad_dpad_container"),
            contentAlignment = Alignment.Center
        ) {
            when (profile.dpadMode) {
                "ANALOG" -> {
                    TactileAnalogStick(
                        scale = profile.scale,
                        themeColors = themeColors,
                        onDirectionChange = { dirX, dirY ->
                            onDpadChange(dirX, dirY)
                        },
                        onButtonPress = { dir ->
                            triggerHaptic()
                            onButtonPress(dir)
                        },
                        onButtonRelease = onButtonRelease
                    )
                }
                "SPLIT" -> {
                    TactileSplitDpad(
                        scale = profile.scale,
                        themeColors = themeColors,
                        spacingMultiplier = profile.buttonSpacing,
                        onButtonPress = { dir ->
                            triggerHaptic()
                            onButtonPress(dir)
                        },
                        onButtonRelease = onButtonRelease
                    )
                }
                else -> { // "CROSS" default
                    TactileCrossDpad(
                        scale = profile.scale,
                        themeColors = themeColors,
                        onButtonPress = { dir ->
                            triggerHaptic()
                            onButtonPress(dir)
                        },
                        onButtonRelease = onButtonRelease
                    )
                }
            }
        }

        // 2. ACTION BUTTONS CLUSTER A, B, X, Y (Right Hand)
        var abxyOffsetX by remember { mutableFloatStateOf(profile.btnAX * maxWidthPx - 145) }
        var abxyOffsetY by remember { mutableFloatStateOf(profile.btnAY * maxHeightPx - 85) }

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
                .size((168 * profile.scale).dp)
                .testTag("gamepad_abxy_container"),
            contentAlignment = Alignment.Center
        ) {
            TactileActionDiamond(
                scale = profile.scale,
                spacingMultiplier = profile.buttonSpacing,
                themeColors = themeColors,
                onButtonPress = { btn ->
                    triggerHaptic()
                    onButtonPress(btn)
                },
                onButtonRelease = onButtonRelease
            )
        }

        // 3. SHOULDER BUMPERS & TRIGGERS (L1, R1, L2, R2)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Shoulders (L2, L1)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (profile.showShoulderL2R2) {
                    TactileShoulderButton(
                        label = "L2",
                        subLabel = "ZL",
                        width = (64 * profile.scale).dp,
                        height = (32 * profile.scale).dp,
                        themeColors = themeColors,
                        testTag = "btn_l2",
                        onPress = {
                            triggerHaptic()
                            onButtonPress("L2")
                        },
                        onRelease = { onButtonRelease("L2") }
                    )
                }
                TactileShoulderButton(
                    label = "L1",
                    subLabel = "LB",
                    width = (74 * profile.scale).dp,
                    height = (36 * profile.scale).dp,
                    themeColors = themeColors,
                    testTag = "btn_l1",
                    onPress = {
                        triggerHaptic()
                        onButtonPress("L1")
                    },
                    onRelease = { onButtonRelease("L1") }
                )
            }

            // Center Auxiliary & Navigation Bar (Select, Start, Turbo, Menu)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                // Turbo Button (Fast forward)
                if (profile.showTurbo) {
                    TactileUtilityPill(
                        label = "TURBO",
                        icon = Icons.Default.FastForward,
                        themeColors = themeColors,
                        testTag = "btn_turbo",
                        onPress = {
                            triggerHaptic()
                            onButtonPress("TURBO")
                        },
                        onRelease = { onButtonRelease("TURBO") }
                    )
                }

                // Select
                TactileUtilityPill(
                    label = "SELECT",
                    themeColors = themeColors,
                    testTag = "btn_select",
                    onPress = {
                        triggerHaptic()
                        onButtonPress("SELECT")
                    },
                    onRelease = { onButtonRelease("SELECT") }
                )

                // Start
                TactileUtilityPill(
                    label = "START",
                    themeColors = themeColors,
                    testTag = "btn_start",
                    onPress = {
                        triggerHaptic()
                        onButtonPress("START")
                    },
                    onRelease = { onButtonRelease("START") }
                )

                // In-Game Quick Save Snapshot
                if (profile.showQuickSave) {
                    TactileUtilityPill(
                        label = "SAVE",
                        icon = Icons.Default.Save,
                        themeColors = themeColors,
                        testTag = "btn_quick_save",
                        onPress = {
                            triggerHaptic()
                            onButtonPress("QUICK_SAVE")
                        },
                        onRelease = { onButtonRelease("QUICK_SAVE") }
                    )
                }
            }

            // Right Shoulders (R2, R1)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
                if (profile.showShoulderL2R2) {
                    TactileShoulderButton(
                        label = "R2",
                        subLabel = "ZR",
                        width = (64 * profile.scale).dp,
                        height = (32 * profile.scale).dp,
                        themeColors = themeColors,
                        testTag = "btn_r2",
                        onPress = {
                            triggerHaptic()
                            onButtonPress("R2")
                        },
                        onRelease = { onButtonRelease("R2") }
                    )
                }
                TactileShoulderButton(
                    label = "R1",
                    subLabel = "RB",
                    width = (74 * profile.scale).dp,
                    height = (36 * profile.scale).dp,
                    themeColors = themeColors,
                    testTag = "btn_r1",
                    onPress = {
                        triggerHaptic()
                        onButtonPress("R1")
                    },
                    onRelease = { onButtonRelease("R1") }
                )
            }
        }
    }
}

// 1. TACTILE CROSS D-PAD
@Composable
private fun TactileCrossDpad(
    scale: Float,
    themeColors: GamepadThemeColors,
    onButtonPress: (String) -> Unit,
    onButtonRelease: (String) -> Unit
) {
    var activeDirection by remember { mutableStateOf<String?>(null) }

    val baseWidth = (156 * scale).dp
    val armThickness = (56 * scale).dp
    val centerPivot = (44 * scale).dp

    Box(
        modifier = Modifier
            .size(baseWidth)
            .testTag("gamepad_cross_dpad"),
        contentAlignment = Alignment.Center
    ) {
        // Cross Body Shadow & Base
        Box(
            modifier = Modifier
                .size(baseWidth, armThickness)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(themeColors.surfaceGradientTop, themeColors.surfaceGradientBottom)
                    )
                )
                .border(1.5.dp, themeColors.borderColor, RoundedCornerShape(14.dp))
        )
        Box(
            modifier = Modifier
                .size(armThickness, baseWidth)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(themeColors.surfaceGradientTop, themeColors.surfaceGradientBottom)
                    )
                )
                .border(1.5.dp, themeColors.borderColor, RoundedCornerShape(14.dp))
        )

        // Center Concave Bevel Ring
        Box(
            modifier = Modifier
                .size(centerPivot)
                .clip(CircleShape)
                .background(themeColors.surfaceGradientBottom)
                .border(1.dp, themeColors.borderColor.copy(alpha = 0.5f), CircleShape)
        )

        // UP
        DpadWingButton(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(armThickness, (56 * scale).dp)
                .testTag("btn_dpad_up"),
            direction = "UP",
            isActive = activeDirection == "UP",
            icon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Up",
                    tint = if (activeDirection == "UP") themeColors.activeBorderColor else themeColors.textPrimary,
                    modifier = Modifier.size((28 * scale).dp)
                )
            },
            onPress = {
                activeDirection = "UP"
                onButtonPress("UP")
            },
            onRelease = {
                activeDirection = null
                onButtonRelease("UP")
            }
        )

        // DOWN
        DpadWingButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(armThickness, (56 * scale).dp)
                .testTag("btn_dpad_down"),
            direction = "DOWN",
            isActive = activeDirection == "DOWN",
            icon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Down",
                    tint = if (activeDirection == "DOWN") themeColors.activeBorderColor else themeColors.textPrimary,
                    modifier = Modifier.size((28 * scale).dp)
                )
            },
            onPress = {
                activeDirection = "DOWN"
                onButtonPress("DOWN")
            },
            onRelease = {
                activeDirection = null
                onButtonRelease("DOWN")
            }
        )

        // LEFT
        DpadWingButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size((56 * scale).dp, armThickness)
                .testTag("btn_dpad_left"),
            direction = "LEFT",
            isActive = activeDirection == "LEFT",
            icon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Left",
                    tint = if (activeDirection == "LEFT") themeColors.activeBorderColor else themeColors.textPrimary,
                    modifier = Modifier.size((28 * scale).dp)
                )
            },
            onPress = {
                activeDirection = "LEFT"
                onButtonPress("LEFT")
            },
            onRelease = {
                activeDirection = null
                onButtonRelease("LEFT")
            }
        )

        // RIGHT
        DpadWingButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size((56 * scale).dp, armThickness)
                .testTag("btn_dpad_right"),
            direction = "RIGHT",
            isActive = activeDirection == "RIGHT",
            icon = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Right",
                    tint = if (activeDirection == "RIGHT") themeColors.activeBorderColor else themeColors.textPrimary,
                    modifier = Modifier.size((28 * scale).dp)
                )
            },
            onPress = {
                activeDirection = "RIGHT"
                onButtonPress("RIGHT")
            },
            onRelease = {
                activeDirection = null
                onButtonRelease("RIGHT")
            }
        )
    }
}

@Composable
private fun DpadWingButton(
    modifier: Modifier,
    direction: String,
    isActive: Boolean,
    icon: @Composable () -> Unit,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed || isActive) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 1200f),
        label = "dpad_wing_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .pointerInput(direction) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress()
                        tryAwaitRelease()
                        isPressed = false
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

// 2. TACTILE 360° FLOATING ANALOG STICK
@Composable
private fun TactileAnalogStick(
    scale: Float,
    themeColors: GamepadThemeColors,
    onDirectionChange: (Float, Float) -> Unit,
    onButtonPress: (String) -> Unit,
    onButtonRelease: (String) -> Unit
) {
    val outerRadiusDp = (76 * scale).dp
    val thumbRadiusDp = (34 * scale).dp

    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var currentDirectionKey by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .size(outerRadiusDp * 2)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(themeColors.surfaceGradientTop, themeColors.surfaceGradientBottom)
                )
            )
            .border(2.dp, themeColors.borderColor, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val delta = offset - Offset(size.width / 2f, size.height / 2f)
                        val maxDistance = size.width / 2f - 20f
                        val dist = delta.getDistance()
                        val clampedOffset = if (dist > maxDistance) delta * (maxDistance / dist) else delta
                        thumbOffset = clampedOffset

                        val normX = (clampedOffset.x / maxDistance).coerceIn(-1f, 1f)
                        val normY = (clampedOffset.y / maxDistance).coerceIn(-1f, 1f)
                        onDirectionChange(normX, normY)
                        val dir = calculateCardinalDirection(normX, normY)
                        if (dir != currentDirectionKey) {
                            currentDirectionKey?.let { onButtonRelease(it) }
                            currentDirectionKey = dir
                            dir?.let { onButtonPress(it) }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val maxDistance = size.width / 2f - 20f
                        val dist = newOffset.getDistance()
                        val clampedOffset = if (dist > maxDistance) newOffset * (maxDistance / dist) else newOffset
                        thumbOffset = clampedOffset

                        val normX = (clampedOffset.x / maxDistance).coerceIn(-1f, 1f)
                        val normY = (clampedOffset.y / maxDistance).coerceIn(-1f, 1f)
                        onDirectionChange(normX, normY)
                        val dir = calculateCardinalDirection(normX, normY)
                        if (dir != currentDirectionKey) {
                            currentDirectionKey?.let { onButtonRelease(it) }
                            currentDirectionKey = dir
                            dir?.let { onButtonPress(it) }
                        }
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onDirectionChange(0f, 0f)
                        currentDirectionKey?.let { onButtonRelease(it) }
                        currentDirectionKey = null
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onDirectionChange(0f, 0f)
                        currentDirectionKey?.let { onButtonRelease(it) }
                        currentDirectionKey = null
                    }
                )
            }
            .testTag("gamepad_analog_stick"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            drawLine(themeColors.borderColor.copy(alpha = 0.35f), Offset(c.x, 10f), Offset(c.x, size.height - 10f), 1.5f)
            drawLine(themeColors.borderColor.copy(alpha = 0.35f), Offset(10f, c.y), Offset(size.width - 10f, c.y), 1.5f)
            drawCircle(themeColors.borderColor.copy(alpha = 0.25f), radius = size.width * 0.3f, style = Stroke(width = 1.5f))
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                .size(thumbRadiusDp * 2)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(themeColors.activeBorderColor.copy(alpha = 0.85f), themeColors.surfaceGradientTop)
                    )
                )
                .border(2.dp, themeColors.activeBorderColor, CircleShape)
                .shadow(8.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size((16 * scale).dp)
                    .clip(CircleShape)
                    .background(themeColors.surfaceGradientBottom)
                    .border(1.dp, themeColors.activeBorderColor.copy(alpha = 0.6f), CircleShape)
            )
        }
    }
}

private fun calculateCardinalDirection(x: Float, y: Float): String? {
    val dist = sqrt(x * x + y * y)
    if (dist < 0.25f) return null
    val angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
    return when {
        angle >= -45f && angle <= 45f -> "RIGHT"
        angle > 45f && angle <= 135f -> "DOWN"
        angle < -45f && angle >= -135f -> "UP"
        else -> "LEFT"
    }
}

// 3. TACTILE SPLIT D-PAD
@Composable
private fun TactileSplitDpad(
    scale: Float,
    themeColors: GamepadThemeColors,
    spacingMultiplier: Float,
    onButtonPress: (String) -> Unit,
    onButtonRelease: (String) -> Unit
) {
    val btnSize = (48 * scale).dp
    val offsetDistance = (54 * scale * spacingMultiplier).dp

    Box(
        modifier = Modifier
            .size((160 * scale).dp)
            .testTag("gamepad_split_dpad"),
        contentAlignment = Alignment.Center
    ) {
        TactileRoundButton(
            label = "",
            icon = { Icon(Icons.Default.KeyboardArrowUp, null, tint = themeColors.textPrimary, modifier = Modifier.size(24.dp)) },
            color = themeColors.textPrimary,
            themeColors = themeColors,
            modifier = Modifier
                .offset(y = -offsetDistance)
                .size(btnSize)
                .testTag("btn_split_up"),
            onPress = { onButtonPress("UP") },
            onRelease = { onButtonRelease("UP") }
        )
        TactileRoundButton(
            label = "",
            icon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = themeColors.textPrimary, modifier = Modifier.size(24.dp)) },
            color = themeColors.textPrimary,
            themeColors = themeColors,
            modifier = Modifier
                .offset(y = offsetDistance)
                .size(btnSize)
                .testTag("btn_split_down"),
            onPress = { onButtonPress("DOWN") },
            onRelease = { onButtonRelease("DOWN") }
        )
        TactileRoundButton(
            label = "",
            icon = { Icon(Icons.Default.KeyboardArrowLeft, null, tint = themeColors.textPrimary, modifier = Modifier.size(24.dp)) },
            color = themeColors.textPrimary,
            themeColors = themeColors,
            modifier = Modifier
                .offset(x = -offsetDistance)
                .size(btnSize)
                .testTag("btn_split_left"),
            onPress = { onButtonPress("LEFT") },
            onRelease = { onButtonRelease("LEFT") }
        )
        TactileRoundButton(
            label = "",
            icon = { Icon(Icons.Default.KeyboardArrowRight, null, tint = themeColors.textPrimary, modifier = Modifier.size(24.dp)) },
            color = themeColors.textPrimary,
            themeColors = themeColors,
            modifier = Modifier
                .offset(x = offsetDistance)
                .size(btnSize)
                .testTag("btn_split_right"),
            onPress = { onButtonPress("RIGHT") },
            onRelease = { onButtonRelease("RIGHT") }
        )
    }
}

// 4. TACTILE ACTION DIAMOND (A, B, X, Y)
@Composable
private fun TactileActionDiamond(
    scale: Float,
    spacingMultiplier: Float,
    themeColors: GamepadThemeColors,
    onButtonPress: (String) -> Unit,
    onButtonRelease: (String) -> Unit
) {
    val btnSize = (52 * scale).dp
    val spread = (54 * scale * spacingMultiplier).dp

    Box(
        modifier = Modifier
            .size((168 * scale).dp)
            .testTag("gamepad_abxy_diamond"),
        contentAlignment = Alignment.Center
    ) {
        // Y Button (Top)
        TactileRoundButton(
            label = "Y",
            subLabel = "▲",
            color = themeColors.accentY,
            themeColors = themeColors,
            modifier = Modifier
                .offset(y = -spread)
                .size(btnSize)
                .testTag("btn_y"),
            onPress = { onButtonPress("Y") },
            onRelease = { onButtonRelease("Y") }
        )

        // A Button (Right - Primary Action)
        TactileRoundButton(
            label = "A",
            subLabel = "●",
            color = themeColors.accentA,
            themeColors = themeColors,
            modifier = Modifier
                .offset(x = spread)
                .size(btnSize)
                .testTag("btn_a"),
            onPress = { onButtonPress("A") },
            onRelease = { onButtonRelease("A") }
        )

        // B Button (Bottom - Cancel / Dash)
        TactileRoundButton(
            label = "B",
            subLabel = "✖",
            color = themeColors.accentB,
            themeColors = themeColors,
            modifier = Modifier
                .offset(y = spread)
                .size(btnSize)
                .testTag("btn_b"),
            onPress = { onButtonPress("B") },
            onRelease = { onButtonRelease("B") }
        )

        // X Button (Left - Menu / Item)
        TactileRoundButton(
            label = "X",
            subLabel = "■",
            color = themeColors.accentX,
            themeColors = themeColors,
            modifier = Modifier
                .offset(x = -spread)
                .size(btnSize)
                .testTag("btn_x"),
            onPress = { onButtonPress("X") },
            onRelease = { onButtonRelease("X") }
        )
    }
}

// 5. REUSABLE TACTILE ROUND ACTION BUTTON
@Composable
private fun TactileRoundButton(
    label: String,
    subLabel: String? = null,
    icon: (@Composable () -> Unit)? = null,
    color: Color,
    themeColors: GamepadThemeColors,
    modifier: Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 1400f),
        label = "btn_press_scale"
    )

    val currentBorderColor by animateColorAsState(
        targetValue = if (isPressed) color else color.copy(alpha = 0.75f),
        animationSpec = tween(120),
        label = "btn_border_glow"
    )

    val currentBgGradient = if (isPressed) {
        listOf(color.copy(alpha = 0.35f), themeColors.surfaceGradientBottom)
    } else {
        listOf(themeColors.surfaceGradientTop, themeColors.surfaceGradientBottom)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(CircleShape)
            .background(Brush.radialGradient(currentBgGradient))
            .border(2.dp, currentBorderColor, CircleShape)
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress()
                        tryAwaitRelease()
                        isPressed = false
                        onRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            icon()
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif
                )
                if (subLabel != null) {
                    Text(
                        text = subLabel,
                        color = color.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 6. TACTILE SHOULDER BUMPERS (L1, R1, L2, R2)
@Composable
private fun TactileShoulderButton(
    label: String,
    subLabel: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    themeColors: GamepadThemeColors,
    testTag: String,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 1200f),
        label = "shoulder_scale"
    )

    Box(
        modifier = Modifier
            .size(width, height)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    if (isPressed) listOf(themeColors.shoulderColor.copy(alpha = 0.4f), themeColors.surfaceGradientBottom)
                    else listOf(themeColors.surfaceGradientTop, themeColors.surfaceGradientBottom)
                )
            )
            .border(1.5.dp, if (isPressed) themeColors.shoulderColor else themeColors.borderColor, RoundedCornerShape(12.dp))
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress()
                        tryAwaitRelease()
                        isPressed = false
                        onRelease()
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = themeColors.shoulderColor, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(subLabel, color = themeColors.textPrimary.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 7. TACTILE UTILITY PILL (SELECT, START, TURBO, SAVE)
@Composable
private fun TactileUtilityPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    themeColors: GamepadThemeColors,
    testTag: String,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 1200f),
        label = "pill_scale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isPressed) themeColors.utilityColor.copy(alpha = 0.3f)
                else themeColors.baseBackground
            )
            .border(1.dp, if (isPressed) themeColors.utilityColor else themeColors.borderColor.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress()
                        tryAwaitRelease()
                        isPressed = false
                        onRelease()
                    }
                )
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) {
                Icon(icon, null, tint = themeColors.utilityColor, modifier = Modifier.size(13.dp))
            }
            Text(
                text = label,
                color = if (isPressed) themeColors.utilityColor else themeColors.textPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
