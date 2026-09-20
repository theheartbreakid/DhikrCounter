package com.Crescent.DhikrCounter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Crescent.DhikrCounter.data.GlobalStats
import com.Crescent.DhikrCounter.data.HistoryEntity
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.Crescent.DhikrCounter.ui.components.*
import com.Crescent.DhikrCounter.ui.components.AdaptiveIcon
import com.Crescent.DhikrCounter.ui.components.LocalPrismalAdaptiveColor
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidCard
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidButton
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidIconButton
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidDialog
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidSurface
import com.Crescent.DhikrCounter.ui.components.catalog.utils.LocalBackdrop
import com.Crescent.DhikrCounter.ui.components.LocalDockSettings
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: CounterViewModel) {
    val globalStats by viewModel.globalStats.observeAsState(GlobalStats())
    val history by viewModel.allHistoryFlow.collectAsState(initial = emptyList())
    val adaptiveColor = LocalPrismalAdaptiveColor.current
    val locale = java.util.Locale.getDefault()
    val backdrop = LocalBackdrop.current ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop()

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val dayFormat = remember(locale) { SimpleDateFormat("yyyyMMdd", locale) }
    val bestDayCount = remember(history, dayFormat) {
        history.groupBy {
            dayFormat.format(Date(it.timestamp))
        }.values.maxOfOrNull { dayEntries ->
            dayEntries.sumOf { it.countChange }.coerceAtLeast(0L)
        } ?: 0L
    }

    val groupedHistory = remember(history, locale) {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", locale)
        history.groupBy {
            dateFormat.format(Date(it.timestamp))
        }
    }

    val sizeDetails = com.Crescent.DhikrCounter.ui.components.LocalAppWindowSizeDetails.current
    val isWide = sizeDetails.widthClass == com.Crescent.DhikrCounter.ui.components.AppWindowWidthSizeClass.EXPANDED
    val isCompact = sizeDetails.heightClass == com.Crescent.DhikrCounter.ui.components.AppWindowHeightSizeClass.COMPACT

    Column(modifier = Modifier.fillMaxSize()) {
        // Persistent Header (Title & Clear)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (isWide) 48.dp else 32.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = adaptiveColor
                )
                Text(
                    "${history.size} Logged Sessions",
                    style = MaterialTheme.typography.labelMedium,
                    color = adaptiveColor.copy(alpha = 0.5f)
                )
            }

            if (history.isNotEmpty()) {
                LiquidIconButton(
                    onClick = { showClearHistoryDialog = true },
                    backdrop = backdrop,
                    surfaceColor = Color.Red.copy(alpha = 0.1f),
                    adaptiveLuminance = true
                ) {
                    Icon(Icons.Outlined.DeleteForever, "Clear History", tint = Color.Red)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = if (isWide) 48.dp else 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary chips Item
            item(key = "summary_chips") {
                if (isWide || (sizeDetails.isLandscape && !isCompact)) {
                    // Single Row for wide/tablet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HistorySummaryChip(Modifier.weight(1f), "Total Sessions", String.format(locale, "%,d", history.size))
                        HistorySummaryChip(Modifier.weight(1f), "Best Day", String.format(locale, "%,d", bestDayCount))
                        HistorySummaryChip(Modifier.weight(1f), "Average", String.format(locale, "%.1f", globalStats.totalCount.toFloat() / maxOf(1, globalStats.daysActive)))
                        HistorySummaryChip(Modifier.weight(1f), "Total Count", String.format(locale, "%,d", globalStats.totalCount))
                    }
                } else {
                    // 2-column grid for phones/portrait
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HistorySummaryChip(Modifier.weight(1f), "Total Sessions", String.format(locale, "%,d", history.size))
                            HistorySummaryChip(Modifier.weight(1f), "Best Day", String.format(locale, "%,d", bestDayCount))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HistorySummaryChip(Modifier.weight(1f), "Average", String.format(locale, "%.1f", globalStats.totalCount.toFloat() / maxOf(1, globalStats.daysActive)))
                            HistorySummaryChip(Modifier.weight(1f), "Total Count", String.format(locale, "%,d", globalStats.totalCount))
                        }
                    }
                }
            }

            item(key = "recent_activity_header") {
                Text(
                    "Recent Activity",
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = adaptiveColor
                )
            }

            if (history.isEmpty()) {
                item(key = "empty_state") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.History,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = adaptiveColor.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No history yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = adaptiveColor.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                val today = SimpleDateFormat("MMMM dd, yyyy", locale).format(Date())
                groupedHistory.forEach { (date, entries) ->
                    item(key = "header_$date") {
                        Text(
                            text = if (date == today) "Today" else date,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = adaptiveColor.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(entries, key = { it.id }) { entry ->
                        HistoryListItem(entry)
                    }
                }
            }

            item(key = "footer_spacer") { Spacer(modifier = Modifier.height(110.dp)) }
        }
    }

    if (showClearHistoryDialog) {
        LiquidDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            backdrop = backdrop,
            title = "Clear History?",
            message = "This will permanently clear all history data. This action cannot be undone.",
            positiveText = "Clear",
            onPositive = {
                viewModel.clearHistory()
                showClearHistoryDialog = false
            }
        ) {
            // Content empty
        }
    }
}

