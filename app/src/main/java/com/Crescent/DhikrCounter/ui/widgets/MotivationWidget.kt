package com.Crescent.DhikrCounter.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.data.GlobalStats
import com.Crescent.DhikrCounter.data.QuotesProvider
import java.util.Calendar

class MotivationWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_SQUARE = DpSize(100.dp, 100.dp)
        private val HORIZONTAL_RECTANGLE = DpSize(200.dp, 100.dp)
        private val BIG_SQUARE = DpSize(200.dp, 200.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL_SQUARE, HORIZONTAL_RECTANGLE, BIG_SQUARE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DhikrApplication
        val stats = app.historyRepository.getGlobalStats()
        val daily = app.historyRepository.getDailyActivity(null, 1)
        val todayCount = daily.firstOrNull()?.second ?: 0L
        
        val prefs = app.settingsManager.prefs
        var quoteSeed = prefs.getLong("widget_quote_seed", -1L)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR).toLong()
        
        if (quoteSeed == -1L || prefs.getLong("widget_quote_day", -1L) != today) {
            quoteSeed = System.currentTimeMillis()
            prefs.edit()
                .putLong("widget_quote_seed", quoteSeed)
                .putLong("widget_quote_day", today)
                .apply()
        }
        
        val quote = QuotesProvider.getRandomQuote(quoteSeed)
        
        provideContent {
            GlanceTheme {
                MotivationLayout(context, stats, todayCount, quote)
            }
        }
    }

    @Composable
    private fun MotivationLayout(context: Context, stats: GlobalStats, todayCount: Long, quote: String) {
        val size = LocalSize.current
        val padding = getWidgetPadding(context)
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .applyWidgetStyle(context),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quote Section - Tap to change
            Box(
                modifier = GlanceModifier
                    .clickable(actionRunCallback<RefreshQuoteAction>())
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                val quoteFontSize = when {
                    quote.length > 100 -> 11f
                    quote.length > 60 -> 13f
                    else -> 15f
                }
                Text(
                    text = quote,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = getWidgetFontSize(context, quoteFontSize),
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 5
                )
            }

            if (size.height >= HORIZONTAL_RECTANGLE.height) {
                Spacer(GlanceModifier.height(padding))
                
                if (size.height >= BIG_SQUARE.height) {
                    // Large Layout: Compact Statistics Dashboard
                    StatisticsGrid(context, stats, todayCount)
                } else {
                    // Small/Medium Layout: Essential Info
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoItem(context, "Today", todayCount.toString())
                        Spacer(GlanceModifier.width(padding * 2))
                        InfoItem(context, "Streak", "${stats.currentStreak}d")
                    }
                }
            }
        }
    }

    @Composable
    private fun StatisticsGrid(context: Context, stats: GlobalStats, todayCount: Long) {
        val padding = getWidgetPadding(context)
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                InfoItem(context, "Today", todayCount.toString(), GlanceModifier.defaultWeight())
                InfoItem(context, "Lifetime", stats.totalCount.toString(), GlanceModifier.defaultWeight())
            }
            Spacer(GlanceModifier.height(padding))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                InfoItem(context, "Streak", "${stats.currentStreak}d", GlanceModifier.defaultWeight())
                InfoItem(context, "Best", "${stats.longestStreak}d", GlanceModifier.defaultWeight())
            }
            Spacer(GlanceModifier.height(padding))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                InfoItem(context, "Sessions", stats.activeSessions.toString(), GlanceModifier.defaultWeight())
                InfoItem(context, "Goals", stats.totalGoalsCompleted.toString(), GlanceModifier.defaultWeight())
            }
        }
    }

    @Composable
    private fun InfoItem(context: Context, label: String, value: String, modifier: GlanceModifier = GlanceModifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
                    fontSize = getWidgetFontSize(context, 10f)
                )
            )
        }
    }
}

class RefreshQuoteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as DhikrApplication
        app.settingsManager.prefs.edit()
            .putLong("widget_quote_seed", System.currentTimeMillis())
            .apply()
        MotivationWidget().update(context, glanceId)
    }
}

class MotivationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MotivationWidget()
}
