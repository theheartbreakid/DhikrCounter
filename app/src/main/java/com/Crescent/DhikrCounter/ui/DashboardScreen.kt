package com.Crescent.DhikrCounter.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Crescent.DhikrCounter.DhikrApplication
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf

import androidx.compose.runtime.livedata.observeAsState
import com.Crescent.DhikrCounter.data.SessionStats
import com.Crescent.DhikrCounter.data.GlobalStats
import java.text.SimpleDateFormat
import java.util.*
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CounterViewModel,
    onNavigateBack: () -> Unit
) {
    val cornerRadius by viewModel.cornerRadius.observeAsState(24f)
    val sessionStats by viewModel.sessionStats.observeAsState(SessionStats())
    val globalStats by viewModel.globalStats.observeAsState(GlobalStats())
    val dailyActivity by viewModel.dailyActivity.observeAsState(emptyList())
    val dailyActivity30 by viewModel.dailyActivity30.observeAsState(emptyList())
    val sessionComparison by viewModel.sessionComparison.observeAsState(emptyList())
    val activeSession by viewModel.activeSession.observeAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Global Dashboard", "Session Statistics")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            Text(
                                title, 
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            ) 
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTabIndex == 0) {
                DashboardContent(cornerRadius, globalStats, sessionComparison)
            } else {
                StatisticsContent(cornerRadius, sessionStats, dailyActivity, dailyActivity30, activeSession?.name ?: "Select a Session")
            }
        }
    }
}

