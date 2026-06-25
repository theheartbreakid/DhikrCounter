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
                StatisticsLayout(stats, todayCount, weeklyCount, monthlyCount)
            }
        }
    }

    @Composable
    private fun StatisticsLayout(stats: GlobalStats, today: Long, weekly: Long, monthly: Long) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp)
                .appWidgetBackground()
                .clickable(actionRunCallback<OpenAppAction>()),
        ) {
            Text(
                text = "STATISTICS",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(GlanceModifier.height(12.dp))
            
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                StatBox("Today", today.toString(), GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(8.dp))
                StatBox("Weekly", formatCount(weekly), GlanceModifier.defaultWeight())
            }
            
            Spacer(GlanceModifier.height(8.dp))
            
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                StatBox("Monthly", formatCount(monthly), GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(8.dp))
                StatBox("Lifetime", formatCount(stats.totalCount), GlanceModifier.defaultWeight())
            }
            
            Spacer(GlanceModifier.height(16.dp))
            
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Box(modifier = GlanceModifier.defaultWeight()) {
                    StatDetail("Active Sessions", stats.activeSessions.toString())
                }
                Box(modifier = GlanceModifier.defaultWeight()) {
                    StatDetail("Goals Done", stats.totalGoalsCompleted.toString())
                }
            }
            
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Box(modifier = GlanceModifier.defaultWeight()) {
                    StatDetail("Streak", "${stats.currentStreak} days")
                }
                Box(modifier = GlanceModifier.defaultWeight()) {
                    StatDetail("Best Streak", "${stats.longestStreak} days")
                }
            }
        }
    }

    @Composable
    private fun StatBox(label: String, value: String, modifier: GlanceModifier) {
        Column(
            modifier = modifier
                .background(GlanceTheme.colors.primaryContainer)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = label,
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 10.sp)
            )
        }
    }

    @Composable
    private fun StatDetail(label: String, value: String) {
        Column(modifier = GlanceModifier.padding(vertical = 4.dp)) {
            Text(
                text = label,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
            )
            Text(
                text = value,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
