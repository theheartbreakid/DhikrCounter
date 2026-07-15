package com.Crescent.DhikrCounter.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT COUNT(*) FROM sessions")
    fun getSessionsChangeFlow(): Flow<Int>

    @Query("SELECT * FROM sessions ORDER BY listOrder ASC")
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionFlow(sessionId: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions ORDER BY listOrder ASC")
    suspend fun getAllSessions(): List<SessionEntity>
        
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionSync(sessionId: Long): SessionEntity?

    @Query("SELECT MAX(listOrder) FROM sessions")
    suspend fun getMaxListOrder(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("UPDATE sessions SET count = count + :incrementValue, modifiedAt = :timestamp WHERE id = :sessionId")
    suspend fun incrementCount(sessionId: Long, incrementValue: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sessions SET count = CASE WHEN count - :decrementValue < 0 AND :allowNegative = 0 THEN 0 ELSE count - :decrementValue END, modifiedAt = :timestamp WHERE id = :sessionId")
    suspend fun decrementCount(sessionId: Long, decrementValue: Long, allowNegative: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sessions SET count = 0, modifiedAt = :timestamp WHERE id = :sessionId")
    suspend fun resetCount(sessionId: Long, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("DELETE FROM sessions")
    suspend fun clearAllSessions()
}
