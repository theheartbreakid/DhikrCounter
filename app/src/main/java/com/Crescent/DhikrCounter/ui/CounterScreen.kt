package com.Crescent.DhikrCounter.ui

import androidx.compose.foundation.clickable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.Crescent.DhikrCounter.data.SessionEntity
import com.Crescent.DhikrCounter.data.SessionStats
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.scale
import com.Crescent.DhikrCounter.ui.components.*
import com.Crescent.DhikrCounter.ui.components.AdaptiveIcon
import com.Crescent.DhikrCounter.ui.components.LocalPrismalAdaptiveColor
import com.Crescent.DhikrCounter.ui.components.LocalGlassIntensity
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidDialog
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidIconButton
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidSurface
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidChip
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidButton
import com.Crescent.DhikrCounter.ui.components.catalog.utils.LocalBackdrop

@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    val activeSession by viewModel.activeSession.observeAsState()
    val sessionStats by viewModel.sessionStats.observeAsState(SessionStats())
    val cornerRadius by viewModel.cornerRadius.observeAsState(24f)
    val isFloatingEnabled by viewModel.isFloatingEnabled.observeAsState(false)
    val wavySettings = LocalWavySettings.current
    val backdrop = LocalBackdrop.current ?: rememberLayerBackdrop()

    var celebrationRingScale by remember { mutableStateOf(1f) }
    var celebrationNumberScale by remember { mutableStateOf(1f) }
    var celebrationGlassIntensity by remember { mutableStateOf(1f) }

    val context = LocalContext.current
    val settingsManager = remember { (context.applicationContext as com.Crescent.DhikrCounter.DhikrApplication).settingsManager }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSavedDialog by remember { mutableStateOf(false) }
    val showResetDialog by viewModel.showResetConfirmation.observeAsState(false)

    Box(modifier = Modifier.fillMaxSize()) {
        if (activeSession == null) {
            EmptySessionState(cornerRadius = cornerRadius, onAddClick = { showAddDialog = true })
        } else {
            val session = activeSession!!
            
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp),
                contentAlignment = Alignment.Center
            ) {
                val availableWidth = maxWidth
                val availableHeight = maxHeight
                val sizeDetails = com.Crescent.DhikrCounter.ui.components.LocalAppWindowSizeDetails.current
                val isWide = availableWidth >= 600.dp || (sizeDetails.isLandscape && availableHeight < availableWidth)

                val counterCircle: @Composable (androidx.compose.ui.unit.Dp) -> Unit = { circleSize ->
                    Box(
                        modifier = Modifier.size(circleSize),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = if (session.goalCount > 0) {
                            (session.count.toFloat() / session.goalCount.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        CompositionLocalProvider(
                            LocalGlassIntensity provides (1.0f * celebrationGlassIntensity)
                        ) {
                            LiquidSurface(
                                modifier = Modifier.fillMaxSize(0.88f * celebrationRingScale),
                                backdrop = backdrop,
                                tint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.1f),
                                shape = CircleShape,
                                adaptiveLuminance = true
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { viewModel.increment() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val contentColor = LocalPrismalAdaptiveColor.current
                                    ProgressRing(
                                        progress = progress,
                                        modifier = Modifier.fillMaxSize(0.9f),
                                        strokeWidth = if (wavySettings.isEnabled) wavySettings.thickness else 18f,
                                        trackStrokeWidth = if (wavySettings.isEnabled) wavySettings.trackThickness else 18f,
                                        gradient = if (wavySettings.isEnabled) null else Brush.sweepGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.primary
                                            )
                                        ),
                                        trackColor = if (wavySettings.trackColor != 0) Color(wavySettings.trackColor) else contentColor.copy(alpha = 0.1f),
                                        color = if (wavySettings.color != 0) Color(wavySettings.color) else contentColor,
                                        isWavy = wavySettings.isEnabled,
                                        amplitude = wavySettings.amplitude,
                                        wavelength = wavySettings.wavelength,
                                        gapSize = wavySettings.gapSize,
                                        waveSpeed = if (wavySettings.waveSpeedAuto) wavySettings.wavelength else wavySettings.waveSpeed
                                    )

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            session.name.uppercase(),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontSize = if (circleSize < 200.dp) 10.sp else 14.sp,
                                            color = contentColor.copy(alpha = 0.4f),
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = if (circleSize < 200.dp) 1.sp else 2.sp
                                        )
                                        Spacer(modifier = Modifier.height(if (circleSize < 200.dp) 2.dp else 8.dp))
                                        AnimatedCounter(
                                            count = session.count,
                                            enabled = true,
                                            modifier = Modifier.scale(celebrationNumberScale),
                                            textStyle = MaterialTheme.typography.displayLarge.copy(
                                                fontSize = if (circleSize < 200.dp) 44.sp else if (circleSize < 280.dp) 72.sp else 100.sp,
                                                fontWeight = FontWeight.Black,
                                                color = contentColor,
                                                letterSpacing = (-4).sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(if (circleSize < 200.dp) 2.dp else 4.dp))
                                        Text(
                                            "TAP TO COUNT",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = if (circleSize < 200.dp) 8.sp else 11.sp,
                                            color = contentColor.copy(alpha = 0.3f),
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }

                        GoalCelebration(
                            trigger = viewModel.goalReachedEvent,
                            hapticIntensity = settingsManager.getHapticIntensity(),
                            hapticEnabled = settingsManager.isHapticFeedbackEnabled,
                            onAnimationUpdate = { ringScale, numberScale, glassIntensity ->
                                celebrationRingScale = ringScale
                                celebrationNumberScale = numberScale
                                celebrationGlassIntensity = glassIntensity
                            }
                        )

                        // Floating Bubble Toggle
                        Box(
                            modifier = Modifier.fillMaxSize(0.85f),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            LiquidIconButton(
                                onClick = { viewModel.toggleFloatingCounter() },
                                backdrop = backdrop,
                                surfaceColor = if (isFloatingEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                modifier = Modifier.offset(x = 12.dp, y = 12.dp),
                                iconSize = if (circleSize < 200.dp) 36.dp else 48.dp,
                                adaptiveLuminance = true
                            ) {
                                AdaptiveIcon(
                                    Icons.Outlined.RadioButtonChecked,
                                    darkVariant = Icons.Filled.RadioButtonChecked,
                                    modifier = Modifier.size(if (circleSize < 200.dp) 18.dp else 24.dp),
                                    tint = LocalPrismalAdaptiveColor.current.copy(alpha = if (isFloatingEnabled) 1.0f else 0.5f)
                                )
                            }
                        }
                    }
                }

                val goalPill: @Composable () -> Unit = {
                    if (session.goalCount > 0) {
                        val remaining = (session.goalCount - session.count).coerceAtLeast(0)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LiquidChip(
                                tint = MaterialTheme.colorScheme.primary,
                                backdrop = backdrop,
                                onClick = { showEditDialog = true },
                                adaptiveLuminance = true
                            ) {
                                val contentColor = LocalPrismalAdaptiveColor.current
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "GOAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = contentColor.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = session.goalCount.toString(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Black,
                                        color = contentColor
                                    )
                                }
                            }
                            
                            if (remaining > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$remaining REMAINING",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalPrismalAdaptiveColor.current.copy(alpha = 0.4f),
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }
                    }
                }

                val mainControls: @Composable (Boolean) -> Unit = { isWideMode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidIconButton(
                            iconSize = if (isWideMode) 64.dp else 88.dp,
                            modifier = if (isWideMode) Modifier else Modifier.offset(y = 48.dp),
                            onClick = { viewModel.showResetConfirmation.value = true },
                            backdrop = backdrop,
                            surfaceColor = Color(0xFFFF5252).copy(alpha = 0.2f),
                            adaptiveLuminance = true
                        ) {
                            AdaptiveIcon(
                                Icons.Default.Refresh,
                                modifier = Modifier.size(if (isWideMode) 28.dp else 40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(if (isWideMode) 12.dp else 16.dp))

                        LiquidIconButton(
                            iconSize = if (isWideMode) 110.dp else 156.dp,
                            onClick = { viewModel.increment() },
                            backdrop = backdrop,
                            surfaceColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            adaptiveLuminance = true
                        ) {
                            AdaptiveIcon(
                                Icons.Default.Add,
                                modifier = Modifier.size(if (isWideMode) 54.dp else 76.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(if (isWideMode) 12.dp else 16.dp))

                        LiquidIconButton(
                            iconSize = if (isWideMode) 64.dp else 88.dp,
                            modifier = if (isWideMode) Modifier else Modifier.offset(y = 48.dp),
                            onClick = { viewModel.decrement() },
                            backdrop = backdrop,
                            surfaceColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                            adaptiveLuminance = true
                        ) {
                            AdaptiveIcon(
                                Icons.Default.Remove,
                                modifier = Modifier.size(if (isWideMode) 28.dp else 40.dp)
                            )
                        }
                    }
                }

                if (!isWide) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val circleSize = if (availableWidth * 0.88f < availableHeight * 0.45f) availableWidth * 0.88f else availableHeight * 0.45f
                        counterCircle(circleSize)
                        Spacer(modifier = Modifier.height(if (session.goalCount > 0) 24.dp else 40.dp))
                        goalPill()
                        Spacer(modifier = Modifier.height(24.dp))
                        mainControls(false)
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val circleSize = if (availableWidth * 0.45f < availableHeight * 0.85f) availableWidth * 0.45f else availableHeight * 0.85f
                        counterCircle(circleSize)
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            goalPill()
                            Spacer(modifier = Modifier.height(20.dp))
                            mainControls(true)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        NewSessionDialog(cornerRadius = cornerRadius, onDismiss = { showAddDialog = false }, onConfirm = { viewModel.addSession(it) })
    }

    if (showEditDialog && activeSession != null) {
        EditSessionDialog(
            session = activeSession!!,
            cornerRadius = cornerRadius,
            onDismiss = { showEditDialog = false },
            onConfirm = { 
                viewModel.updateSession(it)
                showEditDialog = false
                showSavedDialog = true
            }
        )
    }

    if (showSavedDialog) {
        LiquidDialog(
            onDismissRequest = { showSavedDialog = false },
            backdrop = backdrop,
            title = "Session Saved",
            message = "Your session has been saved successfully.",
            positiveText = "OK",
            onPositive = { showSavedDialog = false },
            icon = Icons.Outlined.Check,
            iconTint = Color(0xFF10B981)
        )
    }

    if (showResetDialog) {
        ResetConfirmationDialog(
            cornerRadius = cornerRadius,
            onDismiss = { viewModel.showResetConfirmation.value = false },
            onConfirm = { viewModel.reset(); viewModel.showResetConfirmation.value = false }
        )
    }
}

@Composable
fun EmptySessionState(cornerRadius: Float, onAddClick: () -> Unit) {
    val adaptiveColor = LocalPrismalAdaptiveColor.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AdaptiveIcon(
            Icons.Outlined.Layers,
            darkVariant = Icons.Filled.Layers,
            modifier = Modifier.size(100.dp),
            tint = adaptiveColor.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No Active Session",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = adaptiveColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create a new session to start counting.",
            style = MaterialTheme.typography.bodyLarge,
            color = adaptiveColor.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        LiquidButton(
            onClick = onAddClick,
            backdrop = LocalBackdrop.current ?: rememberLayerBackdrop()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = LocalPrismalAdaptiveColor.current)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Create Session", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LocalPrismalAdaptiveColor.current)
        }
    }
}

@Composable
fun NewSessionDialog(cornerRadius: Float, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var sessionName by remember { mutableStateOf("") }
    val backdrop = LocalBackdrop.current ?: rememberLayerBackdrop()

    LiquidDialog(
        onDismissRequest = onDismiss,
        backdrop = backdrop,
        title = "New Session",
        positiveText = "Create",
        onPositive = { if (sessionName.isNotBlank()) onConfirm(sessionName) },
        icon = Icons.Default.AddCircle,
        iconTint = MaterialTheme.colorScheme.primary
    ) {
        AdaptiveGlassTextField(
            value = sessionName,
            onValueChange = { sessionName = it },
            label = "Session Name",
            backdrop = backdrop
        )
    }
}

@Composable
fun EditSessionDialog(session: SessionEntity, cornerRadius: Float, onDismiss: () -> Unit, onConfirm: (SessionEntity) -> Unit) {
    var name by remember { mutableStateOf(session.name) }
    var category by remember { mutableStateOf(session.category) }
    var goalStr by remember { mutableStateOf(if (session.goalCount > 0) session.goalCount.toString() else "") }
    var incrementStr by remember { mutableStateOf(session.incrementValue.toString()) }

    val backdrop = LocalBackdrop.current ?: rememberLayerBackdrop()

    LiquidDialog(
        onDismissRequest = onDismiss,
        backdrop = backdrop,
        title = "Edit Session",
        positiveText = "Save",
        onPositive = {
            if (name.isNotBlank()) {
                val updated = session.copy(
                    name = name,
                    category = category,
                    goalCount = goalStr.toLongOrNull() ?: 0L,
                    incrementValue = incrementStr.toLongOrNull() ?: 1L,
                    decrementValue = incrementStr.toLongOrNull() ?: 1L
                )
                onConfirm(updated)
            }
        },
        icon = Icons.Default.Edit,
        iconTint = MaterialTheme.colorScheme.primary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AdaptiveGlassTextField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                backdrop = backdrop
            )
            AdaptiveGlassTextField(
                value = category,
                onValueChange = { category = it },
                label = "Category",
                backdrop = backdrop
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AdaptiveGlassTextField(
                    value = goalStr,
                    onValueChange = { goalStr = it.filter { char -> char.isDigit() } },
                    label = "Goal",
                    modifier = Modifier.weight(1f),
                    backdrop = backdrop,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                AdaptiveGlassTextField(
                    value = incrementStr,
                    onValueChange = { incrementStr = it.filter { char -> char.isDigit() } },
                    label = "Step",
                    modifier = Modifier.weight(1f),
                    backdrop = backdrop,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}

@Composable
fun ResetConfirmationDialog(cornerRadius: Float, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val backdrop = LocalBackdrop.current ?: rememberLayerBackdrop()
    LiquidDialog(
        onDismissRequest = onDismiss,
        backdrop = backdrop,
        title = "Reset Counter?",
        message = "Are you sure you want to reset this counter to 0?",
        positiveText = "Reset",
        onPositive = onConfirm,
        icon = Icons.Default.Refresh,
        iconTint = MaterialTheme.colorScheme.primary
    )
}
