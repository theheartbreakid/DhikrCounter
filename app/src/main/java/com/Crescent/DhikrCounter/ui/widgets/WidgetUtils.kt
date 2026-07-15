package com.Crescent.DhikrCounter.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.widget.RemoteViews
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.MainActivity
import com.Crescent.DhikrCounter.R
import com.Crescent.DhikrCounter.data.SessionEntity
import com.Crescent.DhikrCounter.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.random.Random

const val ACTION_COUNTER_CONTROL = "com.Crescent.DhikrCounter.ACTION_COUNTER_CONTROL"
const val ACTION_MOTIVATION_CYCLE = "com.Crescent.DhikrCounter.ACTION_MOTIVATION_CYCLE"
const val EXTRA_CONTROL_TYPE = "extra_control_type" // plus, minus, reset

private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun updateAllWidgets(context: Context) {
    widgetScope.launch {
        val manager = AppWidgetManager.getInstance(context)
        
        val aIds = manager.getAppWidgetIds(ComponentName(context, CounterWidgetProvider::class.java))
        for (id in aIds) updateCounterWidget(context, manager, id)

        val bIds = manager.getAppWidgetIds(ComponentName(context, MotivationWidgetProvider::class.java))
        for (id in bIds) updateMotivationWidget(context, manager, id)
    }
}

fun updateCounterWidget(context: Context, manager: AppWidgetManager, id: Int) {
    val app = context.applicationContext as DhikrApplication
    val settings = app.settingsManager
    val session = app.sessionRepository.getSessionSync(settings.activeSessionId)

    val remoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val viewMapping = mapOf(
            SizeF(60f, 60f) to buildCounterViews(context, R.layout.widget_a_1x1, session, settings, id),
            SizeF(110f, 60f) to buildCounterViews(context, R.layout.widget_a_2x1, session, settings, id),
            SizeF(110f, 110f) to buildCounterViews(context, R.layout.widget_a_2x2, session, settings, id),
            SizeF(180f, 60f) to buildCounterViews(context, R.layout.widget_a_3x1, session, settings, id),
            SizeF(180f, 120f) to buildCounterViews(context, R.layout.widget_a_4x2, session, settings, id),
            SizeF(240f, 180f) to buildCounterViews(context, R.layout.widget_a_4x3, session, settings, id),
            SizeF(240f, 240f) to buildCounterViews(context, R.layout.widget_a_5x5, session, settings, id)
        )
        RemoteViews(viewMapping)
    } else {
        val options = manager.getAppWidgetOptions(id)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val layoutRes = getCounterLayoutForSize(width, height)
        buildCounterViews(context, layoutRes, session, settings, id)
    }
    manager.updateAppWidget(id, remoteViews)
}

private fun getCounterLayoutForSize(width: Int, height: Int): Int {
    return when {
        width >= 240 && height >= 240 -> R.layout.widget_a_5x5
        width >= 240 && height >= 180 -> R.layout.widget_a_4x3
        width >= 180 && height >= 120 -> R.layout.widget_a_4x2
        width >= 180 && height >= 60 -> R.layout.widget_a_3x1
        width >= 110 && height >= 110 -> R.layout.widget_a_2x2
        width >= 110 && height >= 60 -> R.layout.widget_a_2x1
        else -> R.layout.widget_a_1x1
    }
}

private fun buildCounterViews(context: Context, layout: Int, session: SessionEntity?, settings: SettingsManager, id: Int): RemoteViews {
    val views = RemoteViews(context.packageName, layout)
    val count = session?.count ?: 0L
    val name = session?.name ?: "Dhikr"

    views.setTextViewText(R.id.widget_count, count.toString())
    views.setTextViewText(R.id.widget_label, name)

    // Progress
    val goal = 100L
    val progress = if (goal > 0) (count * 100 / goal).toInt().coerceIn(0, 100) else 0
    views.setProgressBar(R.id.widget_progress, 100, progress, false)

    // Stats for large layouts
    if (layout == R.layout.widget_a_4x3 || layout == R.layout.widget_a_5x5) {
        views.setTextViewText(R.id.widget_stats, "Goal: $goal | Progress: $progress%")
    }

    applyFontScaling(views, layout, settings)

    // Clicks
    views.setOnClickPendingIntent(R.id.btn_plus, createControlPendingIntent(context, id, "plus"))
    views.setOnClickPendingIntent(R.id.btn_minus, createControlPendingIntent(context, id, "minus"))
    views.setOnClickPendingIntent(R.id.btn_reset, createControlPendingIntent(context, id, "reset"))

    val openAppIntent = Intent(context, MainActivity::class.java)
    val pIntent = PendingIntent.getActivity(context, id, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.widget_root, pIntent)

    return views
}

