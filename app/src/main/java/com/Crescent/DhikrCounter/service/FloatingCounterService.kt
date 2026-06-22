package com.Crescent.DhikrCounter.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.util.TypedValue
import android.view.*
import android.view.WindowManager.LayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.MainActivity
import com.Crescent.DhikrCounter.R
import com.Crescent.DhikrCounter.data.SessionEntity
import com.Crescent.DhikrCounter.data.SessionRepository
import com.Crescent.DhikrCounter.data.HistoryRepository
import com.Crescent.DhikrCounter.utils.SettingsManager
import com.Crescent.DhikrCounter.utils.SoundManager
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow

class FloatingCounterService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "floating_counter_channel"
        private const val NOTIFICATION_ID = 1
        private const val COLLAPSED_MARGIN_DP = 8
        private const val EXPANDED_MARGIN_DP = 50
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: LayoutParams? = null
    private var dismissZoneView: FrameLayout? = null
    private var dismissZoneParams: LayoutParams? = null
    private var dismissIcon: ImageView? = null

    private var repository: SessionRepository? = null
    private var historyRepository: HistoryRepository? = null
    private var settingsManager: SettingsManager? = null
    private var soundManager: SoundManager? = null
    private var activeSession: SessionEntity? = null
    private var sessionJob: kotlinx.coroutines.Job? = null

    private var isQuickActionsExpanded = false
    private var isDragging = false
    private val displaySize = Point()
    private var isViewAttached = false
    private var isDismissAttached = false

    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var isLongPressActive = false

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "active_session_id") {
            observeActiveSession()
        } else if (key == "pref_bubble_size" || key == "pref_bubble_opacity") {
            updateFloatingViewSize()
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
                wm.defaultDisplay.getRealSize(displaySize)
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

        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = 0
        lp.y = displaySize.y / 2 - 100

        wm.addView(view, lp)
        isViewAttached = true

        updateFloatingViewSize()
        updateAdaptiveColors()

        val bubble = view.findViewById<MaterialCardView>(R.id.floating_bubble)
        
        bubble.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var touchStartTime = 0L
            private val touchSlop = ViewConfiguration.get(this@FloatingCounterService).scaledTouchSlop

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val currentParams = params ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = currentParams.x
                        initialY = currentParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        isLongPressActive = false
                        touchStartTime = System.currentTimeMillis()
                        
                        bubble.animate().scaleX(1.15f).scaleY(1.15f).setDuration(200).start()
                        
                        longPressRunnable = Runnable {
                            if (!isDragging) {
                                isLongPressActive = true
                                toggleQuickActions(true)
                                bubble.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                        }
                        handler.postDelayed(longPressRunnable!!, 500)
                        
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY

                        if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                            isDragging = true
                            handler.removeCallbacks(longPressRunnable!!)
                            // Seamless transition: Trigger reverse animation instead of instant hide
                            if (isQuickActionsExpanded) toggleQuickActions(false)
                            showDismissZone(true)
                        }

                        if (isDragging) {
                            var targetX = (initialX + dx).toInt()
                            var targetY = (initialY + dy).toInt()
                            
                            val centerX = displaySize.x / 2
                            val centerY = displaySize.y - 120
                            val dist = hypot(event.rawX - centerX, event.rawY - centerY)
                            
                            val farThreshold = 650f
                            val captureThreshold = 180f
                            
                            if (dist < farThreshold) {
                                val strength = (1.0f - (dist / farThreshold).coerceIn(0.0f, 1.0f)).pow(3.0f)
                                targetX = (targetX * (1 - strength) + (centerX - v.width / 2) * strength).toInt()
                                targetY = (targetY * (1 - strength) + (centerY - v.height / 2) * strength).toInt()
                                
                                val targetScale = 1.1f + strength * 0.9f
                                dismissIcon?.scaleX = targetScale
                                dismissIcon?.scaleY = targetScale
                                dismissZoneView?.alpha = 0.4f + strength * 0.6f
                                
                                val bubbleBaseScale = 1.15f
                                val shrinkFactor = 1.0f - strength * 0.7f
                                bubble.scaleX = bubbleBaseScale * shrinkFactor
                                bubble.scaleY = bubbleBaseScale * shrinkFactor * (1.0f + strength * 0.3f)
                                bubble.alpha = 1.0f - strength * 0.5f
                                
                                if (dist < captureThreshold) {
                                    bubble.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            } else {
                                dismissIcon?.scaleX = 1.1f
                                dismissIcon?.scaleY = 1.1f
                                dismissZoneView?.alpha = 0.4f
                                bubble.scaleX = 1.15f
                                bubble.scaleY = 1.15f
                                bubble.alpha = 1.0f
                            }

                            val bubbleLp = bubble.layoutParams as FrameLayout.LayoutParams
                            val minX = -bubbleLp.leftMargin
                            val maxX = displaySize.x - (view.width - bubbleLp.rightMargin)
                            
                            if (targetX < minX) targetX = minX
                            if (targetX > maxX) targetX = maxX
                            
                            val minY = -bubbleLp.topMargin
                            val maxY = displaySize.y - (view.height - bubbleLp.bottomMargin)
                            
                            if (targetY < minY) targetY = minY
                            if (targetY > maxY) targetY = maxY
                            
                            currentParams.x = targetX
                            currentParams.y = targetY
                            windowManager?.updateViewLayout(view, currentParams)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val wasDragging = isDragging
                        isDragging = false
                        handler.removeCallbacks(longPressRunnable!!)
                        bubble.animate().scaleX(1f).scaleY(1f).alpha(1.0f).setDuration(200).start()
                        
                        if (!wasDragging) {
                            if (!isLongPressActive) {
                                if (isQuickActionsExpanded) {
                                    toggleQuickActions(false)
                                } else {
                                    incrementCount()
                                }
                            }
                        } else {
                            showDismissZone(false)
                            if (isInDismissZone(event.rawX, event.rawY)) {
                                animateMagneticConsumption()
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
            if (isQuickActionsExpanded) toggleQuickActions(false)
        }
    }

    private fun updateFloatingViewSize() {
        val view = floatingView ?: return
        val sm = settingsManager ?: return
        val bubble = view.findViewById<MaterialCardView>(R.id.floating_bubble)
        
        val sizeVal = sm.bubbleSize.let { if (it < 40) 64 else it }
        val bubbleSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeVal.toFloat(), resources.displayMetrics).toInt()
        
        val bubbleParams = bubble.layoutParams as FrameLayout.LayoutParams
        bubbleParams.width = bubbleSizePx
        bubbleParams.height = bubbleSizePx
        
        val safeMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, COLLAPSED_MARGIN_DP.toFloat(), resources.displayMetrics).toInt()
        if (!isQuickActionsExpanded) {
            bubbleParams.setMargins(safeMargin, safeMargin, safeMargin, safeMargin)
        }
        
        bubble.layoutParams = bubbleParams
        bubble.radius = bubbleSizePx / 2f
        bubble.alpha = sm.bubbleOpacity.coerceAtLeast(0.3f)

        val orbitSize = (bubbleSizePx * 0.35f).toInt()
        val orbitIconPadding = (orbitSize * 0.18f).toInt()
        
        val orbitCards = arrayOf(R.id.btn_floating_decrease_card, R.id.btn_floating_reset_card, R.id.btn_floating_open_app_card)
        orbitCards.forEach { id ->
            val card = view.findViewById<MaterialCardView>(id)
            val lp = card.layoutParams
            lp.width = orbitSize
            lp.height = orbitSize
            card.layoutParams = lp
            card.radius = orbitSize / 2f
            
            val btn = card.findViewById<ImageButton>(R.id.btn_floating_decrease) 
                ?: card.findViewById<ImageButton>(R.id.btn_floating_reset)
                ?: card.findViewById<ImageButton>(R.id.btn_floating_open_app)
            
            btn?.setPadding(orbitIconPadding, orbitIconPadding, orbitIconPadding, orbitIconPadding)
        }
    }

    private fun updateAdaptiveColors() {
        val view = floatingView ?: return
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        val bubble = view.findViewById<MaterialCardView>(R.id.floating_bubble)
        val textCount = view.findViewById<TextView>(R.id.text_floating_count)
        
        val orbitCards = arrayOf(R.id.btn_floating_decrease_card, R.id.btn_floating_reset_card, R.id.btn_floating_open_app_card)
        
        if (isDark) {
            bubble.setCardBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_light_primaryContainer))
            bubble.strokeColor = ContextCompat.getColor(this, R.color.md_theme_light_primary)
            textCount.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onPrimaryContainer))
            
            orbitCards.forEach { id ->
                val card = view.findViewById<MaterialCardView>(id)
                card.setCardBackgroundColor(Color.parseColor("#EEF0F0F0"))
                card.strokeColor = Color.parseColor("#40000000")
                val btn = card.findViewById<ImageButton>(R.id.btn_floating_decrease) 
                    ?: card.findViewById<ImageButton>(R.id.btn_floating_reset)
                    ?: card.findViewById<ImageButton>(R.id.btn_floating_open_app)
                btn?.imageTintList = ColorStateList.valueOf(Color.BLACK)
            }
        } else {
            bubble.setCardBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_dark_primaryContainer))
            bubble.strokeColor = ContextCompat.getColor(this, R.color.md_theme_dark_primary)
            textCount.setTextColor(ContextCompat.getColor(this, R.color.md_theme_dark_onPrimaryContainer))
            
            orbitCards.forEach { id ->
                val card = view.findViewById<MaterialCardView>(id)
                card.setCardBackgroundColor(Color.parseColor("#EE1A1C1E"))
                card.strokeColor = Color.parseColor("#40FFFFFF")
                val btn = card.findViewById<ImageButton>(R.id.btn_floating_decrease) 
                    ?: card.findViewById<ImageButton>(R.id.btn_floating_reset)
                    ?: card.findViewById<ImageButton>(R.id.btn_floating_open_app)
                btn?.imageTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
    }

    private fun setupDismissZone() {
        val wm = windowManager ?: return
        
        val container = FrameLayout(this).apply {
            visibility = View.GONE
            alpha = 0f
        }
        dismissZoneView = container

        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64f, resources.displayMetrics).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xAAFF0000.toInt())
                setStroke(4, Color.WHITE)
            }
            elevation = 12f
            scaleX = 1.1f
            scaleY = 1.1f
        }
        dismissIcon = icon
        container.addView(icon)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            LayoutParams.TYPE_PHONE
        }

        val lp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            350,
            layoutFlag,
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }
        dismissZoneParams = lp

        wm.addView(container, lp)
        isDismissAttached = true
    }

    private fun showDismissZone(show: Boolean) {
        dismissZoneView?.let {
            if (show) {
                it.visibility = View.VISIBLE
                it.animate().alpha(0.4f).translationY(0f).setDuration(300).start()
                
                val pulse = ValueAnimator.ofFloat(1.1f, 1.25f).apply {
                    duration = 800
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    addUpdateListener { anim ->
                        if (!isDraggingCaptured()) {
                            dismissIcon?.scaleX = anim.animatedValue as Float
                            dismissIcon?.scaleY = anim.animatedValue as Float
                        }
                    }
                }
                dismissIcon?.setTag(R.id.floating_bubble, pulse)
                pulse.start()
            } else {
                (dismissIcon?.getTag(R.id.floating_bubble) as? ValueAnimator)?.cancel()
                it.animate().alpha(0f).translationY(100f).setDuration(300).withEndAction {
                    it.visibility = View.GONE
                }.start()
            }
        }
    }
    
    private fun isDraggingCaptured(): Boolean {
        val view = floatingView ?: return false
        val lp = params ?: return false
        val centerX = displaySize.x / 2
        val centerY = displaySize.y - 120
        val dist = hypot((lp.x + view.width / 2) - centerX.toFloat(), (lp.y + view.height / 2) - centerY.toFloat())
        return dist < 300
    }

    private fun isInDismissZone(rawX: Float, rawY: Float): Boolean {
        val centerX = displaySize.x / 2
        val centerY = displaySize.y - 120
        val dist = hypot(rawX - centerX, rawY - centerY)
        return dist < 250
    }

    private fun animateMagneticConsumption() {
        val view = floatingView ?: return
        val lp = params ?: return
        val wm = windowManager ?: return
        val icon = dismissIcon ?: return
        
        val centerX = displaySize.x / 2
        val centerY = displaySize.y - 120
        
        (icon.getTag(R.id.floating_bubble) as? ValueAnimator)?.cancel()
        
        val startX = lp.x.toFloat()
        val startY = lp.y.toFloat()
        val targetX = (centerX - view.width / 2).toFloat()
        val targetY = (centerY - view.height / 2).toFloat()
        
        val bubble = view.findViewById<View>(R.id.floating_bubble)
        
        val collapseAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = AccelerateInterpolator(2f)
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                lp.x = (startX + (targetX - startX) * f).toInt()
                lp.y = (startY + (targetY - startY) * f).toInt()
                try { wm.updateViewLayout(view, lp) } catch (e: Exception) {}
                
                val s = 1.0f - f
                bubble.scaleX = s
                bubble.scaleY = s
                bubble.alpha = s
            }
        }

        val expandAnim = ValueAnimator.ofFloat(icon.scaleX, 2.8f).apply {
            duration = 200
            interpolator = OvershootInterpolator(1.5f)
            addUpdateListener { anim ->
                val s = anim.animatedValue as Float
                icon.scaleX = s
                icon.scaleY = s
            }
        }
        
        val settleAnim = ValueAnimator.ofFloat(2.8f, 0f).apply {
            duration = 250
            interpolator = AccelerateInterpolator()
            addUpdateListener { anim ->
                val s = anim.animatedValue as Float
                icon.scaleX = s
                icon.scaleY = s
                icon.alpha = s
            }
        }

        val sequence = AnimatorSet()
        sequence.play(collapseAnim).before(expandAnim)
        sequence.play(expandAnim).before(settleAnim)
        sequence.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                settingsManager?.prefs?.edit()?.putBoolean("pref_floating_enabled", false)?.apply()
                stopSelf()
            }
        })
        sequence.start()
    }

    private fun snapToEdge() {
        val view = floatingView ?: return
        val wm = windowManager ?: return
        val lp = params ?: return
        val bubble = view.findViewById<View>(R.id.floating_bubble)
        val bubbleLp = bubble.layoutParams as FrameLayout.LayoutParams
        
        val safeMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, COLLAPSED_MARGIN_DP.toFloat(), resources.displayMetrics).toInt()
        val expandMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, EXPANDED_MARGIN_DP.toFloat(), resources.displayMetrics).toInt()
        
        // Collapse window logic
        if (!isQuickActionsExpanded && bubbleLp.leftMargin != safeMargin) {
            val diff = expandMargin - safeMargin
            lp.x += diff
            lp.y += diff
            bubbleLp.setMargins(safeMargin, safeMargin, safeMargin, safeMargin)
            bubble.layoutParams = bubbleLp
            try { wm.updateViewLayout(view, lp) } catch (e: Exception) {}
        }

        val middle = displaySize.x / 2
        val targetX = if (lp.x + view.width / 2 < middle) {
            -bubbleLp.leftMargin
        } else {
            displaySize.x - (view.width - bubbleLp.rightMargin)
        }
        
        val startX = lp.x
        val animator = ValueAnimator.ofInt(startX, targetX)
        animator.addUpdateListener { anim ->
            lp.x = anim.animatedValue as Int
            try { wm.updateViewLayout(view, lp) } catch (e: Exception) {}
        }
        animator.duration = 450
        animator.interpolator = OvershootInterpolator(1.2f)
        animator.start()
    }

    private fun toggleQuickActions(expand: Boolean) {
        if (isQuickActionsExpanded == expand) return
        isQuickActionsExpanded = expand
        
        val view = floatingView ?: return
        val lp = params ?: return
        val bubble = view.findViewById<View>(R.id.floating_bubble)
        val bubbleLp = bubble.layoutParams as FrameLayout.LayoutParams
        
        val decrease = view.findViewById<View>(R.id.btn_floating_decrease_card)
        val reset = view.findViewById<View>(R.id.btn_floating_reset_card)
        val openApp = view.findViewById<View>(R.id.btn_floating_open_app_card)
        
        val bubbleSize = bubble.width
        val offset = bubbleSize * 0.72f
        val diag = offset * 0.707f
        
        val safeMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, COLLAPSED_MARGIN_DP.toFloat(), resources.displayMetrics).toInt()
        val expandMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, EXPANDED_MARGIN_DP.toFloat(), resources.displayMetrics).toInt()
        
        if (expand) {
            bubbleLp.setMargins(expandMargin, expandMargin, expandMargin, expandMargin)
            bubble.layoutParams = bubbleLp
            
            val diff = expandMargin - safeMargin
            lp.x -= diff
            lp.y -= diff
            try { windowManager?.updateViewLayout(view, lp) } catch (e: Exception) {}
            
            listOf(decrease, reset, openApp).forEach { 
                it.visibility = View.VISIBLE
                it.scaleX = 0f; it.scaleY = 0f; it.alpha = 0f
                it.translationX = 0f; it.translationY = 0f
            }
            
            decrease.animate().translationX(-diag).translationY(-diag).scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(450).setInterpolator(OvershootInterpolator()).start()
            reset.animate().translationX(diag).translationY(-diag).scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(450).setInterpolator(OvershootInterpolator()).start()
            openApp.animate().translationX(0f).translationY(offset).scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(450).setInterpolator(OvershootInterpolator()).start()
        } else {
            // REVERSE POP ANIMATION: All action orbs return inward smoothly
            listOf(decrease, reset, openApp).forEach { 
                it.animate()
                    .translationX(0f)
                    .translationY(0f)
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .setDuration(350)
                    .setInterpolator(AnticipateInterpolator())
                    .withEndAction { it.visibility = View.INVISIBLE }
                    .start()
            }
            
            // Only shrink the window origin immediately if NOT dragging to avoid visual jumping
            if (!isDragging) {
                bubbleLp.setMargins(safeMargin, safeMargin, safeMargin, safeMargin)
                bubble.layoutParams = bubbleLp
                
                val diff = expandMargin - safeMargin
                lp.x += diff
                lp.y += diff
                try { windowManager?.updateViewLayout(view, lp) } catch (e: Exception) {}
                
                snapToEdge()
            }
            // If dragging, snapToEdge() will eventually trigger the collapse when the user releases.
        }
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
                    textCount.animate().scaleX(1.25f).scaleY(1.25f).setDuration(120).withEndAction {
                        textCount.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    }.start()
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateDisplaySize()
        updateAdaptiveColors()
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
                repo.decrementCount(current.id, current.incrementValue, sm.isNegativeCountAllowed)
                hr.logEvent(current.id, current.name, "COUNT_CHANGED", -current.incrementValue)
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
