package com.Crescent.DhikrCounter.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

class AmoledWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DhikrApplication
        val activeId = app.settingsManager.activeSessionId
        val session = if (activeId != -1L) app.sessionRepository.getSession(activeId) else null
        
        provideContent {
            AmoledLayout(context, session)
        }
    }

    @Composable
    private fun AmoledLayout(context: Context, session: SessionEntity?) {
        val black = ColorProvider(Color(0xFF000000))
        val white = ColorProvider(Color(0xFFFFFFFF))
        val gray = ColorProvider(Color(0xFF888888))
        val darkGray = ColorProvider(Color(0xFF222222))
        
        val app = context.applicationContext as DhikrApplication
        val padding = getWidgetPadding(context)
        val radius = app.settingsManager.getWidgetCornerRadius()
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(black)
                .cornerRadius(radius.dp)
                .padding(padding)
                .appWidgetBackground()
                .clickable(actionRunCallback<OpenAppAction>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (session == null) {
                Text(
                    text = "DhikrCounter++", 
                    style = TextStyle(
                        color = white, 
                        fontSize = getWidgetFontSize(context, 16f)
                    )
                )
            } else {
                Text(
                    text = session.name,
                    style = TextStyle(
                        color = gray, 
                        fontSize = getWidgetFontSize(context, 12f)
                    ),
                    maxLines = 1
                )
                
                Spacer(GlanceModifier.height(padding))
                
                Text(
                    text = session.count.toString(),
                    style = TextStyle(
                        color = white, 
                        fontSize = getWidgetFontSize(context, 48f), 
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(GlanceModifier.defaultWeight())
                
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AmoledButton(
                        context,
                        "-", 
                        actionRunCallback<DecrementAction>(actionParametersOf(SessionIdKey to session.id)), 
                        darkGray, 
                        white
                    )
                    Spacer(GlanceModifier.width(padding * 2))
                    AmoledButton(
                        context,
                        "+", 
                        actionRunCallback<IncrementAction>(actionParametersOf(SessionIdKey to session.id)), 
                        white, 
                        black
                    )
                }
            }
        }
    }

    @Composable
    private fun AmoledButton(context: Context, text: String, onClick: androidx.glance.action.Action, bgColor: ColorProvider, textColor: ColorProvider) {
        val app = context.applicationContext as DhikrApplication
        val radius = app.settingsManager.getWidgetCornerRadius() / 2f
        val size = if (app.settingsManager.isWidgetCompactMode()) 40.dp else 48.dp
        
        Box(
            modifier = GlanceModifier
                .size(size)
                .background(bgColor)
                .cornerRadius(radius.dp)
                .clickable(onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TextStyle(
                    color = textColor, 
                    fontSize = getWidgetFontSize(context, 24f), 
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class AmoledWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AmoledWidget()
}
