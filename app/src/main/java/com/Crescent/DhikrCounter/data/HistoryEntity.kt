package com.Crescent.DhikrCounter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var sessionId: Long,
    var sessionName: String,
    var eventType: String, // "SESSION", "STARTED", "FINISHED", "COUNT_CHANGED", "GOAL_COMPLETED"
    var countChange: Long,
    var timestamp: Long,
    // Session-based fields
    var duration: Long = 0,
    var isGoalMet: Boolean = false,
    var startCount: Long = 0,
    var endCount: Long = 0
)
