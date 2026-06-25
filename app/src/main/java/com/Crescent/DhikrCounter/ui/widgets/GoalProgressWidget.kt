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

class GoalProgressWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DhikrApplication
        val activeId = app.settingsManager.activeSessionId
        val session = if (activeId != -1L) app.sessionRepository.getSession(activeId) else null
        
        provideContent {
            GlanceTheme {
                GoalProgressLayout(session)
            }
        }
    }

    @Composable
    private fun GoalProgressLayout(session: SessionEntity?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
                .appWidgetBackground()
                .clickable(actionRunCallback<OpenAppAction>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (session == null || session.goalCount == 0L) {
                Text("No Goal", style = TextStyle(color = GlanceTheme.colors.onSurface))
            } else {
                val progress = (session.count.toFloat() / session.goalCount.toFloat()).coerceIn(0f, 1f)
                val remaining = maxOf(0, session.goalCount - session.count)
                
                Text(
                    text = session.name,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                    maxLines = 1
                )
                
                Spacer(GlanceModifier.height(8.dp))
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                )
                
                LinearProgressIndicator(
                    progress = progress,
                    modifier = GlanceModifier.fillMaxWidth().height(8.dp),
                    color = GlanceTheme.colors.primary,
                    backgroundColor = GlanceTheme.colors.surfaceVariant
                )
                
                Spacer(GlanceModifier.height(8.dp))
                
                Text(
                    text = "$remaining left",
                    style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

class GoalProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoalProgressWidget()
}
