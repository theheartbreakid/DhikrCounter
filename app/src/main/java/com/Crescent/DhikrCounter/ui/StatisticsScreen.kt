package com.Crescent.DhikrCounter.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Crescent.DhikrCounter.data.GlobalStats
import com.Crescent.DhikrCounter.ui.components.*
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidCard
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidSurface
import com.Crescent.DhikrCounter.ui.components.catalog.utils.LocalBackdrop
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.marker.markerComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.core.chart.DefaultPointConnector
import com.patrykandpatrick.vico.core.component.shape.DashedShape
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.dimensions.MutableDimensions
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.marker.Marker
import java.util.*

@Composable
fun StatisticsScreen(viewModel: CounterViewModel) {
    val dailyActivity by viewModel.statisticsDailyActivity.observeAsState(emptyList())
    val sessionComparison by viewModel.statisticsSessionComparison.observeAsState(emptyList())
    val periodStats by viewModel.statisticsPeriodStats.observeAsState(com.Crescent.DhikrCounter.data.PeriodStats())
    val adaptiveColor = LocalPrismalAdaptiveColor.current
    val backdrop = LocalBackdrop.current ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    val dockSettings = LocalDockSettings.current
    val selectedFilter by viewModel.statisticsRange.observeAsState("7D")
    var selectedChartType by remember { mutableStateOf(ChartType.TREND) }
    
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]

    val sizeDetails = com.Crescent.DhikrCounter.ui.components.LocalAppWindowSizeDetails.current
    val isWide = sizeDetails.widthClass == com.Crescent.DhikrCounter.ui.components.AppWindowWidthSizeClass.EXPANDED
    val isCompactHeight = sizeDetails.heightClass == com.Crescent.DhikrCounter.ui.components.AppWindowHeightSizeClass.COMPACT
    val topPadding = if (isCompactHeight) 110.dp else 210.dp

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(top = topPadding, bottom = 120.dp, start = if (isWide) 48.dp else 24.dp, end = if (isWide) 48.dp else 24.dp)
        ) {
            item {
                val chartContent: @Composable () -> Unit = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (selectedChartType == ChartType.TREND) "Activity Trend" else "Session Comparison",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveColor.copy(alpha = 0.7f)
                            )

                            ChartTypeToggle(
                                selectedType = selectedChartType,
                                onTypeSelected = { selectedChartType = it },
                                adaptiveColor = adaptiveColor
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Activity Card
                        LiquidCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isCompactHeight) 220.dp else 280.dp),
                            backdrop = backdrop,
                            shape = RoundedCornerShape(32.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                        ) {
                            Box(modifier = Modifier.padding(20.dp)) {
                                val primaryColor = MaterialTheme.colorScheme.primary
                                
                                if (periodStats.totalCount == 0L) {
                                    Text("No data available for this period", modifier = Modifier.align(Alignment.Center), color = adaptiveColor.copy(alpha = 0.4f))
                                } else if (selectedChartType == ChartType.TREND) {
                                    if (dailyActivity.isNotEmpty()) {
                                        val entries = dailyActivity.mapIndexed { index, pair ->
                                            entryOf(index.toFloat(), pair.second.toFloat())
                                        }

                                        val marker = rememberMarker()
                                        val persistentMarker = remember(entries, marker) {
                                            if (entries.isNotEmpty()) mapOf(entries.last().x to marker) else emptyMap<Float, Marker>()
                                        }

                                        Chart(
                                            chart = lineChart(
                                                lines = listOf(
                                                    lineSpec(
                                                        lineColor = primaryColor,
                                                        lineBackgroundShader = DynamicShaders.fromBrush(
                                                            Brush.verticalGradient(
                                                                colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent)
                                                            )
                                                        ),
                                                        pointConnector = DefaultPointConnector(cubicStrength = 0.2f)
                                                    )
                                                ),
                                                persistentMarkers = persistentMarker
                                            ),
                                            model = entryModelOf(entries),
                                            modifier = Modifier.fillMaxSize(),
                                            startAxis = rememberStartAxis(
                                                label = textComponent(color = adaptiveColor.copy(alpha = 0.5f), textSize = 10.sp),
                                                guideline = null,
                                                axis = null
                                            ),
                                            bottomAxis = rememberBottomAxis(
                                                label = textComponent(color = adaptiveColor.copy(alpha = 0.5f), textSize = 10.sp),
                                                guideline = null,
                                                axis = null,
                                                valueFormatter = { value, _ ->
                                                    val idx = value.toInt()
                                                    dailyActivity.getOrNull(idx)?.let { pair ->
                                                        val showLabel = when (selectedFilter) {
                                                            "7D" -> true
                                                            "30D" -> idx % 5 == 0 || idx == dailyActivity.lastIndex
                                                            "90D" -> idx % 15 == 0 || idx == dailyActivity.lastIndex
                                                            "All" -> idx % (maxOf(1, dailyActivity.size / 6)) == 0 || idx == dailyActivity.lastIndex
                                                            else -> true
                                                        }
                                                        if (showLabel) {
                                                            val pattern = when (selectedFilter) {
                                                                "7D" -> "EEE"
                                                                "30D", "90D" -> "d MMM"
                                                                "All" -> "MMM yy"
                                                                else -> "d"
                                                            }
                                                            val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
                                                            sdf.format(java.util.Date(pair.first))
                                                        } else ""
                                                    } ?: ""
                                                }
                                            ),
                                            marker = marker
                                        )
                                    } else {
                                        Text("No activity data", modifier = Modifier.align(Alignment.Center), color = adaptiveColor.copy(alpha = 0.4f))
                                    }
                                } else {
                                    if (sessionComparison.isNotEmpty()) {
                                        val entries = sessionComparison.mapIndexed { index, pair ->
                                            entryOf(index.toFloat(), pair.second.toFloat())
                                        }

                                        Chart(
                                            chart = columnChart(
                                                columns = listOf(
                                                    lineComponent(
                                                        color = primaryColor,
                                                        thickness = 12.dp,
                                                        shape = Shapes.pillShape
                                                    )
                                                )
                                            ),
                                            model = entryModelOf(entries),
                                            modifier = Modifier.fillMaxSize(),
                                            startAxis = rememberStartAxis(
                                                label = textComponent(color = adaptiveColor.copy(alpha = 0.5f), textSize = 10.sp),
                                                guideline = null,
                                                axis = null
                                            ),
                                            bottomAxis = rememberBottomAxis(
                                                label = textComponent(color = adaptiveColor.copy(alpha = 0.5f), textSize = 8.sp),
                                                guideline = null,
                                                axis = null,
                                                valueFormatter = { value, _ ->
                                                    sessionComparison.getOrNull(value.toInt())?.first ?: ""
                                                }
                                            )
                                        )
                                    } else {
                                        Text("No session data", modifier = Modifier.align(Alignment.Center), color = adaptiveColor.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }

                val statsContent: @Composable () -> Unit = {
                    Column {
                        Text("Performance Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = adaptiveColor.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Total Counts Card
                            StatTile(
                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                title = if (selectedFilter == "All") "Total Lifetime Counts" else "Total Counts ($selectedFilter)",
                                value = if (periodStats.totalCount == 0L) "No Activity" else String.format(locale, "%,d", periodStats.totalCount),
                                icon = Icons.Outlined.Functions,
                                accentColor = Color(0xFF6366F1),
                                large = true
                            )
                            
                            // Stats Grid Container
                            LiquidCard(
                                modifier = Modifier.fillMaxWidth(),
                                backdrop = backdrop,
                                shape = RoundedCornerShape(32.dp),
                                tint = Color.White.copy(alpha = 0.02f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        StatTileInner(
                                            modifier = Modifier.weight(1f),
                                            title = "Max/Day",
                                            value = if (periodStats.totalCount == 0L) "-" else String.format(locale, "%,d", periodStats.maxCountPerDay),
                                            icon = Icons.Outlined.AutoGraph,
                                            accentColor = Color(0xFFF59E0B),
                                            adaptiveColor = adaptiveColor
                                        )
                                        StatTileInner(
                                            modifier = Modifier.weight(1f),
                                            title = "Goals",
                                            value = if (periodStats.totalCount == 0L) "-" else periodStats.goalsCompleted.toString(),
                                            icon = Icons.Outlined.EmojiEvents,
                                            accentColor = Color(0xFF10B981),
                                            adaptiveColor = adaptiveColor
                                        )
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        StatTileInner(
                                            modifier = Modifier.weight(1f),
                                            title = "Avg/Day",
                                            value = if (periodStats.totalCount == 0L) "-" else String.format(locale, "%,d", periodStats.averagePerDay),
                                            icon = Icons.Outlined.BarChart,
                                            accentColor = Color(0xFF3B82F6),
                                            adaptiveColor = adaptiveColor
                                        )
                                        StatTileInner(
                                            modifier = Modifier.weight(1f),
                                            title = "Active",
                                            value = if (periodStats.totalCount == 0L) "-" else "${periodStats.daysActive}d",
                                            icon = Icons.Outlined.CalendarToday,
                                            accentColor = Color(0xFFEC4899),
                                            adaptiveColor = adaptiveColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isWide || (sizeDetails.isLandscape && !isCompactHeight)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        Box(modifier = Modifier.weight(1.2f)) { chartContent() }
                        Box(modifier = Modifier.weight(1f)) { statsContent() }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                        chartContent()
                        statsContent()
                    }
                }
            }
        }

        // Floating Top Bar Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .padding(horizontal = 24.dp, vertical = if (isCompactHeight) 12.dp else 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            LiquidSurface(
                backdrop = backdrop,
                shape = RoundedCornerShape(dockSettings.cornerRadius.dp),
                tint = Color.White.copy(alpha = 0.02f),
                adaptiveLuminance = true
            ) {
                if (isCompactHeight) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Statistics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = adaptiveColor,
                            letterSpacing = (-1).sp
                        )

                        Box(modifier = Modifier.width(300.dp)) {
                            SegmentedControl(
                                options = listOf("7D", "30D", "90D", "All"),
                                selectedOption = selectedFilter,
                                onOptionSelected = { viewModel.statisticsRange.value = it },
                                adaptiveColor = adaptiveColor,
                                isFlat = true
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Statistics",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = adaptiveColor,
                            letterSpacing = (-1).sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SegmentedControl(
                            options = listOf("7D", "30D", "90D", "All"),
                            selectedOption = selectedFilter,
                            onOptionSelected = { viewModel.statisticsRange.value = it },
                            adaptiveColor = adaptiveColor,
                            isFlat = true
                        )
                    }
                }
            }
        }
    }
}

