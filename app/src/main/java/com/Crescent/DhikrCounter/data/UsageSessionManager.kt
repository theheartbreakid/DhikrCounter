package com.Crescent.DhikrCounter.data

import kotlinx.coroutines.*

class UsageSessionManager(
    private val historyRepository: HistoryRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
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
    }
    
    @Synchronized
    fun recordWidgetActivity(counterId: Long, counterName: String, currentCount: Long, isGoalMet: Boolean) {
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
