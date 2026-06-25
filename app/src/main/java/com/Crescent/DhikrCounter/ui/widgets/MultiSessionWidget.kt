package com.Crescent.DhikrCounter.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.*
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.data.SessionEntity

class MultiSessionWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DhikrApplication
        val sessions = app.sessionRepository.getAllSessions()
        
        provideContent {
            GlanceTheme {
                MultiSessionLayout(sessions)
            }
        }
    }

    @Composable
    private fun MultiSessionLayout(sessions: List<SessionEntity>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(8.dp)
                .appWidgetBackground()
        ) {
            Text(
                text = "Sessions",
                modifier = GlanceModifier.padding(8.dp),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
            
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(sessions) { session ->
                    SessionItem(session)
                }
            }
        }
    }

    @Composable
    private fun SessionItem(session: SessionEntity) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(GlanceTheme.colors.surfaceVariant)
                .clickable(actionRunCallback<SelectSessionAction>(
                    actionParametersOf(SessionIdKey to session.id)
                )),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = session.name,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                if (session.goalCount > 0) {
                    val progress = (session.count.toFloat() / session.goalCount.toFloat()).coerceIn(0f, 1f)
                    Text(
                        text = "Goal: ${(progress * 100).toInt()}%",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
                    )
                }
            }
            
            Text(
                text = session.count.toString(),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

class SelectSessionAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as DhikrApplication
        val sessionId = parameters[SessionIdKey] ?: return
        app.settingsManager.activeSessionId = sessionId
        
        // Open app
        val intent = android.content.Intent(context, com.Crescent.DhikrCounter.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        
        updateAllWidgets(context)
    }
}

class MultiSessionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MultiSessionWidget()
}