fun updateMotivationWidget(context: Context, manager: AppWidgetManager, id: Int) {
    val app = context.applicationContext as DhikrApplication
    val settings = app.settingsManager
    
    val poolPrefs = context.getSharedPreferences("widget_b_state", Context.MODE_PRIVATE)
    val lastIndex = poolPrefs.getInt("last_index_$id", -1)
    
    // 20% chance to show a live stat instead of a quote
    val showStat = Random.nextInt(100) < 20
    val content: String
    val footer: String

    if (showStat) {
        val session = app.sessionRepository.getSessionSync(settings.activeSessionId)
        val todayCount = session?.count ?: 0L
        content = "You have remembered Allah $todayCount times in your current session."
        footer = "Live Session Stat"
    } else {
        val pool = context.resources.getStringArray(R.array.motivation_pool)
        var nextIndex = Random.nextInt(pool.size)
        while (nextIndex == lastIndex && pool.size > 1) {
            nextIndex = Random.nextInt(pool.size)
        }
        poolPrefs.edit().putInt("last_index_$id", nextIndex).apply()
        content = pool[nextIndex]
        footer = "Islamic Motivation"
    }

    val remoteViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val viewMapping = mapOf(
            SizeF(60f, 60f) to buildMotivationViews(context, R.layout.widget_b_small, content, footer, settings, id),
            SizeF(110f, 110f) to buildMotivationViews(context, R.layout.widget_b_medium, content, footer, settings, id),
            SizeF(240f, 180f) to buildMotivationViews(context, R.layout.widget_b_large, content, footer, settings, id)
        )
        RemoteViews(viewMapping)
    } else {
        val options = manager.getAppWidgetOptions(id)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val layoutRes = when {
            width >= 240 && height >= 180 -> R.layout.widget_b_large
            width >= 110 && height >= 110 -> R.layout.widget_b_medium
            else -> R.layout.widget_b_small
        }
        buildMotivationViews(context, layoutRes, content, footer, settings, id)
    }
    manager.updateAppWidget(id, remoteViews)
}

private fun buildMotivationViews(context: Context, layout: Int, content: String, footer: String, settings: SettingsManager, id: Int): RemoteViews {
    val views = RemoteViews(context.packageName, layout)
    views.setTextViewText(R.id.widget_content, content)
    
    if (layout == R.layout.widget_b_medium || layout == R.layout.widget_b_large) {
        views.setTextViewText(R.id.widget_footer, footer)
    }

    applyFontScaling(views, layout, settings)

    val cycleIntent = Intent(context, MotivationWidgetProvider::class.java).apply {
        action = ACTION_MOTIVATION_CYCLE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    }
    val pIntent = PendingIntent.getBroadcast(context, id, cycleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.widget_root, pIntent)

    return views
}

private fun applyFontScaling(views: RemoteViews, layout: Int, settings: SettingsManager) {
    val autoFit = settings.isWidgetAutoFitEnabled()
    val manualScale = settings.getWidgetFontScale()

    fun getScaledSize(base: Float): Float {
        return if (autoFit) base else base * manualScale
    }

    when (layout) {
        R.layout.widget_a_1x1 -> {
            views.setTextViewTextSize(R.id.widget_count, TypedValue.COMPLEX_UNIT_SP, getScaledSize(20f))
        }
        R.layout.widget_a_2x1 -> {
            views.setTextViewTextSize(R.id.widget_count, TypedValue.COMPLEX_UNIT_SP, getScaledSize(24f))
        }
        R.layout.widget_a_2x2 -> {
            views.setTextViewTextSize(R.id.widget_label, TypedValue.COMPLEX_UNIT_SP, getScaledSize(12f))
            views.setTextViewTextSize(R.id.widget_count, TypedValue.COMPLEX_UNIT_SP, getScaledSize(36f))
        }
        R.layout.widget_a_3x1 -> {
            views.setTextViewTextSize(R.id.widget_label, TypedValue.COMPLEX_UNIT_SP, getScaledSize(12f))
            views.setTextViewTextSize(R.id.widget_count, TypedValue.COMPLEX_UNIT_SP, getScaledSize(32f))
        }
        R.layout.widget_a_4x2 -> {
            views.setTextViewTextSize(R.id.widget_label, TypedValue.COMPLEX_UNIT_SP, getScaledSize(14f))
            views.setTextViewTextSize(R.id.widget_count, TypedValue.COMPLEX_UNIT_SP, getScaledSize(48f))
        }
        R.layout.widget_a_4x3 -> {
            views.setTextViewTextSize(R.id.widget_label, TypedValue.COMPLEX_UNIT_SP, getScaledSize(16f))
            views.setTextViewTextSize(R.id.widget_count, TypedValue.COMPLEX_UNIT_SP, getScaledSize(64f))
            views.setTextViewTextSize(R.id.widget_stats, TypedValue.COMPLEX_UNIT_SP, getScaledSize(14f))
        }
        R.layout.widget_a_5x5 -> {
            views.setTextViewTextSize(R.id.widget_label, TypedValue.COMPLEX_UNIT_SP, getScaledSize(20f))
            views.setTextViewTextSize(R.id.widget_count, TypedValue.COMPLEX_UNIT_SP, getScaledSize(80f))
            views.setTextViewTextSize(R.id.widget_stats, TypedValue.COMPLEX_UNIT_SP, getScaledSize(18f))
        }
        R.layout.widget_b_small -> {
            views.setTextViewTextSize(R.id.widget_content, TypedValue.COMPLEX_UNIT_SP, getScaledSize(14f))
        }
        R.layout.widget_b_medium -> {
            views.setTextViewTextSize(R.id.widget_content, TypedValue.COMPLEX_UNIT_SP, getScaledSize(16f))
            views.setTextViewTextSize(R.id.widget_footer, TypedValue.COMPLEX_UNIT_SP, getScaledSize(10f))
        }
        R.layout.widget_b_large -> {
            views.setTextViewTextSize(R.id.widget_content, TypedValue.COMPLEX_UNIT_SP, getScaledSize(20f))
            views.setTextViewTextSize(R.id.widget_footer, TypedValue.COMPLEX_UNIT_SP, getScaledSize(14f))
        }
    }
}

private fun createControlPendingIntent(context: Context, widgetId: Int, type: String): PendingIntent {
    val intent = Intent(context, CounterWidgetProvider::class.java).apply {
        action = ACTION_COUNTER_CONTROL
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        putExtra(EXTRA_CONTROL_TYPE, type)
    }
    return PendingIntent.getBroadcast(context, widgetId + type.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
