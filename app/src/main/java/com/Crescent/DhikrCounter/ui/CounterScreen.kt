package com.Crescent.DhikrCounter.ui

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Crescent.DhikrCounter.data.SessionEntity
import com.Crescent.DhikrCounter.ui.components.AnimatedCounter
import com.Crescent.DhikrCounter.ui.components.ProgressRing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    viewModel: CounterViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val activeSession by viewModel.activeSession.observeAsState()
    val isFloatingEnabled by viewModel.isFloatingEnabled.observeAsState(false)
    val isAnimationEnabled by viewModel.isCountAnimationEnabled.observeAsState(true)
    val isConfirmResetEnabled by viewModel.isConfirmResetEnabled.observeAsState(true)
    val cornerRadius by viewModel.cornerRadius.observeAsState(24f)
    val allSessions by viewModel.allSessions.observeAsState(emptyList())
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<SessionEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<SessionEntity?>(null) }
    val showResetDialog by viewModel.showResetConfirmation.observeAsState(false)
    var searchQuery by remember { mutableStateOf("") }

    val filteredSessions = allSessions.filter { it.name.contains(searchQuery, ignoreCase = true) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(340.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                drawerShape = RoundedCornerShape(topEnd = cornerRadius.dp, bottomEnd = cornerRadius.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Sessions",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    placeholder = { Text("Search sessions...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        NavigationDrawerItem(
                            label = { Text("New Session", fontWeight = FontWeight.SemiBold) },
                            selected = false,
                            onClick = { 
                                showAddDialog = true
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    
                    items(filteredSessions) { session ->
                        val isSelected = activeSession?.id == session.id
                        NavigationDrawerItem(
                            label = { 
                                Column {
                                    Text(session.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    if (session.category.isNotBlank()) {
                                        Text(session.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            selected = isSelected,
                            onClick = {
                                viewModel.setActiveSessionId(session.id)
                                scope.launch { drawerState.close() }
                            },
                            icon = { 
                                Icon(
                                    if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle, 
                                    contentDescription = null, 
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            badge = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            session.count.toString(),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            sessionToEdit = session
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (allSessions.size > 1) {
                                                sessionToDelete = session
                                                showDeleteDialog = true
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Cannot delete the only remaining session.")
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                NavigationDrawerItem(
                    label = { Text("Dashboard & Stats", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        onNavigateToDashboard()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Analytics, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                )
                
                NavigationDrawerItem(
                    label = { Text("Settings", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        onNavigateToSettings()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "DhikrCounter++", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .padding(paddingValues)
            ) {
                if (activeSession == null) {
                    EmptySessionState(
                        cornerRadius = cornerRadius,
                        onAddClick = { showAddDialog = true }
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Section: Session Info
                        SessionHeader(
                            session = activeSession!!,
                            cornerRadius = cornerRadius,
                            onEditClick = { showEditDialog = true }
                        )

                        Spacer(modifier = Modifier.weight(0.1f))

                        // Center Section: Counter & Ring
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            val session = activeSession!!
                            val progress = if (session.goalCount > 0) {
                                (session.count.toFloat() / session.goalCount.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            ProgressRing(
                                progress = progress,
                                modifier = Modifier.size(320.dp),
                                strokeWidth = 16.dp
                            )

                            // Inner Box ensures all center content uses the exact same center anchor
                            Box(
                                modifier = Modifier
                                    .size(320.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        viewModel.increment()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedCounter(
                                    count = session.count,
                                    enabled = isAnimationEnabled,
                                    textStyle = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = 80.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-2).sp
                                    )
                                )
                                
                                if (session.goalCount > 0) {
                                    val remaining = maxOf(0, session.goalCount - session.count)
                                    Surface(
                                        modifier = Modifier.offset(y = 72.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(cornerRadius.dp / 1.5f)
                                    ) {
                                        Text(
                                            text = "$remaining remaining",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Floating Toggle Button - Relocated for better accessibility
                            Surface(
                                onClick = {
                                    if (!isFloatingEnabled && !Settings.canDrawOverlays(context)) {
                                        val intent = android.content.Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        viewModel.toggleFloatingCounter()
                                    }
                                },
                                color = if (isFloatingEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 24.dp, bottom = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        if (isFloatingEnabled) Icons.Default.FilterCenterFocus else Icons.Outlined.FilterCenterFocus,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isFloatingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Floating",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFloatingEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Below Counter: Progress Bar & Stats
                        if (activeSession!!.goalCount > 0) {
                            SessionProgressDetails(activeSession!!, cornerRadius)
                        } else {
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        Spacer(modifier = Modifier.weight(0.1f))

                        // Bottom Section: Controls
                        MainControls(
                            onIncrement = { viewModel.increment() },
                            onDecrement = { viewModel.decrement() },
                            onReset = { 
                                if (isConfirmResetEnabled) {
                                    viewModel.showResetConfirmation.value = true
                                } else {
                                    viewModel.reset()
                                }
                            },
                            cornerRadius = cornerRadius
                        )
                        
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        NewSessionDialog(
            cornerRadius = cornerRadius,
            onDismiss = { showAddDialog = false },
            onConfirm = { name -> 
                viewModel.addSession(name)
                showAddDialog = false
            }
        )
    }

    if (showEditDialog) {
        val sessionForEdit = sessionToEdit ?: activeSession
        if (sessionForEdit != null) {
            EditSessionDialog(
                session = sessionForEdit,
                cornerRadius = cornerRadius,
                onDismiss = { 
                    showEditDialog = false
                    sessionToEdit = null
                },
                onConfirm = { updated ->
                    viewModel.updateSession(updated)
                    showEditDialog = false
                    sessionToEdit = null
                }
            )
        }
    }

    if (showDeleteDialog && sessionToDelete != null) {
        DeleteSessionDialog(
            cornerRadius = cornerRadius,
            onDismiss = { 
                showDeleteDialog = false
                sessionToDelete = null
            },
            onConfirm = {
                val toDelete = sessionToDelete!!
                viewModel.deleteSession(toDelete)
                
                // After deletion, automatically select a valid remaining session if we deleted the active one
                if (activeSession?.id == toDelete.id) {
                    val remaining = allSessions.filter { it.id != toDelete.id }
                    if (remaining.isNotEmpty()) {
                        viewModel.setActiveSessionId(remaining[0].id)
                    }
                }
                
                showDeleteDialog = false
                sessionToDelete = null
            }
        )
    }

    if (showResetDialog) {
        ResetConfirmationDialog(
            cornerRadius = cornerRadius,
            onDismiss = { viewModel.showResetConfirmation.value = false },
            onConfirm = {
                viewModel.reset()
                viewModel.showResetConfirmation.value = false
            }
        )
    }
}

@Composable
fun EmptySessionState(cornerRadius: Float, onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Layers, 
            contentDescription = null, 
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "No Active Session", 
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create a new session to start counting.", 
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(cornerRadius.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Session", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SessionHeader(session: SessionEntity, cornerRadius: Float, onEditClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEditClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(cornerRadius.dp / 2)
        ) {
            Text(
                text = session.category.ifBlank { "General" }.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                letterSpacing = 1.sp
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Outlined.Edit, 
                contentDescription = "Edit", 
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        if (session.goalCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Goal: ${session.goalCount}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SessionProgressDetails(session: SessionEntity, cornerRadius: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        val progress = if (session.goalCount > 0) {
            (session.count.toFloat() / session.goalCount.toFloat()).coerceIn(0f, 1f)
        } else 0f
        
        val percent = (progress * 100).toInt()
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec,
            label = "LinearProgressAnimation"
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "Overall Progress",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "$percent%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LinearWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(cornerRadius.dp / 2)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            amplitude = { p ->
                when {
                    p >= 0.99f -> 0f
                    p > 0.8f -> 0.4f
                    p > 0.4f -> 1.0f
                    else -> 0.3f
                }
            }
        )
    }
}

@Composable
fun MainControls(
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    cornerRadius: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset Button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalIconButton(
                onClick = onReset,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "RESET", 
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )
        }

        // Massive Increment Button
        Button(
            onClick = onIncrement,
            modifier = Modifier
                .size(140.dp),
            shape = RoundedCornerShape(cornerRadius.dp * 1.5f),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 10.dp,
                pressedElevation = 2.dp
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                Icons.Default.Add, 
                contentDescription = "Increment", 
                modifier = Modifier.size(80.dp)
            )
        }

        // Decrement Button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalIconButton(
                onClick = onDecrement,
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(cornerRadius.dp / 1.5f),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "MINUS", 
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun NewSessionDialog(cornerRadius: Float, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var sessionName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Session", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it },
                label = { Text("Session Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(cornerRadius.dp / 2)
            )
        },
        confirmButton = {
            Button(onClick = { if (sessionName.isNotBlank()) onConfirm(sessionName) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(cornerRadius.dp)
    )
}

@Composable
fun EditSessionDialog(session: SessionEntity, cornerRadius: Float, onDismiss: () -> Unit, onConfirm: (SessionEntity) -> Unit) {
    var name by remember { mutableStateOf(session.name) }
    var category by remember { mutableStateOf(session.category) }
    var goalStr by remember { mutableStateOf(if (session.goalCount > 0) session.goalCount.toString() else "") }
    var incrementStr by remember { mutableStateOf(session.incrementValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Session", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cornerRadius.dp / 2)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Morning, Custom)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(cornerRadius.dp / 2)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = goalStr,
                        onValueChange = { goalStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Goal") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(cornerRadius.dp / 2)
                    )
                    OutlinedTextField(
                        value = incrementStr,
                        onValueChange = { incrementStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Step") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(cornerRadius.dp / 2)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
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
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(cornerRadius.dp)
    )
}

@Composable
fun ResetConfirmationDialog(cornerRadius: Float, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Counter?", fontWeight = FontWeight.Bold) },
        text = {
            Text("Are you sure you want to reset this counter to 0?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(cornerRadius.dp)
    )
}

@Composable
fun DeleteSessionDialog(cornerRadius: Float, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Session?", fontWeight = FontWeight.Bold) },
        text = {
            Text("Are you sure you want to delete this session?\nThis action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(cornerRadius.dp)
    )
}
