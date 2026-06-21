package com.Crescent.DhikrCounter.data

import android.app.Application
import kotlinx.coroutines.flow.Flow

class SessionRepository(application: Application) {

    private val sessionDao: SessionDao
    val allSessionsFlow: Flow<List<SessionEntity>>

    init {
        val db = AppDatabase.getDatabase(application)
        sessionDao = db.sessionDao()
        allSessionsFlow = sessionDao.getAllSessionsFlow()
    }

    fun getSessionFlow(sessionId: Long): Flow<SessionEntity?> {
        return sessionDao.getSessionFlow(sessionId)
    }
    
    suspend fun getSession(sessionId: Long): SessionEntity? {
        return sessionDao.getSession(sessionId)
    }

    suspend fun insert(session: SessionEntity): Long {
        val maxOrder = sessionDao.getMaxListOrder() ?: 0
        session.listOrder = maxOrder + 1
        return sessionDao.insert(session)
    }

    suspend fun update(session: SessionEntity) {
        sessionDao.update(session)
    }

    suspend fun incrementCount(sessionId: Long, incrementValue: Long) {
        sessionDao.incrementCount(sessionId, incrementValue)
    }

    suspend fun decrementCount(sessionId: Long, decrementValue: Long, allowNegative: Boolean) {
        sessionDao.decrementCount(sessionId, decrementValue, allowNegative)
    }

    suspend fun resetCount(sessionId: Long) {
        sessionDao.resetCount(sessionId)
    }

    suspend fun delete(session: SessionEntity) {
        sessionDao.delete(session)
    }
}
