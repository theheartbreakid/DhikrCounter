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

class CompactCounterWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DhikrApplication
        val activeId = app.settingsManager.activeSessionId
        val session = if (activeId != -1L) app.sessionRepository.getSession(activeId) else null
        
        provideContent {
            GlanceTheme {
                CompactLayout(session)
            }
        }
    }

    @Composable
    private fun CompactLayout(session: SessionEntity?) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
                .appWidgetBackground()
                .clickable(actionRunCallback<OpenAppAction>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (session == null) {
                Text("DhikrCounter++", style = TextStyle(color = GlanceTheme.colors.onSurface))
            } else {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = session.name,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }
                
                Text(
                    text = session.count.toString(),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class CompactCounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactCounterWidget()
}
