package com.Crescent.DhikrCounter.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.*
import android.provider.Settings
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.util.TypedValue
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.MainActivity
import com.Crescent.DhikrCounter.R
import com.Crescent.DhikrCounter.data.SessionEntity
import com.Crescent.DhikrCounter.data.SessionRepository
import com.Crescent.DhikrCounter.data.HistoryRepository
import com.Crescent.DhikrCounter.utils.GlassUtils
import com.Crescent.DhikrCounter.utils.SettingsManager
import com.Crescent.DhikrCounter.utils.SoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingCounterService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "floating_counter_channel"
        private const val NOTIFICATION_ID = 1
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: LayoutParams? = null
    private var dismissZoneView: View? = null
    private var dismissZoneParams: LayoutParams? = null

    private var repository: SessionRepository? = null
    private var historyRepository: HistoryRepository? = null
    private var settingsManager: SettingsManager? = null
    private var soundManager: SoundManager? = null
    private var activeSession: SessionEntity? = null
    private var sessionJob: kotlinx.coroutines.Job? = null

    private var isQuickActionsExpanded = false
    private val displaySize = Point()
    private var isViewAttached = false
    private var isDismissAttached = false

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "active_session_id") {
            observeActiveSession()
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        try {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        try {
            val app = application as DhikrApplication
            repository = app.sessionRepository
            historyRepository = app.historyRepository
            settingsManager = app.settingsManager
            soundManager = app.soundManager
            settingsManager?.prefs?.registerOnSharedPreferenceChangeListener(prefListener)
            
            soundManager?.playSound(SoundManager.SoundType.FLOATING_POPUP)

            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager = wm
            updateDisplaySize()

            setupFloatingView()
            setupDismissZone()
            observeActiveSession()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun updateDisplaySize() {
        windowManager?.let { wm ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics = wm.currentWindowMetrics
                displaySize.set(metrics.bounds.width(), metrics.bounds.height())
            } else {
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getSize(displaySize)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Floating Counter Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DhikrCounter++ Floating")
            .setContentText("Floating counter is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun setupFloatingView() {
        val wm = windowManager ?: return
        val sm = settingsManager ?: return
        
        val contextThemeWrapper = ContextThemeWrapper(this, R.style.Theme_DhikrCounter)
        val view = LayoutInflater.from(contextThemeWrapper).inflate(R.layout.layout_floating_widget, null)
        floatingView = view

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            LayoutParams.TYPE_PHONE
        }

        val lp = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            layoutFlag,
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params = lp

        // Default position: Left middle
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = 0
        lp.y = displaySize.y / 2 - 100

        wm.addView(view, lp)
        isViewAttached = true

        val bubble = view.findViewById<androidx.cardview.widget.CardView>(R.id.floating_bubble)
        val quickActions = view.findViewById<View>(R.id.layout_quick_actions)
        
        // Apply size settings
        val sizeVal = sm.bubbleSize.let { if (it < 40) 64 else it }
        val bubbleSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeVal.toFloat(), resources.displayMetrics).toInt()
        
        val bubbleParams = bubble.layoutParams
        bubbleParams.width = bubbleSizePx
        bubbleParams.height = bubbleSizePx
        bubble.layoutParams = bubbleParams
        bubble.radius = bubbleSizePx / 2f
        bubble.alpha = sm.bubbleOpacity.coerceAtLeast(0.5f)

        // Force visibility
        view.visibility = View.VISIBLE
        bubble.visibility = View.VISIBLE

        bubble.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false
            private var touchStartTime = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val currentParams = params ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = currentParams.x
                        initialY = currentParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        touchStartTime = System.currentTimeMillis()
                        dismissZoneView?.visibility = View.VISIBLE
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY

                        if (abs(dx) > 10 || abs(dy) > 10) {
                            isDragging = true
                        }

                        if (isDragging) {
                            currentParams.x = (initialX + dx).toInt()
                            currentParams.y = (initialY + dy).toInt()
                            constrainParams(currentParams, view)
                            windowManager?.updateViewLayout(view, currentParams)
                            checkDismissZone(event.rawX, event.rawY)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        dismissZoneView?.visibility = View.GONE
                        val touchDuration = System.currentTimeMillis() - touchStartTime
                        
                        if (!isDragging) {
                            if (touchDuration < 500) {
                                incrementCount()
                            } else {
                                toggleQuickActions(quickActions)
                            }
                        } else {
                            if (isInDismissZone(event.rawX, event.rawY)) {
                                sm.prefs.edit().putBoolean("pref_floating_enabled", false).apply()
                                stopSelf()
                            } else {
                                snapToEdge()
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        view.findViewById<View>(R.id.btn_floating_decrease).setOnClickListener { decrementCount() }
        view.findViewById<View>(R.id.btn_floating_reset).setOnClickListener { resetCount() }
        view.findViewById<View>(R.id.btn_floating_open_app).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            if (isQuickActionsExpanded) toggleQuickActions(quickActions)
        }
    }

    private fun constrainParams(lp: LayoutParams, view: View) {
        val viewWidth = view.width.coerceAtLeast(1)
        val viewHeight = view.height.coerceAtLeast(1)
        
        if (lp.x < 0) lp.x = 0
        if (lp.y < 0) lp.y = 0
        
        if (lp.x + viewWidth > displaySize.x) {
            lp.x = displaySize.x - viewWidth
        }
        if (lp.y + viewHeight > displaySize.y) {
            lp.y = displaySize.y - viewHeight
        }
    }

    private fun setupDismissZone() {
        val wm = windowManager ?: return
        val view = View(this).apply {
            setBackgroundColor(0x88FF0000.toInt())
            visibility = View.GONE
        }
        dismissZoneView = view

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            LayoutParams.TYPE_PHONE
        }

        val lp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            240,
            layoutFlag,
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }
        dismissZoneParams = lp

        wm.addView(view, lp)
        isDismissAttached = true
    }

    private fun checkDismissZone(rawX: Float, rawY: Float) {
        if (isInDismissZone(rawX, rawY)) {
            dismissZoneView?.setBackgroundColor(0xCCFF0000.toInt())
        } else {
            dismissZoneView?.setBackgroundColor(0x88FF0000.toInt())
        }
    }

    private fun isInDismissZone(rawX: Float, rawY: Float): Boolean {
        return rawY > (displaySize.y - 240)
    }

    private fun snapToEdge() {
        val view = floatingView ?: return
        val wm = windowManager ?: return
        val lp = params ?: return
        
        val middle = displaySize.x / 2
        lp.x = if (lp.x + view.width / 2 < middle) 0 else displaySize.x - view.width
        constrainParams(lp, view)
        wm.updateViewLayout(view, lp)
    }

    private fun toggleQuickActions(quickActions: View) {
        isQuickActionsExpanded = !isQuickActionsExpanded
        quickActions.visibility = if (isQuickActionsExpanded) View.VISIBLE else View.GONE
    }

    private fun observeActiveSession() {
        val view = floatingView ?: return
        val repo = repository ?: return
        val sm = settingsManager ?: return
        
        sessionJob?.cancel()
        sessionJob = lifecycleScope.launch {
            val sessionId = sm.activeSessionId
            repo.getSessionFlow(sessionId).collectLatest { session ->
                activeSession = session
                session?.let {
                    val textCount = view.findViewById<TextView>(R.id.text_floating_count)
                    textCount.text = it.count.toString()
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateDisplaySize()
        if (floatingView != null && isViewAttached) {
            snapToEdge()
        }
    }

    private fun incrementCount() {
        val repo = repository ?: return
        val hr = historyRepository ?: return
        activeSession?.let { current ->
            lifecycleScope.launch {
                repo.incrementCount(current.id, current.incrementValue)
                hr.logEvent(current.id, current.name, "COUNT_CHANGED", current.incrementValue)
                launch(Dispatchers.Main) {
                    if (current.goalCount > 0 && current.count + current.incrementValue >= current.goalCount && current.count < current.goalCount) {
                        soundManager?.playSound(SoundManager.SoundType.GOAL_REACHED)
                    } else {
                        soundManager?.playSound(SoundManager.SoundType.INCREMENT)
                    }
                }
            }
        }
    }

    private fun decrementCount() {
        val repo = repository ?: return
        val hr = historyRepository ?: return
        val sm = settingsManager ?: return
        activeSession?.let { current ->
            lifecycleScope.launch {
                repo.decrementCount(current.id, current.decrementValue, sm.isNegativeCountAllowed)
                hr.logEvent(current.id, current.name, "COUNT_CHANGED", -current.decrementValue)
                launch(Dispatchers.Main) {
                    soundManager?.playSound(SoundManager.SoundType.DECREMENT)
                }
            }
        }
    }

    private fun resetCount() {
        val repo = repository ?: return
        val hr = historyRepository ?: return
        activeSession?.let { current ->
            lifecycleScope.launch {
                repo.resetCount(current.id)
                hr.logEvent(current.id, current.name, "RESET", -current.count)
                launch(Dispatchers.Main) {
                    soundManager?.playSound(SoundManager.SoundType.RESET)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager?.playSound(SoundManager.SoundType.FLOATING_DISMISS)
        settingsManager?.prefs?.unregisterOnSharedPreferenceChangeListener(prefListener)
        
        val wm = windowManager
        floatingView?.let {
            if (isViewAttached && wm != null) {
                try { wm.removeView(it) } catch (e: Exception) {}
            }
        }
        dismissZoneView?.let {
            if (isDismissAttached && wm != null) {
                try { wm.removeView(it) } catch (e: Exception) {}
            }
        }
    }
}
