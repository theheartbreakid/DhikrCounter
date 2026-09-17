package com.Crescent.DhikrCounter.ui

import android.app.Application
import androidx.lifecycle.*
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.data.*
import com.Crescent.DhikrCounter.utils.SettingsManager
import com.Crescent.DhikrCounter.utils.SoundManager
import com.Crescent.DhikrCounter.ui.widgets.updateAllWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val hapticManager: com.Crescent.DhikrCounter.utils.HapticManager
    private val usageSessionManager: UsageSessionManager

    private val _goalReachedEvent = MutableSharedFlow<Unit>()
    val goalReachedEvent = _goalReachedEvent.asSharedFlow()

    val allSessions: LiveData<List<SessionEntity>>
    val allHistoryFlow: Flow<List<HistoryEntity>>
    
    private val activeSessionId = savedStateHandle.getLiveData<Long>("active_session_id")
    val activeSession: LiveData<SessionEntity?>
    val isFloatingEnabled = MutableLiveData<Boolean>()
    val isCountAnimationEnabled = MutableLiveData<Boolean>()
    val isNegativeCountAllowed = MutableLiveData<Boolean>()
    val isConfirmResetEnabled = MutableLiveData<Boolean>()
    val showResetConfirmation = MutableLiveData<Boolean>().apply { value = false }
    val cornerRadius = MutableLiveData<Float>()

    val isBackingUp = MutableLiveData<Boolean>(false)
    val isRestoring = MutableLiveData<Boolean>(false)
    val lastBackupTimestamp = MutableLiveData<Long>()

    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
        when (key) {
            "pref_floating_enabled" -> isFloatingEnabled.postValue(p.getBoolean("pref_floating_enabled", false))
            "pref_count_animation" -> isCountAnimationEnabled.postValue(p.getBoolean("pref_count_animation", SettingsManager.DEFAULT_COUNT_ANIMATION))
            "pref_allow_negative" -> isNegativeCountAllowed.postValue(p.getBoolean("pref_allow_negative", SettingsManager.DEFAULT_ALLOW_NEGATIVE))
            "pref_confirm_reset" -> isConfirmResetEnabled.postValue(p.getBoolean("pref_confirm_reset", SettingsManager.DEFAULT_CONFIRM_RESET))
            "pref_corner_radius" -> cornerRadius.postValue(p.getFloat("pref_corner_radius", SettingsManager.DEFAULT_CORNER_RADIUS))
            "last_backup_timestamp" -> lastBackupTimestamp.postValue(settingsManager.getLastBackupTimestamp())
            "active_session_id" -> {
                val newId = settingsManager.activeSessionId
                if (newId != activeSessionId.value) {
                    activeSessionId.postValue(newId)
                }
            }
        }
        if (key != null && (key.startsWith("pref_widget_") || key == "pref_amoled_mode" || key == "pref_theme_mode" || key == "pref_dynamic_colors")) {
            viewModelScope.launch { updateAllWidgets(getApplication()) }
        }
    }

    init {
        val app = application as DhikrApplication
        repository = app.sessionRepository
        historyRepository = app.historyRepository
        achievementRepository = app.achievementRepository
        settingsManager = app.settingsManager
        soundManager = app.soundManager
        hapticManager = app.hapticManager
        usageSessionManager = app.usageSessionManager
        
        allHistoryFlow = historyRepository.allHistoryFlow

        allSessions = repository.allSessionsFlow.asLiveData()
        settingsManager.prefs.registerOnSharedPreferenceChangeListener(prefListener)
        isFloatingEnabled.value = settingsManager.isFloatingBubbleEnabled
        isCountAnimationEnabled.value = settingsManager.isCountAnimationEnabled
        isNegativeCountAllowed.value = settingsManager.isNegativeCountAllowed
        isConfirmResetEnabled.value = settingsManager.isConfirmBeforeReset
        cornerRadius.value = settingsManager.cornerRadius
        lastBackupTimestamp.value = settingsManager.getLastBackupTimestamp()
        
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
        addSource(activeSession) { update() }
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
        addSource(allSessions) { update() }
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

    val statisticsRange = MutableLiveData<String>("7D")

    val statisticsDailyActivity = MediatorLiveData<List<Pair<Long, Long>>>().apply {
        val update = {
            val range = statisticsRange.value ?: "7D"
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    postValue(historyRepository.getDailyActivityForRange(null, range))
                } catch (e: Exception) {
                    e.printStackTrace()
                    postValue(emptyList())
                }
            }
        }
        addSource(statisticsRange) { update() }
        addSource(historyRepository.getHistoryChangeFlow().asLiveData()) { update() }
    }

    val statisticsPeriodStats = MediatorLiveData<PeriodStats>().apply {
        val update = {
            val range = statisticsRange.value ?: "7D"
            val daily = statisticsDailyActivity.value ?: emptyList()
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    postValue(historyRepository.getPeriodStats(range, daily))
                } catch (e: Exception) {
                    e.printStackTrace()
                    postValue(PeriodStats())
                }
            }
        }
        addSource(statisticsDailyActivity) { update() }
        addSource(statisticsRange) { update() }
    }

    val statisticsSessionComparison = MediatorLiveData<List<Pair<String, Long>>>().apply {
        val update = {
            val range = statisticsRange.value ?: "7D"
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    postValue(historyRepository.getSessionComparisonForRange(range))
                } catch (e: Exception) {
                    e.printStackTrace()
                    postValue(emptyList())
                }
            }
        }
        addSource(statisticsRange) { update() }
        addSource(historyRepository.getHistoryChangeFlow().asLiveData()) { update() }
        addSource(allSessions) { update() }
    }

    fun setActiveSessionId(id: Long) {
        activeSession.value?.let { current ->
            usageSessionManager.endSession(current.count)
        }
        settingsManager.activeSessionId = id
        activeSessionId.value = id
        viewModelScope.launch { 
            updateAllWidgets(getApplication())
            repository.getSession(id)?.let { newSession ->
                usageSessionManager.startSession(newSession.id, newSession.name, newSession.count)
            }
        }
    }

    fun increment() {
        activeSession.value?.let { current ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.incrementCount(current.id, current.incrementValue)
                // historyRepository.logEvent(current.id, current.name, "INCREMENT", current.incrementValue) // Removed per-tap logging
                updateAllWidgets(getApplication())
                
                val isGoalJustReached = current.goalCount > 0 && current.count + current.incrementValue >= current.goalCount && current.count < current.goalCount
                if (isGoalJustReached) {
                    // historyRepository.logEvent(current.id, current.name, "GOAL_COMPLETED", 0) // Removed
                    usageSessionManager.updateGoalMet(true)
                    _goalReachedEvent.emit(Unit)
                }
                
                launch(Dispatchers.Main) {
                    hapticManager.vibrate()
                    
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
                // historyRepository.logEvent(current.id, current.name, "DECREMENT", -current.incrementValue) // Removed per-tap logging
                updateAllWidgets(getApplication())
                launch(Dispatchers.Main) {
                    hapticManager.vibrate()
                    soundManager.playSound(SoundManager.SoundType.DECREMENT)
                }
            }
        }
    }

    // vibrateClick replaced by hapticManager.vibrate()

    // Removed vibrateClickSoft to unify with intensity-based vibration

    fun reset() {
        activeSession.value?.let { current ->
            viewModelScope.launch(Dispatchers.IO) {
                // Before reset, we should end the session to log what was achieved
                usageSessionManager.endSession(current.count)
                
                repository.resetCount(current.id)
                // historyRepository.logEvent(current.id, current.name, "RESET", -current.count) // Removed per-tap logging
                
                // Start a new session with 0 count
                usageSessionManager.startSession(current.id, current.name, 0)

                updateAllWidgets(getApplication())
                launch(Dispatchers.Main) {
                    hapticManager.vibrateStrong()
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

    fun exportBackup(context: android.content.Context, uri: android.net.Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            isBackingUp.value = true
            val success = com.Crescent.DhikrCounter.utils.BackupManagerUtil.exportBackup(context, uri)
            isBackingUp.value = false
            onComplete(success)
        }
    }

    fun validateBackup(context: android.content.Context, uri: android.net.Uri, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = com.Crescent.DhikrCounter.utils.BackupManagerUtil.validateBackup(context, uri)
            onComplete(result.first, result.second)
        }
    }

    fun restoreBackup(context: android.content.Context, uri: android.net.Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            isRestoring.value = true
            val success = com.Crescent.DhikrCounter.utils.BackupManagerUtil.restoreBackup(context, uri)
            isRestoring.value = false
            onComplete(success)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.clearHistory()
            updateAllWidgets(getApplication())
        }
    }

    fun toggleFloatingCounter() {
        val newValue = !(isFloatingEnabled.value ?: false)
        settingsManager.prefs.edit().putBoolean("pref_floating_enabled", newValue).apply()
    }

    override fun onCleared() {
        super.onCleared()
        settingsManager.prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}
