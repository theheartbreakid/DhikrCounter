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

class MotivationWidget : GlanceAppWidget() {

    private val quotes = listOf(
        "Verily, in the remembrance of Allah do hearts find rest.",
        "The best dhikr is 'La ilaha illAllah'.",
        "Keep your tongue moist with the remembrance of Allah.",
        "Allah is with those who remember Him.",
        "Success is found in consistency.",
        "Every count is a step closer to peace.",
        "Start your day with gratitude and Dhikr."
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DhikrApplication
        val stats = app.historyRepository.getGlobalStats()
        val daily = app.historyRepository.getDailyActivity(null, 1)
        val todayCount = daily.firstOrNull()?.second ?: 0L
        
        val quote = quotes[System.currentTimeMillis().div(86400000).toInt() % quotes.size]
        
        provideContent {
            GlanceTheme {
                MotivationLayout(context, stats, todayCount, quote)
            }
        }
    }

    @Composable
    private fun MotivationLayout(context: Context, stats: GlobalStats, todayCount: Long, quote: String) {
        val padding = getWidgetPadding(context)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .applyWidgetStyle(context)
                .clickable(actionRunCallback<OpenAppAction>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = quote,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = getWidgetFontSize(context, 14f),
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier.padding(horizontal = padding)
            )
            
            Spacer(GlanceModifier.height(padding * 2))
            
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                InfoItem(context, "Today", todayCount.toString())
                Spacer(GlanceModifier.width(padding * 2))
                InfoItem(context, "Streak", "${stats.currentStreak}d")
            }
        }
    }

    @Composable
    private fun InfoItem(context: Context, label: String, value: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = TextStyle(
                    color = GlanceTheme.colors.primary, 
                    fontSize = getWidgetFontSize(context, 16f), 
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
}

class MotivationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MotivationWidget()
}
