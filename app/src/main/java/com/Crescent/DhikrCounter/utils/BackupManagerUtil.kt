package com.Crescent.DhikrCounter.utils

import android.content.Context
import android.net.Uri
import com.Crescent.DhikrCounter.data.AppDatabase
import com.Crescent.DhikrCounter.data.SessionEntity
import com.Crescent.DhikrCounter.data.HistoryEntity
import com.Crescent.DhikrCounter.data.AchievementEntity
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object BackupManagerUtil {
    private const val SCHEMA_VERSION = 1

    suspend fun exportBackup(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val settingsManager = SettingsManager(context)
            
            val exportJson = JSONObject()
            exportJson.put("schemaVersion", SCHEMA_VERSION)
            exportJson.put("exportedAt", System.currentTimeMillis())

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
                hObj.put("duration", history.duration)
                hObj.put("isGoalMet", history.isGoalMet)
                hObj.put("startCount", history.startCount)
                hObj.put("endCount", history.endCount)
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

            settingsManager.prefs.edit().putLong("last_backup_timestamp", System.currentTimeMillis()).apply()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun validateBackup(context: Context, uri: Uri): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return@withContext false to "Could not read file."
            
            if (jsonString.isBlank()) return@withContext false to "File is empty."
            val exportJson = JSONObject(jsonString)
            
            if (!exportJson.has("schemaVersion")) return@withContext false to "Invalid backup file (missing schema version)."
            val version = exportJson.getInt("schemaVersion")
            if (version > SCHEMA_VERSION) {
                return@withContext false to "Backup from a newer version of the app. Please update."
            }
            
            if (!exportJson.has("sessions")) return@withContext false to "Invalid backup file (missing data)."
            
            true to null
        } catch (e: Exception) {
            e.printStackTrace()
            false to "Invalid JSON format or corrupted file."
        }
    }

    suspend fun restoreBackup(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return@withContext false
            
            if (jsonString.isBlank()) return@withContext false
            val exportJson = JSONObject(jsonString)
            
            // Schema Validation
            if (!exportJson.has("schemaVersion")) return@withContext false
            val version = exportJson.getInt("schemaVersion")
            if (version > SCHEMA_VERSION) {
                // Future version - reject for now or implement migration
                return@withContext false
            }
            
            if (!exportJson.has("sessions")) return@withContext false
            
            db.withTransaction {
                val settingsManager = SettingsManager(context)
                
                // Backup/Restore is all-or-nothing overwrite
                db.sessionDao().clearAllSessions()
                db.historyDao().clearAllHistory()
                db.achievementDao().clearAllAchievements()
                
                // Restore Sessions
                if (exportJson.has("sessions")) {
                    val sessionsJson = exportJson.getJSONArray("sessions")
                    for (i in 0 until sessionsJson.length()) {
                        val sObj = sessionsJson.getJSONObject(i)
                        val id = sObj.optLong("id", 0)
                        
                        val name = sObj.optString("name", "Restored Counter")
                        if (name.isBlank()) continue

                        val session = SessionEntity(
                            id = id,
                            name = name,
                            count = sObj.optLong("count", 0).coerceAtLeast(0),
                            incrementValue = sObj.optLong("incrementValue", 1).coerceAtLeast(1),
                            decrementValue = sObj.optLong("decrementValue", 1).coerceAtLeast(1),
                            createdAt = sObj.optLong("createdAt", System.currentTimeMillis()),
                            modifiedAt = sObj.optLong("modifiedAt", System.currentTimeMillis()),
                            listOrder = sObj.optInt("listOrder", 0),
                            goalCount = sObj.optLong("goalCount", 0).coerceAtLeast(0),
                            goalName = sObj.optString("goalName", ""),
                            description = sObj.optString("description", ""),
                            colorHex = sObj.optString("colorHex", "#6200EE"),
                            category = sObj.optString("category", "General"),
                            notes = sObj.optString("notes", ""),
                            tags = sObj.optString("tags", ""),
                            lastUsedAt = sObj.optLong("lastUsedAt", 0)
                        )
                        db.sessionDao().insert(session)
                    }
                }
                
                // Restore History
                if (exportJson.has("history")) {
                    val historyJson = exportJson.getJSONArray("history")
                    for (i in 0 until historyJson.length()) {
                        val hObj = historyJson.getJSONObject(i)
                        val history = HistoryEntity(
                            id = hObj.optLong("id", 0),
                            sessionId = hObj.optLong("sessionId", 0),
                            sessionName = hObj.optString("sessionName", ""),
                            eventType = hObj.optString("eventType", ""),
                            countChange = hObj.optLong("countChange", 0),
                            timestamp = hObj.optLong("timestamp", System.currentTimeMillis()),
                            duration = hObj.optLong("duration", 0),
                            isGoalMet = hObj.optBoolean("isGoalMet", false),
                            startCount = hObj.optLong("startCount", 0),
                            endCount = hObj.optLong("endCount", 0)
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
                            id = aObj.optLong("id", 0),
                            achievementId = aObj.optString("achievementId", ""),
                            name = aObj.optString("name", ""),
                            description = aObj.optString("description", ""),
                            isUnlocked = aObj.optBoolean("isUnlocked", false),
                            unlockedAt = aObj.optLong("unlockedAt", 0),
                            progress = aObj.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f)
                        )
                        db.achievementDao().insert(achievement)
                    }
                }
                
                // Restore Settings
                if (exportJson.has("settings")) {
                    val settingsJson = exportJson.getJSONObject("settings")
                    val editor = settingsManager.prefs.edit()
                    
                    // All-or-nothing: Clear existing and rebuild from backup
                    editor.clear()
                    
                    val keys = settingsJson.keys()

                    // Type mapping for disambiguation when types are lost in JSON
                    val longKeys = setOf("active_session_id", "last_backup_timestamp")
                    val intKeys = setOf(
                        "pref_wavy_color", "pref_wavy_track_color", "pref_glass_tint_color",
                        "pref_glass_shadow_color", "pref_adaptive_luminance_interval",
                        "pref_font_tint_fallback_mode", "pref_font_tint_palette_color",
                        "pref_font_tint_custom_color", "pref_font_tint_auto_color",
                        "pref_bubble_accent_color"
                    )

                    while (keys.hasNext()) {
                        val key = keys.next()
                        // Skip internal flags that should be managed by current app state
                        if (key == "is_first_launch_overlay" || key == "is_first_launch") continue
                        
                        val value = settingsJson.get(key)
                        if (value == JSONObject.NULL) continue

                        try {
                            when {
                                key in longKeys -> editor.putLong(key, settingsJson.optLong(key))
                                key in intKeys -> editor.putInt(key, settingsJson.optInt(key))
                                value is Boolean -> editor.putBoolean(key, value)
                                value is String -> editor.putString(key, value)
                                value is Number -> {
                                    // Default to Float for all other numbers in this app (optics, scales, etc)
                                    editor.putFloat(key, settingsJson.optDouble(key).toFloat())
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    // Re-add essential current-session flags if they were cleared
                    editor.putBoolean("is_first_launch", false)
                    editor.putBoolean("is_first_launch_overlay", false)
                    editor.putLong("last_backup_timestamp", System.currentTimeMillis())
                    
                    editor.commit() // Synchronous write to ensure next reads are correct
                }
                
                // Final validation: Ensure at least one session exists and active_session_id is valid
                val allSessions = db.sessionDao().getAllSessions()
                if (allSessions.isEmpty()) {
                    val now = System.currentTimeMillis()
                    val defaultSession = SessionEntity(
                        name = "Counter 1",
                        count = 0,
                        incrementValue = 1,
                        decrementValue = 1,
                        createdAt = now,
                        modifiedAt = now,
                        listOrder = 0
                    )
                    val newId = db.sessionDao().insert(defaultSession)
                    settingsManager.setActiveSessionId(newId)
                } else {
                    val currentActive = settingsManager.activeSessionId
                    if (allSessions.none { it.id == currentActive }) {
                        settingsManager.setActiveSessionId(allSessions[0].id)
                    }
                }
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

