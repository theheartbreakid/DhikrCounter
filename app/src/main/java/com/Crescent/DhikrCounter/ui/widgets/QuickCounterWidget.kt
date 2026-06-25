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
import androidx.glance.layout.*
import androidx.glance.text.*
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.MainActivity
import com.Crescent.DhikrCounter.data.SessionEntity

class QuickCounterWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DhikrApplication
        val activeId = app.settingsManager.activeSessionId
        val session = if (activeId != -1L) app.sessionRepository.getSession(activeId) else null
        
        provideContent {
            GlanceTheme {
                QuickCounterLayout(context, session)
            }
        }
    }

    @Composable
    private fun QuickCounterLayout(context: Context, session: SessionEntity?) {
        val padding = getWidgetPadding(context)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .applyWidgetStyle(context)
                .clickable(actionRunCallback<OpenAppAction>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (session == null) {
                Text(
                    text = "No Active Session",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface, 
                        fontSize = getWidgetFontSize(context, 14f),
                        fontWeight = FontWeight.Medium
                    )
                )
            } else {
                Text(
                    text = session.name,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = getWidgetFontSize(context, 12f),
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                
                Spacer(GlanceModifier.height(padding / 2))
                
                Text(
                    text = session.count.toString(),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = getWidgetFontSize(context, 32f),
                        fontWeight = FontWeight.Bold
                    )
                )

                if (session.goalCount > 0) {
                    val progress = (session.count.toFloat() / session.goalCount.toFloat()).coerceIn(0f, 1f)
                    val percent = (progress * 100).toInt()
                    Text(
                        text = "Goal: $percent%",
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary, 
                            fontSize = getWidgetFontSize(context, 10f)
                        )
                    )
                }

                Spacer(GlanceModifier.defaultWeight())

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WidgetButton(
                        context = context,
                        text = "-",
                        onClick = actionRunCallback<DecrementAction>(
                            actionParametersOf(SessionIdKey to session.id)
                        )
                    )
                    
                    Spacer(GlanceModifier.width(padding * 2))
                    
                    WidgetButton(
                        context = context,
                        text = "+",
                        onClick = actionRunCallback<IncrementAction>(
                            actionParametersOf(SessionIdKey to session.id)
                        ),
                        isPrimary = true
                    )
                }
            }
        }
    }

    @Composable
    private fun WidgetButton(
        context: Context,
        text: String,
        onClick: androidx.glance.action.Action,
        isPrimary: Boolean = false
    ) {
        val app = context.applicationContext as DhikrApplication
        val radius = app.settingsManager.getWidgetCornerRadius() / 2f
        val size = if (app.settingsManager.isWidgetCompactMode()) 40.dp else 48.dp
        
        Box(
            modifier = GlanceModifier
                .size(size)
                .background(if (isPrimary) GlanceTheme.colors.primary else GlanceTheme.colors.secondaryContainer)
                .cornerRadius(radius.dp)
                .clickable(onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TextStyle(
                    color = if (isPrimary) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSecondaryContainer,
                    fontSize = getWidgetFontSize(context, 20f),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class QuickCounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickCounterWidget()
}