@Composable
fun DashboardContent(cornerRadius: Float, globalStats: GlobalStats, sessionComparison: List<Pair<String, Long>>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Global Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // Stats Grid
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Counts",
                        value = String.format(Locale.getDefault(), "%, d", globalStats.totalCount),
                        icon = Icons.Outlined.Functions,
                        color = MaterialTheme.colorScheme.primary,
                        cornerRadius = cornerRadius
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Active Sessions",
                        value = globalStats.activeSessions.toString(),
                        icon = Icons.Outlined.Layers,
                        color = MaterialTheme.colorScheme.secondary,
                        cornerRadius = cornerRadius
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Goals",
                        value = globalStats.totalGoalsCompleted.toString(),
                        icon = Icons.Outlined.EmojiEvents,
                        color = MaterialTheme.colorScheme.tertiary,
                        cornerRadius = cornerRadius
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Global Streak",
                        value = "${globalStats.currentStreak} Days",
                        icon = Icons.Outlined.LocalFireDepartment,
                        color = MaterialTheme.colorScheme.error,
                        cornerRadius = cornerRadius
                    )
                }
            }
        }

        if (sessionComparison.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Session Comparison", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    shape = RoundedCornerShape(cornerRadius.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val maxCount = sessionComparison.maxOf { it.second }.toFloat().coerceAtLeast(1f)
                        sessionComparison.take(5).forEach { (name, count) ->
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(String.format(Locale.getDefault(), "%, d", count), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                ProgressIndicatorWrapper(count.toFloat() / maxCount)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProgressIndicatorWrapper(progressValue: Float) {
    val context = LocalContext.current
    val settingsManager = (context.applicationContext as DhikrApplication).settingsManager
    val isWavyEnabled = settingsManager.isWavyProgressEnabled
    val wavyThickness = settingsManager.wavyThickness
    val wavyAmplitude = settingsManager.wavyAmplitude
    val wavyWavelength = settingsManager.wavyWavelength
    val wavyGapSize = settingsManager.wavyGapSize
    val wavyWaveSpeed = settingsManager.wavyWaveSpeed
    val wavyColorInt = settingsManager.getWavyColor()
    val wavyTrackColorInt = settingsManager.getWavyTrackColor()

    val indicatorColor = if (wavyColorInt != 0) Color(wavyColorInt) else MaterialTheme.colorScheme.primary
    val trackColor = if (wavyTrackColorInt != 0) Color(wavyTrackColorInt) else indicatorColor.copy(alpha = 0.1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progressValue,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "DashboardWavyProgressAnimation"
    )

    if (isWavyEnabled) {
        LinearWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(maxOf(14.dp, wavyThickness.dp + 6.dp)),
            color = indicatorColor,
            trackColor = trackColor,
            stroke = Stroke(width = with(androidx.compose.ui.platform.LocalDensity.current) { wavyThickness.dp.toPx() }, cap = StrokeCap.Round),
            trackStroke = Stroke(width = with(androidx.compose.ui.platform.LocalDensity.current) { wavyThickness.dp.toPx() }, cap = StrokeCap.Round),
            amplitude = { _ -> wavyAmplitude },
            wavelength = wavyWavelength.dp,
            gapSize = wavyGapSize.dp,
            waveSpeed = wavyWaveSpeed.dp
        )
    } else {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(maxOf(8.dp, wavyThickness.dp))
                .clip(RoundedCornerShape(4.dp)),
            color = indicatorColor,
            trackColor = trackColor
        )
    }
}

@Composable
fun StatisticsContent(cornerRadius: Float, stats: SessionStats, dailyActivity: List<Pair<Long, Long>>, dailyActivity30: List<Pair<Long, Long>>, sessionName: String) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    var daysToShow by remember { mutableIntStateOf(7) }
    val currentActivity = if (daysToShow == 7) dailyActivity else dailyActivity30
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(sessionName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text("Session Analytics", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            // Detailed Stats Grid
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStatCard(Modifier.weight(1f), "Lifetime", String.format(Locale.getDefault(), "%,d", stats.lifetimeCount), Icons.Outlined.AllInclusive, cornerRadius)
                    MiniStatCard(Modifier.weight(1f), "Today", String.format(Locale.getDefault(), "%,d", stats.todayCount), Icons.Outlined.Today, cornerRadius)
                    MiniStatCard(Modifier.weight(1f), "Goals", stats.goalsCompleted.toString(), Icons.Outlined.Flag, cornerRadius)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStatCard(Modifier.weight(1f), "Weekly", String.format(Locale.getDefault(), "%,d", stats.weeklyCount), Icons.Outlined.DateRange, cornerRadius)
                    MiniStatCard(Modifier.weight(1f), "Monthly", String.format(Locale.getDefault(), "%,d", stats.monthlyCount), Icons.Outlined.CalendarToday, cornerRadius)
                    MiniStatCard(Modifier.weight(1f), "Streak", "${stats.currentStreak}d", Icons.Outlined.LocalFireDepartment, cornerRadius)
                }
            }
        }

        if (currentActivity.isNotEmpty()) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Activity (Last $daysToShow Days)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row {
                        TextButton(onClick = { daysToShow = 7 }) {
                            Text("7D", color = if (daysToShow == 7) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { daysToShow = 30 }) {
                            Text("30D", color = if (daysToShow == 30) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(cornerRadius.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth().height(180.dp)) {
                        val entries = currentActivity.mapIndexed { index, pair ->
                            entryOf(index.toFloat(), pair.second.toFloat())
                        }
                        Chart(
                            chart = columnChart(),
                            model = entryModelOf(entries),
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis(),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(cornerRadius.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Longest Streak", style = MaterialTheme.typography.bodyMedium)
                        Text("${stats.longestStreak} Days", fontWeight = FontWeight.Bold)
                    }
                    if (stats.lastActivityAt > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Last Activity", style = MaterialTheme.typography.bodyMedium)
                            Text(dateFormat.format(Date(stats.lastActivityAt)), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (stats.createdAt > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Created On", style = MaterialTheme.typography.bodyMedium)
                            Text(dateFormat.format(Date(stats.createdAt)), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniStatCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, cornerRadius: Float) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, color: Color, cornerRadius: Float) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun StreakCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, isHot: Boolean, cornerRadius: Float) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        color = if (isHot) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (isHot) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, 
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title, 
                style = MaterialTheme.typography.labelMedium, 
                color = if (isHot) MaterialTheme.colorScheme.onErrorContainer.copy(alpha=0.7f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.7f)
            )
            Text(
                value, 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.ExtraBold, 
                color = if (isHot) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
