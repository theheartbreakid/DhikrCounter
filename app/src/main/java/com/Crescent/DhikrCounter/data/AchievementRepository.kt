package com.Crescent.DhikrCounter.data

import android.app.Application
import kotlinx.coroutines.flow.Flow

class AchievementRepository(application: Application) {
    private val achievementDao: AchievementDao
    val allAchievementsFlow: Flow<List<AchievementEntity>>

    init {
        val db = AppDatabase.getDatabase(application)
        achievementDao = db.achievementDao()
        allAchievementsFlow = achievementDao.getAllAchievementsFlow()
    }

    suspend fun getAchievement(id: String): AchievementEntity? {
        return achievementDao.getAchievement(id)
    }

    suspend fun unlockAchievement(id: String) {
        val achievement = getAchievement(id)
        if (achievement != null && !achievement.isUnlocked) {
            achievement.isUnlocked = true
            achievement.unlockedAt = System.currentTimeMillis()
            achievement.progress = 1.0f
            achievementDao.update(achievement)
        }
    }
}
