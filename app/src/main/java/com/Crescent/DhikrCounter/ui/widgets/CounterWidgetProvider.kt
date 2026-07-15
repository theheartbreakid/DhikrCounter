package com.Crescent.DhikrCounter.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.Crescent.DhikrCounter.DhikrApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CounterWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            updateCounterWidget(context, manager, id)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        updateCounterWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_COUNTER_CONTROL) {
            val app = context.applicationContext as DhikrApplication
            val settings = app.settingsManager
            val type = intent.getStringExtra(EXTRA_CONTROL_TYPE) ?: return
            val sessionId = settings.activeSessionId

            scope.launch {
                when (type) {
                    "plus" -> app.sessionRepository.incrementCount(sessionId, 1)
                    "minus" -> app.sessionRepository.decrementCount(sessionId, 1, settings.isNegativeCountAllowed)
                    "reset" -> app.sessionRepository.resetCount(sessionId)
                }
                launch(Dispatchers.Main) {
                    updateAllWidgets(context)
                }
            }
        }
    }
}
