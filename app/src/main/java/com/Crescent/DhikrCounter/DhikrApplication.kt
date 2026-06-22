package com.Crescent.DhikrCounter

import android.app.Application
import androidx.preference.PreferenceManager
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

    override fun onCreate() {
        super.onCreate()

        settingsManager = SettingsManager(this)
        soundManager = SoundManager(this, settingsManager)
        sessionRepository = SessionRepository(this)
        historyRepository = HistoryRepository(this)
        achievementRepository = AchievementRepository(this)

        // Check first launch
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            createDefaultSession()
            prefs.edit().putBoolean("is_first_launch", false).apply()
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
