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
    val longestStreak: Int = 0,
    val daysActive: Int = 0
)

data class PeriodStats(
    val totalCount: Long = 0,
    val averagePerDay: Long = 0,
    val maxCountPerDay: Long = 0,
    val daysActive: Int = 0,
    val goalsCompleted: Int = 0,
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
            longestStreak = longestStreak,
            daysActive = activeDays.size
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

    suspend fun getDailyActivityForRange(sessionId: Long?, range: String): List<Pair<Long, Long>> {
        val days = when (range) {
            "7D" -> 7
            "30D" -> 30
            "90D" -> 90
            "All" -> {
                val oldest = historyDao.getOldestTimestamp() ?: System.currentTimeMillis()
                val diffMillis = System.currentTimeMillis() - oldest
                val diffDays = (diffMillis / 86400000).toInt() + 1
                maxOf(7, diffDays)
            }
            else -> 7
        }
        return getDailyActivity(sessionId, days)
    }

    suspend fun getPeriodStats(range: String, dailyList: List<Pair<Long, Long>>): PeriodStats {
        val totalCount = dailyList.sumOf { it.second }
        val maxCountPerDay = dailyList.maxOfOrNull { it.second } ?: 0L
        val daysActive = dailyList.count { it.second > 0 }
        val averagePerDay = if (daysActive > 0) totalCount / daysActive else 0L

        val days = when (range) {
            "7D" -> 7
            "30D" -> 30
            "90D" -> 90
            "All" -> dailyList.size
            else -> 7
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))
        val startTime = if (range == "All") 0L else calendar.timeInMillis

        val goals = historyDao.getGlobalGoalsCompletedSince(startTime)

        val activeDayIndices = dailyList.filter { it.second > 0 }.map { it.first / 86400000 }
        val (currentStreak, longestStreak) = calculateStreak(activeDayIndices)

        return PeriodStats(
            totalCount = totalCount,
            averagePerDay = averagePerDay,
            maxCountPerDay = maxCountPerDay,
            daysActive = daysActive,
            goalsCompleted = goals,
            currentStreak = currentStreak,
            longestStreak = longestStreak
        )
    }

    suspend fun getSessionComparisonForRange(range: String): List<Pair<String, Long>> {
        val days = when (range) {
            "7D" -> 7
            "30D" -> 30
            "90D" -> 90
            "All" -> -1
            else -> 7
        }
        val startTime = if (days == -1) {
            0L
        } else {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))
            calendar.timeInMillis
        }
        val sessions = sessionDao.getAllSessions()
        return sessions.map { session ->
            val count = if (startTime == 0L) {
                historyDao.getLifetimeCount(session.id) ?: 0
            } else {
                historyDao.getCountSince(session.id, startTime) ?: 0
            }
            Pair(session.name, count)
        }
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

    suspend fun insert(history: HistoryEntity) {
        historyDao.insert(history)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    suspend fun migrateToSessions() {
        val allHistory = historyDao.getAllHistory()
        if (allHistory.isEmpty() || allHistory.any { it.eventType == "SESSION" }) return

        val groupedByCounter = allHistory.groupBy { it.sessionId }
        val newSessions = mutableListOf<HistoryEntity>()

        for ((counterId, entries) in groupedByCounter) {
            val sortedEntries = entries.sortedBy { it.timestamp }
            if (sortedEntries.isEmpty()) continue

            var currentSessionStart = sortedEntries[0]
            var lastEntry = sortedEntries[0]
            var accumulatedCount = 0L
            var goalMet = false

            for (i in sortedEntries.indices) {
                val entry = sortedEntries[i]
                
                // If gap is more than 5 minutes, end session and start new
                if (entry.timestamp - lastEntry.timestamp > 5 * 60 * 1000) {
                    newSessions.add(createSyntheticSession(currentSessionStart, lastEntry, accumulatedCount, goalMet))
                    currentSessionStart = entry
                    accumulatedCount = 0
                    goalMet = false
                }

                if (entry.eventType == "INCREMENT" || entry.eventType == "DECREMENT" || entry.eventType == "COUNT_CHANGED") {
                    accumulatedCount += entry.countChange
                } else if (entry.eventType == "GOAL_COMPLETED") {
                    goalMet = true
                }
                
                lastEntry = entry
            }
            newSessions.add(createSyntheticSession(currentSessionStart, lastEntry, accumulatedCount, goalMet))
        }

        if (newSessions.isNotEmpty()) {
            historyDao.clearAllHistory()
            newSessions.forEach { historyDao.insert(it) }
        }
    }

    private fun createSyntheticSession(start: HistoryEntity, end: HistoryEntity, count: Long, goalMet: Boolean): HistoryEntity {
        return HistoryEntity(
            sessionId = start.sessionId,
            sessionName = start.sessionName,
            eventType = "SESSION",
            countChange = count,
            timestamp = end.timestamp,
            duration = maxOf(60000, end.timestamp - start.timestamp),
            isGoalMet = goalMet,
            startCount = 0, // We don't know the exact start count from history
            endCount = count
        )
    }
}
