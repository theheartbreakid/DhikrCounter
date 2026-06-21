package com.Crescent.DhikrCounter.data

import android.app.Application
import kotlinx.coroutines.flow.Flow

class HistoryRepository(application: Application) {
    private val historyDao: HistoryDao
    val allHistoryFlow: Flow<List<HistoryEntity>>

    init {
        val db = AppDatabase.getDatabase(application)
        historyDao = db.historyDao()
        allHistoryFlow = historyDao.getAllHistoryFlow()
    }

    fun getHistoryForSessionFlow(sessionId: Long): Flow<List<HistoryEntity>> {
        return historyDao.getHistoryForSessionFlow(sessionId)
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
