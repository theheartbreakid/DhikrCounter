package com.Crescent.DhikrCounter.ui.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.Crescent.DhikrCounter.DhikrApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IncrementAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as DhikrApplication
        val sessionId = parameters[SessionIdKey] ?: app.settingsManager.activeSessionId
        
        if (sessionId != -1L) {
            val session = app.sessionRepository.getSession(sessionId)
            if (session != null) {
                app.sessionRepository.incrementCount(sessionId, session.incrementValue)
                app.historyRepository.logEvent(sessionId, session.name, "INCREMENT", session.incrementValue)
                
                // Update all widgets
                updateAllWidgets(context)
            }
        }
    }
}

class DecrementAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as DhikrApplication
        val sessionId = parameters[SessionIdKey] ?: app.settingsManager.activeSessionId
        val allowNegative = app.settingsManager.isNegativeCountAllowed

        if (sessionId != -1L) {
            val session = app.sessionRepository.getSession(sessionId)
            if (session != null) {
                app.sessionRepository.decrementCount(sessionId, session.decrementValue, allowNegative)
                app.historyRepository.logEvent(sessionId, session.name, "DECREMENT", -session.decrementValue)
                
                // Update all widgets
                updateAllWidgets(context)
            }
        }
    }
}

class OpenAppAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = android.content.Intent(context, com.Crescent.DhikrCounter.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

val SessionIdKey = ActionParameters.Key<Long>("sessionId")

suspend fun updateAllWidgets(context: Context) {
    CounterWidget().updateAll(context)
    CompactCounterWidget().updateAll(context)
    DashboardWidget().updateAll(context)
    StatisticsWidget().updateAll(context)
    MultiSessionWidget().updateAll(context)
    GoalProgressWidget().updateAll(context)
    MotivationWidget().updateAll(context)
    AmoledWidget().updateAll(context)
}
