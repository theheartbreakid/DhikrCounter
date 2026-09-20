package com.Crescent.DhikrCounter

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.Crescent.DhikrCounter.ui.widgets.updateAllWidgets
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.Crescent.DhikrCounter.data.*
import com.Crescent.DhikrCounter.utils.SettingsManager
import com.Crescent.DhikrCounter.utils.SoundManager
import com.Crescent.DhikrCounter.utils.HapticManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DhikrApplication : Application() {

    lateinit var sessionRepository: SessionRepository
        private set
    lateinit var historyRepository: HistoryRepository
        private set
    lateinit var achievementRepository: AchievementRepository
        private set
    lateinit var usageSessionManager: UsageSessionManager
        private set
    lateinit var settingsManager: SettingsManager
        private set
    lateinit var soundManager: SoundManager
        private set
    lateinit var hapticManager: HapticManager
        private set
        
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Store reference to prevent garbage collection
    private lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreate() {
        super.onCreate()

        settingsManager = SettingsManager(this)
        soundManager = SoundManager(this, settingsManager)
        hapticManager = HapticManager(this)
        sessionRepository = SessionRepository(this)
        historyRepository = HistoryRepository(this)
        achievementRepository = AchievementRepository(this)
        usageSessionManager = UsageSessionManager(
            sessionRepository,
            historyRepository,
            settingsManager,
            soundManager,
            hapticManager
        ).apply {
            onCounterAction = {
                updateAllWidgets(this@DhikrApplication)
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                startUsageSession()
            }

            override fun onStop(owner: LifecycleOwner) {
                endUsageSession()
            }
        })

        // Sanity Check on Startup
        applicationScope.launch(Dispatchers.IO) {
            historyRepository.migrateToSessions()
            validateAndRecoverData()
            
            // Listen for data changes and update widgets
            launch {
                sessionRepository.allSessionsFlow.collect {
                    updateAllWidgets(this@DhikrApplication)
                }
            }
        }

        // Strong reference listener for preference changes
        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null) {
                if (key == "pref_floating_enabled") {
                    android.service.quicksettings.TileService.requestListeningState(
                        this,
                        android.content.ComponentName(this, com.Crescent.DhikrCounter.service.FloatingCounterTileService::class.java)
                    )
                }
                if (key == "active_session_id" || key.startsWith("pref_widget_") || (key.startsWith("pref_") && !key.startsWith("pref_glass_") && !key.startsWith("pref_bubble_"))) {
                    updateAllWidgets(this@DhikrApplication)
                }
            }
        }
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(prefListener)

        // Check first launch
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            createDefaultSession()
            prefs.edit().putBoolean("is_first_launch", false).apply()
        }
    }

    private fun startUsageSession() {
        val activeId = settingsManager.activeSessionId
        applicationScope.launch(Dispatchers.IO) {
            val session = sessionRepository.getSession(activeId)
            session?.let {
                usageSessionManager.startSession(it.id, it.name, it.count)
            }
        }
    }

    private fun endUsageSession() {
        val activeId = settingsManager.activeSessionId
        applicationScope.launch(Dispatchers.IO) {
            val session = sessionRepository.getSession(activeId)
            session?.let {
                usageSessionManager.endSession(it.count)
            }
        }
    }

    private suspend fun validateAndRecoverData() {
        try {
            val sessions = sessionRepository.getAllSessions()
            if (sessions.isEmpty()) {
                // If somehow database exists but no sessions, create one
                createDefaultSession()
            } else {
                val activeId = settingsManager.activeSessionId
                if (sessions.none { it.id == activeId }) {
                    settingsManager.activeSessionId = sessions[0].id
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createDefaultSession() {
        val now = System.currentTimeMillis()
        val defaultSession = SessionEntity(
            name = "Counter 1",
            count = 0,
            incrementValue = 1,
            decrementValue = 1,
            createdAt = now,
            modifiedAt = now,
            listOrder = 0
        )
        applicationScope.launch(Dispatchers.IO) {
            try {
                val id = sessionRepository.insert(defaultSession)
                settingsManager.activeSessionId = id
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
