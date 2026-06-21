package com.Crescent.DhikrCounter.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.Crescent.DhikrCounter.DhikrApplication

class CounterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as DhikrApplication
        val activeSessionId = app.settingsManager.activeSessionId
        val session = if (activeSessionId != -1L) app.sessionRepository.getSession(activeSessionId) else null

        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (session != null) {
                    Text(text = session.name, style = TextStyle(fontWeight = FontWeight.Bold))
                    Text(text = session.count.toString(), style = TextStyle(fontWeight = FontWeight.Bold))
                } else {
                    Text("No Active Session")
                }
            }
        }
    }
}
