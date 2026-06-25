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
                DashboardLayout(context, session, stats, todayCount)
            }
        }
    }

    @Composable
    private fun DashboardLayout(context: Context, session: SessionEntity?, stats: GlobalStats, todayCount: Long) {
        val padding = getWidgetPadding(context)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .applyWidgetStyle(context)
                .clickable(actionRunCallback<OpenAppAction>()),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = session?.name ?: "No Active Session",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary, 
                            fontSize = getWidgetFontSize(context, 16f), 
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "Current: ${session?.count ?: 0}",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant, 
                            fontSize = getWidgetFontSize(context, 12f)
                        )
                    )
                }
                
                if (session != null && session.goalCount > 0) {
                    val progress = (session.count.toFloat() / session.goalCount.toFloat()).coerceIn(0f, 1f)
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary, 
                            fontSize = getWidgetFontSize(context, 12f), 
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            Spacer(GlanceModifier.height(padding))
            
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                StatItem(context, "Today", todayCount.toString())
                Spacer(GlanceModifier.width(padding))
                StatItem(context, "Streak", "${stats.currentStreak}d")
                Spacer(GlanceModifier.width(padding))
                StatItem(context, "Total", formatCount(stats.totalCount))
            }
        }
    }

    @Composable
    private fun RowScope.StatItem(context: Context, label: String, value: String) {
        val app = context.applicationContext as DhikrApplication
        val radius = app.settingsManager.getWidgetCornerRadius() / 2f
        
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(radius.dp)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface, 
                    fontSize = getWidgetFontSize(context, 13f), 
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = label,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant, 
                    fontSize = getWidgetFontSize(context, 9f)
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

class DashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DashboardWidget()
}