@Composable
fun HistorySummaryChip(modifier: Modifier = Modifier, label: String, value: String) {
    val backdrop = LocalBackdrop.current ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    LiquidCard(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        backdrop = backdrop,
        tint = MaterialTheme.colorScheme.surfaceContainer,
        adaptiveLuminance = true
    ) {
        val adaptiveColor = LocalPrismalAdaptiveColor.current
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = adaptiveColor.copy(alpha = 0.6f))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = adaptiveColor)
        }
    }
}

@Composable
fun HistoryListItem(entry: HistoryEntity) {
    val backdrop = LocalBackdrop.current ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    val locale = java.util.Locale.getDefault()
    val timeFormat = remember(locale) { SimpleDateFormat("hh:mm a", locale) }

    val icon = if (entry.eventType == "SESSION") {
        if (entry.isGoalMet) Icons.Outlined.CheckCircle else Icons.Outlined.Timer
    } else {
        when (entry.eventType) {
            "INCREMENT" -> Icons.Outlined.AddCircle
            "DECREMENT" -> Icons.Outlined.RemoveCircle
            "RESET" -> Icons.Outlined.Refresh
            "GOAL_COMPLETED" -> Icons.Outlined.Star
            else -> Icons.Outlined.History
        }
    }
    
    val iconColor = if (entry.eventType == "SESSION") {
        if (entry.isGoalMet) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
    } else {
        when (entry.eventType) {
            "INCREMENT" -> MaterialTheme.colorScheme.primary
            "DECREMENT" -> MaterialTheme.colorScheme.secondary
            "RESET" -> Color(0xFFF59E0B)
            "GOAL_COMPLETED" -> Color(0xFF10B981)
            else -> LocalPrismalAdaptiveColor.current
        }
    }

    LiquidCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backdrop = backdrop,
        adaptiveLuminance = true
    ) {
        val adaptiveColor = LocalPrismalAdaptiveColor.current
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColoredIconChip(icon = icon, color = iconColor)
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(entry.sessionName, fontWeight = FontWeight.Bold, color = adaptiveColor, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (entry.eventType == "GOAL_COMPLETED") "Goal Reached!" else timeFormat.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (entry.eventType == "GOAL_COMPLETED") Color(0xFF10B981) else adaptiveColor.copy(alpha = 0.5f),
                        fontWeight = if (entry.eventType == "GOAL_COMPLETED") FontWeight.Bold else FontWeight.Normal
                    )
                    if (entry.duration > 0) {
                        val minutes = entry.duration / 60000
                        val seconds = (entry.duration % 60000) / 1000
                        val durationText = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
                        Text(" • $durationText", style = MaterialTheme.typography.bodySmall, color = adaptiveColor.copy(alpha = 0.5f))
                    }
                    if (entry.eventType == "GOAL_COMPLETED") {
                         Text(" • ${timeFormat.format(Date(entry.timestamp))}", style = MaterialTheme.typography.bodySmall, color = adaptiveColor.copy(alpha = 0.5f))
                    }
                }
            }
            
            val prefix = if (entry.countChange > 0) "+" else ""
            if (entry.countChange != 0L || entry.eventType == "SESSION") {
                Text(
                    text = "$prefix${entry.countChange}",
                    fontWeight = FontWeight.Black,
                    color = adaptiveColor,
                    fontSize = 18.sp
                )
            }
            
            if (entry.isGoalMet) {
                Icon(
                    Icons.Outlined.Star,
                    contentDescription = "Goal Met",
                    modifier = Modifier.padding(start = 8.dp).size(16.dp),
                    tint = Color(0xFFF59E0B)
                )
            } else {
                AdaptiveIcon(
                    Icons.Outlined.ChevronRight,
                    modifier = Modifier.padding(start = 8.dp),
                    tint = adaptiveColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}
