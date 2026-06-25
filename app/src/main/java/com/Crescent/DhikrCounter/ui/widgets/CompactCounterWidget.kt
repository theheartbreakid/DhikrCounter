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
                CompactLayout(context, session)
            }
        }
    }

    @Composable
    private fun CompactLayout(context: Context, session: SessionEntity?) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .applyWidgetStyle(context)
                .clickable(actionRunCallback<OpenAppAction>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (session == null) {
                Text(
                    text = "DhikrCounter++", 
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = getWidgetFontSize(context, 14f)
                    )
                )
            } else {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = session.name,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = getWidgetFontSize(context, 12f),
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }
                
                Text(
                    text = session.count.toString(),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = getWidgetFontSize(context, 24f),
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
