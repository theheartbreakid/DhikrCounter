package com.Crescent.DhikrCounter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var sessionId: Long,
    var sessionName: String,
    var eventType: String, // "STARTED", "FINISHED", "COUNT_CHANGED", "GOAL_COMPLETED"
    var countChange: Long,
    var timestamp: Long
)
