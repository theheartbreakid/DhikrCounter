package com.Crescent.DhikrCounter.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT COUNT(*) FROM history")
    fun getHistoryChangeFlow(): Flow<Int>

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getHistoryForSessionFlow(sessionId: Long): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    suspend fun getAllHistory(): List<HistoryEntity>

    @Query("SELECT SUM(countChange) FROM history WHERE sessionId = :sessionId AND eventType IN ('INCREMENT', 'DECREMENT', 'COUNT_CHANGED', 'SESSION')")
    suspend fun getLifetimeCount(sessionId: Long): Long?

    @Query("SELECT SUM(countChange) FROM history WHERE sessionId = :sessionId AND timestamp >= :startTime AND eventType IN ('INCREMENT', 'DECREMENT', 'COUNT_CHANGED', 'SESSION')")
    suspend fun getCountSince(sessionId: Long, startTime: Long): Long?

    @Query("SELECT SUM(countChange) FROM history WHERE sessionId = :sessionId AND timestamp >= :startTime AND timestamp < :endTime AND eventType IN ('INCREMENT', 'DECREMENT', 'COUNT_CHANGED', 'SESSION')")
    suspend fun getCountInRange(sessionId: Long, startTime: Long, endTime: Long): Long?

    @Query("SELECT SUM(countChange) FROM history WHERE timestamp >= :startTime AND timestamp < :endTime AND eventType IN ('INCREMENT', 'DECREMENT', 'COUNT_CHANGED', 'SESSION')")
    suspend fun getGlobalCountInRange(startTime: Long, endTime: Long): Long?

    @Query("SELECT COUNT(*) FROM history WHERE sessionId = :sessionId AND eventType = 'GOAL_COMPLETED'")
    suspend fun getGoalsCompletedCount(sessionId: Long): Int

    @Query("SELECT DISTINCT (timestamp / 86400000) FROM history WHERE sessionId = :sessionId AND eventType IN ('INCREMENT', 'COUNT_CHANGED', 'SESSION') ORDER BY timestamp DESC")
    suspend fun getActiveDays(sessionId: Long): List<Long>

    @Query("SELECT SUM(countChange) FROM history WHERE eventType IN ('INCREMENT', 'DECREMENT', 'COUNT_CHANGED', 'SESSION')")
    suspend fun getGlobalTotalCount(): Long?

    @Query("SELECT COUNT(DISTINCT sessionId) FROM history")
    suspend fun getGlobalActiveSessions(): Int

    @Query("SELECT COUNT(*) FROM history WHERE eventType = 'GOAL_COMPLETED'")
    suspend fun getGlobalGoalsCompleted(): Int

    @Query("SELECT DISTINCT (timestamp / 86400000) FROM history WHERE eventType IN ('INCREMENT', 'COUNT_CHANGED', 'SESSION') ORDER BY timestamp DESC")
    suspend fun getGlobalActiveDays(): List<Long>

    @Query("SELECT MAX(timestamp) FROM history WHERE sessionId = :sessionId")
    suspend fun getLastActivity(sessionId: Long): Long?

    @Query("SELECT MIN(timestamp) FROM history")
    suspend fun getOldestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM history WHERE eventType = 'GOAL_COMPLETED' AND timestamp >= :startTime")
    suspend fun getGlobalGoalsCompletedSince(startTime: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity): Long

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
}
