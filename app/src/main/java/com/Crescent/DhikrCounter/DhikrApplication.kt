package com.Crescent.DhikrCounter

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.glance.appwidget.updateAll
import com.Crescent.DhikrCounter.data.SessionEntity
import com.Crescent.DhikrCounter.data.SessionRepository
import com.Crescent.DhikrCounter.data.HistoryRepository
import com.Crescent.DhikrCounter.data.AchievementRepository
import com.Crescent.DhikrCounter.utils.SettingsManager
import com.Crescent.DhikrCounter.utils.SoundManager
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
    lateinit var settingsManager: SettingsManager
        private set
    lateinit var soundManager: SoundManager
        private set
        
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Store reference to prevent garbage collection
    private lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreate() {
        super.onCreate()

        settingsManager = SettingsManager(this)
        soundManager = SoundManager(this, settingsManager)
        sessionRepository = SessionRepository(this)
        historyRepository = HistoryRepository(this)
        achievementRepository = AchievementRepository(this)

        // Sanity Check on Startup
        applicationScope.launch(Dispatchers.IO) {
            validateAndRecoverData()
            
            // Listen for changes and update specific widgets
            launch {
                sessionRepository.allSessionsFlow.collect {
                    com.Crescent.DhikrCounter.ui.widgets.CounterWidget().updateAll(this@DhikrApplication)
                }
            }
            launch {
                historyRepository.allHistoryFlow.collect {
                    com.Crescent.DhikrCounter.ui.widgets.MotivationWidget().updateAll(this@DhikrApplication)
                }
            }
        }

        // Strong reference listener for preference changes
        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null && (key.startsWith("pref_") || key == "active_session_id" || key.startsWith("widget_"))) {
                applicationScope.launch(Dispatchers.IO) {
                    com.Crescent.DhikrCounter.ui.widgets.updateAllWidgets(this@DhikrApplication)
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
