package com.Crescent.DhikrCounter.utils

import android.content.Context
import android.net.Uri
import com.Crescent.DhikrCounter.data.AppDatabase
import com.Crescent.DhikrCounter.data.SessionEntity
import com.Crescent.DhikrCounter.data.HistoryEntity
import com.Crescent.DhikrCounter.data.AchievementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object BackupManagerUtil {

    suspend fun exportBackup(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val settingsManager = SettingsManager(context)
            
            val exportJson = JSONObject()
            
            // Export Sessions
            val sessionsJson = JSONArray()
            db.sessionDao().getAllSessions().forEach { session ->
                val sObj = JSONObject()
                sObj.put("id", session.id)
                sObj.put("name", session.name)
                sObj.put("count", session.count)
                sObj.put("incrementValue", session.incrementValue)
                sObj.put("decrementValue", session.decrementValue)
                sObj.put("createdAt", session.createdAt)
                sObj.put("modifiedAt", session.modifiedAt)
                sObj.put("listOrder", session.listOrder)
                sObj.put("goalCount", session.goalCount)
                sObj.put("goalName", session.goalName)
                sObj.put("description", session.description)
                sObj.put("colorHex", session.colorHex)
                sObj.put("category", session.category)
                sObj.put("notes", session.notes)
                sObj.put("tags", session.tags)
                sObj.put("lastUsedAt", session.lastUsedAt)
                sessionsJson.put(sObj)
            }
            exportJson.put("sessions", sessionsJson)

            // Export History
            val historyJson = JSONArray()
            db.historyDao().getAllHistory().forEach { history ->
                val hObj = JSONObject()
                hObj.put("id", history.id)
                hObj.put("sessionId", history.sessionId)
                hObj.put("sessionName", history.sessionName)
                hObj.put("eventType", history.eventType)
                hObj.put("countChange", history.countChange)
                hObj.put("timestamp", history.timestamp)
                historyJson.put(hObj)
            }
            exportJson.put("history", historyJson)
            
            // Export Achievements
            val achievementsJson = JSONArray()
            db.achievementDao().getAllAchievements().forEach { achievement ->
                val aObj = JSONObject()
                aObj.put("id", achievement.id)
                aObj.put("achievementId", achievement.achievementId)
                aObj.put("name", achievement.name)
                aObj.put("description", achievement.description)
                aObj.put("isUnlocked", achievement.isUnlocked)
                aObj.put("unlockedAt", achievement.unlockedAt)
                aObj.put("progress", achievement.progress.toDouble())
                achievementsJson.put(aObj)
            }
            exportJson.put("achievements", achievementsJson)
            
            // Export Settings
            val settingsJson = JSONObject()
            val allPrefs = settingsManager.prefs.all
            for ((key, value) in allPrefs) {
                if (value is Boolean || value is Float || value is Int || value is Long || value is String) {
                    settingsJson.put(key, value)
                }
            }
            exportJson.put("settings", settingsJson)
            
            // Write to file
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(exportJson.toString().toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackup(context: Context, uri: Uri, isReplace: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return@withContext false
            
            val exportJson = JSONObject(jsonString)
            
            val db = AppDatabase.getDatabase(context)
            val settingsManager = SettingsManager(context)
            
            // If replace, clear all data first
            if (isReplace) {
                db.sessionDao().clearAllSessions()
                db.historyDao().clearAllHistory()
                db.achievementDao().clearAllAchievements()
            }
            
            val oldToNewSessionIdMap = mutableMapOf<Long, Long>()
            
            // Restore Sessions
            if (exportJson.has("sessions")) {
                val sessionsJson = exportJson.getJSONArray("sessions")
                for (i in 0 until sessionsJson.length()) {
                    val sObj = sessionsJson.getJSONObject(i)
                    val oldId = sObj.optLong("id", 0)
                    val session = SessionEntity(
                        id = if (isReplace) oldId else 0,
                        name = sObj.optString("name", "Restored Counter"),
                        count = sObj.optLong("count", 0),
                        incrementValue = sObj.optLong("incrementValue", 1),
                        decrementValue = sObj.optLong("decrementValue", 1),
                        createdAt = sObj.optLong("createdAt", System.currentTimeMillis()),
                        modifiedAt = sObj.optLong("modifiedAt", System.currentTimeMillis()),
                        listOrder = sObj.optInt("listOrder", 0),
                        goalCount = sObj.optLong("goalCount", 0),
                        goalName = sObj.optString("goalName", ""),
                        description = sObj.optString("description", ""),
                        colorHex = sObj.optString("colorHex", "#6200EE"),
                        category = sObj.optString("category", "General"),
                        notes = sObj.optString("notes", ""),
                        tags = sObj.optString("tags", ""),
                        lastUsedAt = sObj.optLong("lastUsedAt", 0)
                    )
                    val newId = db.sessionDao().insert(session)
                    oldToNewSessionIdMap[oldId] = newId
                }
            }
            
            // Restore History
            if (exportJson.has("history")) {
                val historyJson = exportJson.getJSONArray("history")
                for (i in 0 until historyJson.length()) {
                    val hObj = historyJson.getJSONObject(i)
                    val oldSessionId = hObj.optLong("sessionId", 0)
                    val newSessionId = if (isReplace) oldSessionId else (oldToNewSessionIdMap[oldSessionId] ?: oldSessionId)
                    
                    val history = HistoryEntity(
                        id = if (isReplace) hObj.optLong("id", 0) else 0,
                        sessionId = newSessionId,
                        sessionName = hObj.optString("sessionName", ""),
                        eventType = hObj.optString("eventType", ""),
                        countChange = hObj.optLong("countChange", 0),
                        timestamp = hObj.optLong("timestamp", System.currentTimeMillis())
                    )
                    db.historyDao().insert(history)
                }
            }
            
            // Restore Achievements
            if (exportJson.has("achievements")) {
                val achievementsJson = exportJson.getJSONArray("achievements")
                for (i in 0 until achievementsJson.length()) {
                    val aObj = achievementsJson.getJSONObject(i)
                    val achievement = AchievementEntity(
                        id = if (isReplace) aObj.optLong("id", 0) else 0,
                        achievementId = aObj.optString("achievementId", ""),
                        name = aObj.optString("name", ""),
                        description = aObj.optString("description", ""),
                        isUnlocked = aObj.optBoolean("isUnlocked", false),
                        unlockedAt = aObj.optLong("unlockedAt", 0),
                        progress = aObj.optDouble("progress", 0.0).toFloat()
                    )
                    db.achievementDao().insert(achievement)
                }
            }
            
            // Restore Settings
            if (exportJson.has("settings")) {
                val settingsJson = exportJson.getJSONObject("settings")
                val editor = settingsManager.prefs.edit()
                val keys = settingsJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = settingsJson.get(key)
                    when (value) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is String -> editor.putString(key, value)
                        is Double -> editor.putFloat(key, value.toFloat())
                    }
                }
                editor.apply()
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
