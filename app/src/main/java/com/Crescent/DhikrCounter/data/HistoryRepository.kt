package com.Crescent.DhikrCounter.data

import android.app.Application
import kotlinx.coroutines.flow.Flow
import java.util.*

data class SessionStats(
    val lifetimeCount: Long = 0,
    val todayCount: Long = 0,
    val weeklyCount: Long = 0,
    val monthlyCount: Long = 0,
    val goalsCompleted: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActivityAt: Long = 0,
    val createdAt: Long = 0
)

data class GlobalStats(
    val totalCount: Long = 0,
    val activeSessions: Int = 0,
    val totalGoalsCompleted: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

class HistoryRepository(application: Application) {
    private val historyDao: HistoryDao
    private val sessionDao: SessionDao
    val allHistoryFlow: Flow<List<HistoryEntity>>

    init {
        val db = AppDatabase.getDatabase(application)
        historyDao = db.historyDao()
        sessionDao = db.sessionDao()
        allHistoryFlow = historyDao.getAllHistoryFlow()
    }

    fun getHistoryForSessionFlow(sessionId: Long): Flow<List<HistoryEntity>> {
        return historyDao.getHistoryForSessionFlow(sessionId)
    }

    fun getHistoryChangeFlow(): Flow<Int> = historyDao.getHistoryChangeFlow()
    fun getSessionsChangeFlow(): Flow<Int> = sessionDao.getSessionsChangeFlow()

    suspend fun getSessionStats(sessionId: Long): SessionStats {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Today
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        
        // This Week
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val weekStart = calendar.timeInMillis
        
        // This Month
        calendar.timeInMillis = now
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        
        val lifetime = historyDao.getLifetimeCount(sessionId) ?: 0
        val today = historyDao.getCountSince(sessionId, todayStart) ?: 0
        val weekly = historyDao.getCountSince(sessionId, weekStart) ?: 0
        val monthly = historyDao.getCountSince(sessionId, monthStart) ?: 0
        val goals = historyDao.getGoalsCompletedCount(sessionId)
        
        val activeDays = historyDao.getActiveDays(sessionId)
        val (currentStreak, longestStreak) = calculateStreak(activeDays)
        
        val lastActivity = historyDao.getLastActivity(sessionId) ?: 0
        
        val session = sessionDao.getSession(sessionId)
        val createdAt = session?.createdAt ?: 0

        return SessionStats(
            lifetimeCount = lifetime,
            todayCount = today,
            weeklyCount = weekly,
            monthlyCount = monthly,
            goalsCompleted = goals,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastActivityAt = lastActivity,
            createdAt = createdAt
        )
    }

    suspend fun getGlobalStats(): GlobalStats {
        val total = historyDao.getGlobalTotalCount() ?: 0
        val activeSessions = historyDao.getGlobalActiveSessions()
        val goals = historyDao.getGlobalGoalsCompleted()
        
        val activeDays = historyDao.getGlobalActiveDays()
        val (currentStreak, longestStreak) = calculateStreak(activeDays)
        
        return GlobalStats(
            totalCount = total,
            activeSessions = activeSessions,
            totalGoalsCompleted = goals,
            currentStreak = currentStreak,
            longestStreak = longestStreak
        )
    }

    suspend fun getDailyActivity(sessionId: Long?, days: Int): List<Pair<Long, Long>> {
        val result = mutableListOf<Pair<Long, Long>>()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Start from today and go back
        for (i in 0 until days) {
            val start = calendar.timeInMillis
            val end = start + 86400000
            
            val count = if (sessionId != null) {
                historyDao.getCountInRange(sessionId, start, end) ?: 0
            } else {
                historyDao.getGlobalCountInRange(start, end) ?: 0
            }
            result.add(Pair(start, count))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return result.reversed()
    }

    suspend fun getSessionComparison(): List<Pair<String, Long>> {
        val sessions = sessionDao.getAllSessions()
        return sessions.map { session ->
            val count = historyDao.getLifetimeCount(session.id) ?: 0
            Pair(session.name, count)
        }
    }

    private fun calculateStreak(activeDays: List<Long>): Pair<Int, Int> {
        if (activeDays.isEmpty()) return Pair(0, 0)
        
        // Sanitize data: must be positive, unique, and sorted descending
        val sortedDays = activeDays.filter { it > 0 }.distinct().sortedDescending()
        if (sortedDays.isEmpty()) return Pair(0, 0)
        
        val today = System.currentTimeMillis() / 86400000
        var currentStreak = 0
        
        // Current streak (can be today or starting from yesterday)
        if (sortedDays[0] == today || sortedDays[0] == today - 1) {
            currentStreak = 1
            for (i in 0 until sortedDays.size - 1) {
                if (sortedDays[i] - sortedDays[i+1] == 1L) {
                    currentStreak++
                } else {
                    break
                }
            }
        }

        // Longest streak
        var maxStreak = 1
        var tempStreak = 1
        for (i in 0 until sortedDays.size - 1) {
            if (sortedDays[i] - sortedDays[i+1] == 1L) {
                tempStreak++
            } else {
                maxStreak = maxOf(maxStreak, tempStreak)
                tempStreak = 1
            }
        }
        maxStreak = maxOf(maxStreak, tempStreak)
        
        return Pair(currentStreak, maxStreak)
    }

    suspend fun logEvent(sessionId: Long, sessionName: String, eventType: String, countChange: Long = 0) {
        val event = HistoryEntity(
            sessionId = sessionId,
            sessionName = sessionName,
            eventType = eventType,
            countChange = countChange,
            timestamp = System.currentTimeMillis()
        )
        historyDao.insert(event)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }
}
