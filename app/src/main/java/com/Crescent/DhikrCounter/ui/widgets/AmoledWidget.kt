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
            AmoledLayout(session)
        }
    }

    @Composable
    private fun AmoledLayout(session: SessionEntity?) {
        val black = ColorProvider(Color(0xFF000000))
        val white = ColorProvider(Color(0xFFFFFFFF))
        val gray = ColorProvider(Color(0xFF888888))
        val darkGray = ColorProvider(Color(0xFF222222))
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(black)
                .padding(12.dp)
                .appWidgetBackground()
                .clickable(actionRunCallback<OpenAppAction>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (session == null) {
                Text("DhikrCounter++", style = TextStyle(color = white, fontSize = 16.sp))
            } else {
                Text(
                    text = session.name,
                    style = TextStyle(color = gray, fontSize = 14.sp),
                    maxLines = 1
                )
                
                Spacer(GlanceModifier.height(8.dp))
                
                Text(
                    text = session.count.toString(),
                    style = TextStyle(color = white, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                )
                
                Spacer(GlanceModifier.defaultWeight())
                
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AmoledButton("-", actionRunCallback<DecrementAction>(actionParametersOf(SessionIdKey to session.id)), darkGray, white)
                    Spacer(GlanceModifier.width(24.dp))
                    AmoledButton("+", actionRunCallback<IncrementAction>(actionParametersOf(SessionIdKey to session.id)), white, black)
                }
            }
        }
    }

    @Composable
    private fun AmoledButton(text: String, onClick: androidx.glance.action.Action, bgColor: ColorProvider, textColor: ColorProvider) {
        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .background(bgColor)
                .clickable(onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TextStyle(color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

class AmoledWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AmoledWidget()
}
