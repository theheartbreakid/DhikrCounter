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
import com.Crescent.DhikrCounter.data.SessionEntity
import com.Crescent.DhikrCounter.data.GlobalStats

class DashboardWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DhikrApplication
        val activeId = app.settingsManager.activeSessionId
        val session = if (activeId != -1L) app.sessionRepository.getSession(activeId) else null
        val stats = app.historyRepository.getGlobalStats()
        val dailyActivity = app.historyRepository.getDailyActivity(null, 1)
        val todayCount = dailyActivity.firstOrNull()?.second ?: 0L
        
        provideContent {
            GlanceTheme {
                DashboardLayout(session, stats, todayCount)
            }
        }
    }

    @Composable
    private fun DashboardLayout(session: SessionEntity?, stats: GlobalStats, todayCount: Long) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
                .appWidgetBackground()
                .clickable(actionRunCallback<OpenAppAction>()),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = session?.name ?: "No Active Session",
                        style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        text = "Current Count: ${session?.count ?: 0}",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                    )
                }
                
                if (session != null && session.goalCount > 0) {
                    val progress = (session.count.toFloat() / session.goalCount.toFloat()).coerceIn(0f, 1f)
                    Text(
                        text = "${(progress * 100).toInt()}% Goal",
                        style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
            
            Spacer(GlanceModifier.height(12.dp))
            
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                StatItem("Today", todayCount.toString())
                Spacer(GlanceModifier.width(8.dp))
                StatItem("Streak", "${stats.currentStreak}d")
                Spacer(GlanceModifier.width(8.dp))
                StatItem("Lifetime", formatCount(stats.totalCount))
            }
        }
    }

    @Composable
    private fun RowScope.StatItem(label: String, value: String) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .background(GlanceTheme.colors.surfaceVariant)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = label,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
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

class DashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DashboardWidget()
}
