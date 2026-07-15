package com.Crescent.DhikrCounter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var name: String,
    var count: Long,
    var incrementValue: Long,
    var decrementValue: Long,
    var createdAt: Long,
    var modifiedAt: Long,
    var listOrder: Int,
    var goalCount: Long = 0,
    var goalName: String = "",
    // Phase 3 Extensions
    var description: String = "",
    var colorHex: String = "#6200EE",
    var category: String = "General",
    var notes: String = "",
    var tags: String = "", // Comma separated tags
    var lastUsedAt: Long = 0
)
