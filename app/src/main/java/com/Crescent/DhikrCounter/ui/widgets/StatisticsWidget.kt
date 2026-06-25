package com.Crescent.DhikrCounter.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.data.GlobalStats

class StatisticsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DhikrApplication
        val stats = app.historyRepository.getGlobalStats()
        val daily = app.historyRepository.getDailyActivity(null, 30)
        val todayCount = daily.lastOrNull()?.second ?: 0L
        val weeklyCount = daily.takeLast(7).sumOf { it.second }
        val monthlyCount = daily.sumOf { it.second }
        
        provideContent {
            GlanceTheme {
                StatisticsLayout(context, stats, todayCount, weeklyCount, monthlyCount)
            }
        }
    }

    @Composable
    private fun StatisticsLayout(context: Context, stats: GlobalStats, today: Long, weekly: Long, monthly: Long) {
        val padding = getWidgetPadding(context)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .applyWidgetStyle(context)
                .clickable(actionRunCallback<OpenAppAction>()),
        ) {
            Text(
                text = "STATISTICS",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = getWidgetFontSize(context, 14f),
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(GlanceModifier.height(padding))
            
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                StatBox(context, "Today", today.toString(), GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(padding))
                StatBox(context, "Weekly", formatCount(weekly), GlanceModifier.defaultWeight())
            }
            
            Spacer(GlanceModifier.height(padding))
            
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                StatBox(context, "Monthly", formatCount(monthly), GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(padding))
                StatBox(context, "Lifetime", formatCount(stats.totalCount), GlanceModifier.defaultWeight())
            }
            
            Spacer(GlanceModifier.height(padding))
            
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Box(modifier = GlanceModifier.defaultWeight()) {
                    StatDetail(context, "Active Sessions", stats.activeSessions.toString())
                }
                Box(modifier = GlanceModifier.defaultWeight()) {
                    StatDetail(context, "Goals Done", stats.totalGoalsCompleted.toString())
                }
            }
            
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Box(modifier = GlanceModifier.defaultWeight()) {
                    StatDetail(context, "Streak", "${stats.currentStreak} days")
                }
                Box(modifier = GlanceModifier.defaultWeight()) {
                    StatDetail(context, "Best Streak", "${stats.longestStreak} days")
                }
            }
        }
    }

    @Composable
    private fun StatBox(context: Context, label: String, value: String, modifier: GlanceModifier) {
        val app = context.applicationContext as DhikrApplication
        val radius = app.settingsManager.getWidgetCornerRadius() / 2f
        Column(
            modifier = modifier
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(radius.dp)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer, 
                    fontSize = getWidgetFontSize(context, 16f), 
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = label,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer, 
                    fontSize = getWidgetFontSize(context, 9f)
                )
            )
        }
    }

    @Composable
    private fun StatDetail(context: Context, label: String, value: String) {
        Column(modifier = GlanceModifier.padding(vertical = 2.dp)) {
            Text(
                text = label,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant, 
                    fontSize = getWidgetFontSize(context, 9f)
                )
            )
            Text(
                text = value,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface, 
                    fontSize = getWidgetFontSize(context, 12f), 
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
    
    private fun formatCount(count: Long): String {
        return if (count >= 1000000) {
            String.format("%.1fM", count / 1000000f)
        } else if (count >= 1000) {
            String.format("%.1fK", count / 1000f)
        } else {
            count.toString()
        }
    }
}

class StatisticsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatisticsWidget()
}
