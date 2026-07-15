package com.Crescent.DhikrCounter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Crescent.DhikrCounter.data.SessionEntity
import com.Crescent.DhikrCounter.ui.components.*
import com.Crescent.DhikrCounter.ui.components.AdaptiveIcon
import com.Crescent.DhikrCounter.ui.components.LocalPrismalAdaptiveColor
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidDialog
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidButton
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidIconButton
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidSurface
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidCard
import com.Crescent.DhikrCounter.ui.components.catalog.utils.LocalBackdrop

@Composable
fun SessionsScreen(viewModel: CounterViewModel) {
    val activeSession by viewModel.activeSession.observeAsState()
    val allSessions by viewModel.allSessions.observeAsState(emptyList())
    val cornerRadius by viewModel.cornerRadius.observeAsState(24f)
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<SessionEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSessions = remember(allSessions, searchQuery) {
        allSessions.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val adaptiveColor = LocalPrismalAdaptiveColor.current
    val listState = rememberLazyListState()
    val backdrop = LocalBackdrop.current ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Sessions", 
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black, 
                    color = adaptiveColor
                )
                Text(
                    "${allSessions.size} Active Goals",
                    style = MaterialTheme.typography.labelMedium,
                    color = adaptiveColor.copy(alpha = 0.5f)
                )
            }
            
            LiquidButton(
                onClick = { showAddDialog = true },
                backdrop = backdrop,
                tint = MaterialTheme.colorScheme.primary,
                adaptiveLuminance = true
            ) {
                AdaptiveIcon(Icons.Default.Add, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("New", fontWeight = FontWeight.Bold, color = adaptiveColor)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Search Bar (Redesigned Fix Pass 26)
        LiquidSurface(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            backdrop = backdrop,
            shape = RoundedCornerShape(20.dp),
            tint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.15f),
            adaptiveLuminance = true
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdaptiveIcon(
                    Icons.Outlined.Search, 
                    darkVariant = Icons.Filled.Search, 
                    modifier = Modifier.size(22.dp), 
                    tint = adaptiveColor.copy(alpha = 0.4f)
                )
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Search sessions...", 
                            color = adaptiveColor.copy(alpha = 0.4f), 
                            fontSize = 16.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = adaptiveColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(adaptiveColor),
                        singleLine = true
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = null, 
                            modifier = Modifier.size(16.dp),
                            tint = adaptiveColor.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(filteredSessions, key = { it.id }) { session ->
                val isSelected = activeSession?.id == session.id
                
                LiquidCard(
                    onClick = { viewModel.setActiveSessionId(session.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backdrop = backdrop,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    adaptiveLuminance = true
                ) {
                    val adaptiveColor = LocalPrismalAdaptiveColor.current
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconColor = when (session.id % 5) {
                            0L -> Color(0xFF8B5CF6)
                            1L -> Color(0xFFF43F5E)
                            2L -> Color(0xFF10B981)
                            3L -> Color(0xFFF59E0B)
                            else -> Color(0xFF3B82F6)
                        }
                        
                        ColoredIconChip(icon = Icons.Outlined.Circle, color = iconColor)
                        
                        Spacer(Modifier.width(20.dp))
                        
                        Column(Modifier.weight(1f)) {
                            Text(
                                session.name, 
                                fontWeight = FontWeight.Bold,
                                color = adaptiveColor,
                                fontSize = 17.sp,
                                letterSpacing = (-0.2).sp
                            )
                            Text(
                                if (session.category.isNotBlank()) session.category.uppercase() else "GENERAL", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = adaptiveColor.copy(alpha = 0.4f),
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                session.count.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = adaptiveColor
                            )
                            if (session.goalCount > 0) {
                                Text(
                                    "/ ${session.goalCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = adaptiveColor.copy(alpha = 0.3f)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        LiquidIconButton(
                            onClick = { sessionToEdit = session; showEditDialog = true },
                            backdrop = backdrop,
                            iconSize = 42.dp,
                            adaptiveLuminance = true
                        ) {
                            AdaptiveIcon(Icons.Default.Edit, modifier = Modifier.size(20.dp), tint = adaptiveColor.copy(alpha = 0.4f))
                        }

                        Spacer(Modifier.width(8.dp))

                        LiquidIconButton(
                            onClick = { 
                                sessionToEdit = session
                                showDeleteDialog = true 
                            },
                            backdrop = backdrop,
                            iconSize = 42.dp,
                            surfaceColor = Color.Red.copy(alpha = 0.1f),
                            adaptiveLuminance = true
                        ) {
                            AdaptiveIcon(Icons.Default.Delete, modifier = Modifier.size(20.dp), tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        NewSessionDialog(cornerRadius = cornerRadius, onDismiss = { showAddDialog = false }, onConfirm = { viewModel.addSession(it) })
    }
    if (showEditDialog && sessionToEdit != null) {
        EditSessionDialog(session = sessionToEdit!!, cornerRadius = cornerRadius, onDismiss = { showEditDialog = false }, onConfirm = { viewModel.updateSession(it) })
    }

    if (showDeleteDialog && sessionToEdit != null) {
        val backdrop = LocalBackdrop.current
        LiquidDialog(
            onDismissRequest = { showDeleteDialog = false },
            backdrop = backdrop ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop(),
            title = "Delete Session?",
            message = "This action cannot be undone. Are you sure you want to delete this session?",
            positiveText = "Delete",
            onPositive = {
                viewModel.deleteSession(sessionToEdit!!)
                showDeleteDialog = false
            },
            icon = Icons.Default.Delete,
            iconTint = Color(0xFFFF3B30)
        )
    }
}