enum class ChartType {
    TREND, COMPARISON
}

@Composable
fun ChartTypeToggle(
    selectedType: ChartType,
    onTypeSelected: (ChartType) -> Unit,
    adaptiveColor: Color
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(adaptiveColor.copy(alpha = 0.08f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ChartType.entries.forEach { type ->
            val isSelected = selectedType == type
            val icon = if (type == ChartType.TREND) Icons.AutoMirrored.Outlined.ShowChart else Icons.Outlined.BarChart
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onTypeSelected(type) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) Color.White else adaptiveColor.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    adaptiveColor: Color,
    isFlat: Boolean = false
) {
    val backdrop = LocalBackdrop.current ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    
    val content = @Composable {
        Row(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                val backgroundColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) 
                    else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )
                val textColor by animateColorAsState(
                    if (isSelected) Color.White 
                    else adaptiveColor.copy(alpha = 0.5f)
                )
                val scale by animateFloatAsState(if (isSelected) 1f else 0.95f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .clickable { onOptionSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        option, 
                        color = textColor, 
                        fontSize = 13.sp, 
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }
    }

    if (isFlat) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(adaptiveColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    } else {
        LiquidCard(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = CircleShape,
            backdrop = backdrop,
            tint = Color.White.copy(alpha = 0.05f)
        ) {
            content()
        }
    }
}

@Composable
fun StatTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    large: Boolean = false
) {
    val backdrop = LocalBackdrop.current ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    LiquidCard(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        backdrop = backdrop,
        tint = accentColor.copy(alpha = 0.08f),
        adaptiveLuminance = true
    ) {
        val adaptiveColor = LocalPrismalAdaptiveColor.current
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
            ColoredIconChip(icon = icon, color = accentColor, size = if (large) 48.dp else 40.dp)
            Spacer(modifier = Modifier.height(if (large) 12.dp else 8.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = adaptiveColor.copy(alpha = 0.5f), letterSpacing = 1.sp)
            Text(value, style = if (large) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = adaptiveColor)
        }
    }
}

@Composable
fun StatTileInner(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    adaptiveColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(16.dp)
    ) {
        Column {
            ColoredIconChip(icon = icon, color = accentColor, size = 32.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = adaptiveColor.copy(alpha = 0.5f), fontSize = 10.sp, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = adaptiveColor)
        }
    }
}

@Composable
fun ColoredIconChip(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3f))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size / 2f)
        )
    }
}

@Composable
fun rememberMarker(): Marker {
    val label = textComponent(
        color = Color.White,
        background = shapeComponent(
            shape = Shapes.pillShape,
            color = MaterialTheme.colorScheme.primary
        ),
        padding = MutableDimensions(4f, 2f, 4f, 2f),
    )
    val indicator = shapeComponent(
        shape = Shapes.pillShape,
        color = Color.White,
        strokeColor = MaterialTheme.colorScheme.primary,
        strokeWidth = 2.dp
    )
    val guideline = lineComponent(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        thickness = 1.dp,
        shape = DashedShape(
            Shapes.rectShape,
            4f,
            4f
        )
    )
    return markerComponent(
        label = label,
        indicator = indicator,
        guideline = guideline
    )
}
