package com.Crescent.DhikrCounter.data

import com.Crescent.DhikrCounter.utils.HapticManager
import com.Crescent.DhikrCounter.utils.SettingsManager
import com.Crescent.DhikrCounter.utils.SoundManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UsageSessionManager(
    private val sessionRepository: SessionRepository,
    private val historyRepository: HistoryRepository,
    private val settingsManager: SettingsManager,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    
    private val _goalReachedEvent = MutableSharedFlow<Unit>(replay = 0)
    val goalReachedEvent: SharedFlow<Unit> = _goalReachedEvent

    var currentCounterId: Long = -1L
        private set
    var currentCounterName: String = ""
        private set
    var startCount: Long = 0
        private set
    var lastRecordedCount: Long = 0
        private set
    var startTime: Long = 0
        private set
    private var isGoalMetDuringSession: Boolean = false
    
    private var sessionEndJob: Job? = null
    
    // Callback for widget updates or other side effects
    var onCounterAction: (() -> Unit)? = null

    @Synchronized
    fun startSession(counterId: Long, counterName: String, currentCount: Long) {
        sessionEndJob?.cancel()
        if (currentCounterId != -1L) {
            if (currentCounterId == counterId) return // Already active for this counter
            endSessionInternal(lastRecordedCount) 
        }
        currentCounterId = counterId
        currentCounterName = counterName
        startCount = currentCount
        lastRecordedCount = currentCount
        startTime = System.currentTimeMillis()
        isGoalMetDuringSession = false
    }

    @Synchronized
    fun increment(session: SessionEntity) {
        scope.launch {
            mutex.withLock {
                val current = sessionRepository.getSession(session.id) ?: return@withLock
                val prevCount = current.count
                val nextCount = prevCount + current.incrementValue
                val goal = current.goalCount
                
                sessionRepository.incrementCount(current.id, current.incrementValue)
                onCounterAction?.invoke()
                
                // Goal Detection: prev < goal && next >= goal
                if (goal > 0 && prevCount < goal && nextCount >= goal) {
                    logGoalCompletion(current)
                    _goalReachedEvent.emit(Unit)
                    
                    withContext(Dispatchers.Main) {
                        hapticManager.vibrate()
                        soundManager.playSound(SoundManager.SoundType.GOAL_REACHED)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        hapticManager.vibrate()
                        soundManager.playSound(SoundManager.SoundType.INCREMENT)
                    }
                }
                
                synchronized(this@UsageSessionManager) {
                    if (currentCounterId == current.id) {
                        lastRecordedCount = nextCount
                        if (goal > 0 && nextCount >= goal) isGoalMetDuringSession = true
                    } else {
                        recordActivityInternal(current.id, current.name, nextCount, goal > 0 && nextCount >= goal)
                    }
                }
            }
        }
    }

    @Synchronized
    fun decrement(session: SessionEntity) {
        scope.launch {
            mutex.withLock {
                val current = sessionRepository.getSession(session.id) ?: return@withLock
                val prevCount = current.count
                val allowNegative = settingsManager.isNegativeCountAllowed
                val nextCount = if (prevCount - current.incrementValue < 0 && !allowNegative) 0L else prevCount - current.incrementValue
                
                sessionRepository.decrementCount(current.id, current.incrementValue, allowNegative)
                onCounterAction?.invoke()
                
                withContext(Dispatchers.Main) {
                    hapticManager.vibrate()
                    soundManager.playSound(SoundManager.SoundType.DECREMENT)
                }
                
                synchronized(this@UsageSessionManager) {
                    if (currentCounterId == current.id) {
                        lastRecordedCount = nextCount
                    } else {
                        recordActivityInternal(current.id, current.name, nextCount, isGoalMetDuringSession)
                    }
                }
            }
        }
    }

    @Synchronized
    fun reset(session: SessionEntity) {
        scope.launch {
            mutex.withLock {
                val current = sessionRepository.getSession(session.id) ?: return@withLock
                
                // End current session before reset
                synchronized(this@UsageSessionManager) {
                    if (currentCounterId == current.id) {
                        endSessionInternal(lastRecordedCount)
                    }
                }
                
                sessionRepository.resetCount(current.id)
                onCounterAction?.invoke()
                
                withContext(Dispatchers.Main) {
                    hapticManager.vibrateStrong()
                    soundManager.playSound(SoundManager.SoundType.RESET)
                }
                
                // Start new session
                synchronized(this@UsageSessionManager) {
                    startSession(current.id, current.name, 0)
                }
            }
        }
    }

    private suspend fun logGoalCompletion(session: SessionEntity) {
        val historyEntry = HistoryEntity(
            sessionId = session.id,
            sessionName = session.name,
            eventType = "GOAL_COMPLETED",
            countChange = 0, // Goal reached event itself doesn't change count, it marks a milestone
            timestamp = System.currentTimeMillis(),
            isGoalMet = true,
            endCount = session.count + session.incrementValue
        )
        historyRepository.insert(historyEntry)
    }

    @Synchronized
    fun updateGoalMet(met: Boolean) {
        if (met) isGoalMetDuringSession = true
    }

    @Synchronized
    fun endSession(endCount: Long) {
        sessionEndJob?.cancel()
        endSessionInternal(endCount)
    }

    private fun endSessionInternal(endCount: Long) {
        if (currentCounterId == -1L) return
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val countChange = endCount - startCount
        
        if (countChange != 0L) {
            val historyEntry = HistoryEntity(
                sessionId = currentCounterId,
                sessionName = currentCounterName,
                eventType = "SESSION",
                countChange = countChange,
                timestamp = endTime,
                duration = duration,
                isGoalMet = isGoalMetDuringSession,
                startCount = startCount,
                endCount = endCount
            )
            scope.launch {
                historyRepository.insert(historyEntry)
            }
        }
        
        currentCounterId = -1L
        isGoalMetDuringSession = false
    }
    
    @Synchronized
    fun recordWidgetActivity(counterId: Long, counterName: String, currentCount: Long, isGoalMet: Boolean) {
        recordActivityInternal(counterId, counterName, currentCount, isGoalMet)
    }

    private fun recordActivityInternal(counterId: Long, counterName: String, currentCount: Long, isGoalMet: Boolean) {
        if (currentCounterId != -1L && currentCounterId != counterId) {
            endSessionInternal(lastRecordedCount)
        }
        
        if (currentCounterId == -1L) {
            startSession(counterId, counterName, currentCount)
        }
        
        lastRecordedCount = currentCount
        if (isGoalMet) isGoalMetDuringSession = true
        
        // Reset timeout
        sessionEndJob?.cancel()
        sessionEndJob = scope.launch {
            delay(5 * 60 * 1000) // 5 minutes inactivity timeout for widget taps
            synchronized(this@UsageSessionManager) {
                endSessionInternal(lastRecordedCount)
            }
        }
    }
    
    @Synchronized
    fun isSessionActive(): Boolean = currentCounterId != -1L
}
