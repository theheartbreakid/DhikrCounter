package com.Crescent.DhikrCounter.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.data.SessionEntity

class CounterWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_SQUARE = DpSize(100.dp, 100.dp)
        private val HORIZONTAL_RECTANGLE = DpSize(220.dp, 120.dp)
        private val BIG_RECTANGLE = DpSize(220.dp, 220.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL_SQUARE, HORIZONTAL_RECTANGLE, BIG_RECTANGLE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DhikrApplication
        val activeId = app.settingsManager.activeSessionId
        val sessions = app.sessionRepository.getAllSessions()
        val session = if (activeId != -1L) sessions.find { it.id == activeId } ?: sessions.firstOrNull() else sessions.firstOrNull()

        provideContent {
            GlanceTheme {
                CounterLayout(context, session, sessions)
            }
        }
    }

    @Composable
    private fun CounterLayout(context: Context, session: SessionEntity?, allSessions: List<SessionEntity>) {
        val size = LocalSize.current
        val padding = getWidgetPadding(context)
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .applyWidgetStyle(context),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (session == null) {
                Text("No Sessions", style = TextStyle(color = GlanceTheme.colors.onSurface))
            } else {
                // Top Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.name,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = getWidgetFontSize(context, 12f),
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight().clickable(actionRunCallback<OpenAppAction>())
                    )
                    
                    if (session.goalCount > 0) {
                        val percent = ((session.count.toFloat() / session.goalCount) * 100).toInt()
                        Chip(context, "$percent%", GlanceTheme.colors.secondaryContainer, GlanceTheme.colors.onSecondaryContainer)
                    }

                    if (size.width >= HORIZONTAL_RECTANGLE.width) {
                        Spacer(GlanceModifier.width(4.dp))
                        val app = context.applicationContext as DhikrApplication
                        val floatingEnabled = app.settingsManager.isFloatingBubbleEnabled()
                        Box(
                            modifier = GlanceModifier
                                .size(24.dp)
                                .clickable(actionRunCallback<ToggleFloatingAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (floatingEnabled) "⦿" else "⦾",
                                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 18.sp)
                            )
                        }
                    }
                }

                if (size.height >= BIG_RECTANGLE.height) {
                    Spacer(GlanceModifier.height(padding))
                    // Session Selector
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        allSessions.take(3).forEach { s ->
                            val isSelected = s.id == session.id
                            Box(
                                modifier = GlanceModifier
                                    .padding(horizontal = 4.dp)
                                    .clickable(actionRunCallback<SwitchSessionAction>(actionParametersOf(SessionIdKey to s.id))),
                                contentAlignment = Alignment.Center
                            ) {
                                Chip(
                                    context, 
                                    s.name, 
                                    if (isSelected) GlanceTheme.colors.primary else GlanceTheme.colors.surfaceVariant,
                                    if (isSelected) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(GlanceModifier.defaultWeight())

                // Large Count
                Text(
                    text = session.count.toString(),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = getWidgetFontSize(context, if (size.height < HORIZONTAL_RECTANGLE.height) 36f else 48f),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.clickable(actionRunCallback<OpenAppAction>())
                )

                if (size.height >= HORIZONTAL_RECTANGLE.height && session.goalCount > 0) {
                    // Progress Section
                    Spacer(GlanceModifier.height(padding))
                    ProgressBar(context, session.count, session.goalCount)
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp)) {
                        Text(
                            "Rem: ${session.goalCount - session.count}",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = getWidgetFontSize(context, 9f))
                        )
                        Spacer(GlanceModifier.defaultWeight())
                        Text(
                            "Goal: ${session.goalCount}",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = getWidgetFontSize(context, 9f))
                        )
                    }
                }

                Spacer(GlanceModifier.defaultWeight())

                // Bottom Buttons
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WidgetButton(context, "-", actionRunCallback<DecrementAction>(actionParametersOf(SessionIdKey to session.id)))
                    Spacer(GlanceModifier.width(padding * 2))
                    WidgetButton(context, "↺", actionRunCallback<ResetAction>(actionParametersOf(SessionIdKey to session.id)))
                    Spacer(GlanceModifier.width(padding * 2))
                    WidgetButton(context, "+", actionRunCallback<IncrementAction>(actionParametersOf(SessionIdKey to session.id)), isPrimary = true)
                }
            }
        }
    }

    @Composable
    private fun Chip(context: Context, text: String, bgColor: ColorProvider, textColor: ColorProvider) {
        val app = context.applicationContext as DhikrApplication
        val radius = app.settingsManager.getWidgetCornerRadius() / 3f
        Box(
            modifier = GlanceModifier
                .background(bgColor)
                .cornerRadius(radius.dp)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = TextStyle(color = textColor, fontSize = getWidgetFontSize(context, 10f), fontWeight = FontWeight.Medium))
        }
    }

    @Composable
    private fun ProgressBar(context: Context, count: Long, goal: Long) {
        val progress = (count.toFloat() / goal).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier.fillMaxWidth().height(8.dp),
            color = GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.surfaceVariant
        )
    }

    @Composable
    private fun WidgetButton(context: Context, text: String, onClick: androidx.glance.action.Action, isPrimary: Boolean = false) {
        val app = context.applicationContext as DhikrApplication
        val radius = app.settingsManager.getWidgetCornerRadius() / 2f
        val scale = app.settingsManager.getWidgetScale()
        val size = (48 * scale).dp
        
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

class CounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CounterWidget()
}
