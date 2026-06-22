package com.Crescent.DhikrCounter.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.*
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.data.SessionEntity
import com.Crescent.DhikrCounter.data.SessionRepository
import com.Crescent.DhikrCounter.data.HistoryRepository
import com.Crescent.DhikrCounter.data.SessionStats
import com.Crescent.DhikrCounter.data.GlobalStats
import com.Crescent.DhikrCounter.data.AchievementRepository
import com.Crescent.DhikrCounter.utils.SettingsManager
import com.Crescent.DhikrCounter.utils.SoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CounterViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository: SessionRepository
    private val historyRepository: HistoryRepository
    private val achievementRepository: AchievementRepository
    private val settingsManager: SettingsManager
    private val soundManager: SoundManager
    private val vibrator: Vibrator?

    val allSessions: LiveData<List<SessionEntity>>
    private val activeSessionId = savedStateHandle.getLiveData<Long>("active_session_id")
    val activeSession: LiveData<SessionEntity?>
    val isFloatingEnabled = MutableLiveData<Boolean>()
    val isCountAnimationEnabled = MutableLiveData<Boolean>()
    val isNegativeCountAllowed = MutableLiveData<Boolean>()
    val isConfirmResetEnabled = MutableLiveData<Boolean>()
    val showResetConfirmation = MutableLiveData<Boolean>().apply { value = false }
    val cornerRadius = MutableLiveData<Float>()
    
    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
        when (key) {
            "pref_floating_enabled" -> isFloatingEnabled.postValue(p.getBoolean("pref_floating_enabled", false))
            "pref_count_animation" -> isCountAnimationEnabled.postValue(p.getBoolean("pref_count_animation", true))
            "pref_allow_negative" -> isNegativeCountAllowed.postValue(p.getBoolean("pref_allow_negative", false))
            "pref_confirm_reset" -> isConfirmResetEnabled.postValue(p.getBoolean("pref_confirm_reset", true))
            "pref_corner_radius" -> cornerRadius.postValue(p.getFloat("pref_corner_radius", 24f))
        }
    }

    init {
        val app = application as DhikrApplication
        repository = app.sessionRepository
        historyRepository = app.historyRepository
        achievementRepository = app.achievementRepository
        settingsManager = app.settingsManager
        soundManager = app.soundManager
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        allSessions = repository.allSessionsFlow.asLiveData()
        settingsManager.prefs.registerOnSharedPreferenceChangeListener(prefListener)
        isFloatingEnabled.value = settingsManager.isFloatingBubbleEnabled
        isCountAnimationEnabled.value = settingsManager.isCountAnimationEnabled
        isNegativeCountAllowed.value = settingsManager.isNegativeCountAllowed
        isConfirmResetEnabled.value = settingsManager.isConfirmBeforeReset
        cornerRadius.value = settingsManager.cornerRadius
        
        if (activeSessionId.value == null) {
            activeSessionId.value = settingsManager.activeSessionId
        }
        
        activeSession = activeSessionId.switchMap { sessionId ->
            repository.getSessionFlow(sessionId).asLiveData()
        }
    }

    val sessionStats = MediatorLiveData<SessionStats>().apply {
        val update = {
            activeSessionId.value?.let { id ->
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        postValue(historyRepository.getSessionStats(id))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        postValue(SessionStats())
                    }
                }
            }
        }
        addSource(activeSessionId) { update() }
        addSource(historyRepository.getHistoryChangeFlow().asLiveData()) { update() }
        addSource(activeSession) { update() } // Reacts to renames, goal changes
    }

    val globalStats = MediatorLiveData<GlobalStats>().apply {
        val update = {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    postValue(historyRepository.getGlobalStats())
                } catch (e: Exception) {
                    e.printStackTrace()
                    postValue(GlobalStats())
                }
            }
        }
        addSource(historyRepository.getHistoryChangeFlow().asLiveData()) { update() }
        addSource(allSessions) { update() } // Reacts to session count changes, renames
    }

    val dailyActivity = MediatorLiveData<List<Pair<Long, Long>>>().apply {
        val update = {
            activeSessionId.value?.let { id ->
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        postValue(historyRepository.getDailyActivity(id, 7))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        postValue(emptyList())
                    }
                }
            }
        }
        addSource(activeSessionId) { update() }
        addSource(historyRepository.getHistoryChangeFlow().asLiveData()) { update() }
    }

    val dailyActivity30 = MediatorLiveData<List<Pair<Long, Long>>>().apply {
        val update = {
            activeSessionId.value?.let { id ->
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        postValue(historyRepository.getDailyActivity(id, 30))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        postValue(emptyList())
                    }
                }
            }
        }
        addSource(activeSessionId) { update() }
        addSource(historyRepository.getHistoryChangeFlow().asLiveData()) { update() }
    }

    val sessionComparison = MediatorLiveData<List<Pair<String, Long>>>().apply {
        val update = {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    postValue(historyRepository.getSessionComparison())
                } catch (e: Exception) {
                    e.printStackTrace()
                    postValue(emptyList())
                }
            }
        }
        addSource(historyRepository.getHistoryChangeFlow().asLiveData()) { update() }
        addSource(allSessions) { update() }
    }

    fun setActiveSessionId(id: Long) {
        settingsManager.activeSessionId = id
        activeSessionId.value = id
    }

    fun increment() {
        activeSession.value?.let { current ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.incrementCount(current.id, current.incrementValue)
                historyRepository.logEvent(current.id, current.name, "INCREMENT", current.incrementValue)
                
                val isGoalJustReached = current.goalCount > 0 && current.count + current.incrementValue >= current.goalCount && current.count < current.goalCount
                if (isGoalJustReached) {
                    historyRepository.logEvent(current.id, current.name, "GOAL_COMPLETED", 0)
                }
                
                launch(Dispatchers.Main) {
                    if (settingsManager.isHapticFeedbackEnabled) vibrateClick()
                    
                    if (isGoalJustReached) {
                        soundManager.playSound(SoundManager.SoundType.GOAL_REACHED)
                    } else {
                        soundManager.playSound(SoundManager.SoundType.INCREMENT)
                    }
                }
            }
        }
    }

    fun decrement() {
        activeSession.value?.let { current ->
            val allowNegative = settingsManager.isNegativeCountAllowed
            viewModelScope.launch(Dispatchers.IO) {
                repository.decrementCount(current.id, current.incrementValue, allowNegative)
                historyRepository.logEvent(current.id, current.name, "DECREMENT", -current.incrementValue)
                launch(Dispatchers.Main) {
                    if (settingsManager.isHapticFeedbackEnabled) vibrateClickSoft()
                    soundManager.playSound(SoundManager.SoundType.DECREMENT)
                }
            }
        }
    }

    private fun vibrateClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }
    
    private fun vibrateClickSoft() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(30, 50))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(30)
        }
    }

    fun reset() {
        activeSession.value?.let { current ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.resetCount(current.id)
                historyRepository.logEvent(current.id, current.name, "RESET", -current.count)
                launch(Dispatchers.Main) {
                    soundManager.playSound(SoundManager.SoundType.RESET)
                }
            }
        }
    }

    fun addSession(name: String) {
        val now = System.currentTimeMillis()
        val session = SessionEntity(
            name = name,
            count = 0,
            incrementValue = 1,
            decrementValue = 1,
            createdAt = now,
            modifiedAt = now,
            listOrder = 0
        )
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insert(session)
            launch(Dispatchers.Main) {
                setActiveSessionId(id)
            }
        }
    }
    
    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(session)
        }
    }
    
    fun updateSession(session: SessionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(session)
        }
    }

    fun toggleFloatingCounter() {
        val newValue = !(isFloatingEnabled.value ?: false)
        settingsManager.prefs.edit().putBoolean("pref_floating_enabled", newValue).apply()
        // No need to set LiveData here, the listener will handle it
    }

    override fun onCleared() {
        super.onCleared()
        settingsManager.prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}
