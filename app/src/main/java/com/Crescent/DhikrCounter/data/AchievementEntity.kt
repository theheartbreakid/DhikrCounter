package com.Crescent.DhikrCounter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var achievementId: String, // e.g. "first_session", "100_counts", "7_day_streak"
    var name: String,
    var description: String,
    var isUnlocked: Boolean = false,
    var unlockedAt: Long = 0,
    var progress: Float = 0f // 0f to 1f
)
