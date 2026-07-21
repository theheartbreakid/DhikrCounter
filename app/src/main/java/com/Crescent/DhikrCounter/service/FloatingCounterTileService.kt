package com.Crescent.DhikrCounter.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.MainActivity

class FloatingCounterTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        val app = application as DhikrApplication
        val settingsManager = app.settingsManager

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        val isEnabled = settingsManager.isFloatingBubbleEnabled
        settingsManager.prefs.edit().putBoolean("pref_floating_enabled", !isEnabled).apply()
        
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val app = application as DhikrApplication
        val isEnabled = app.settingsManager.isFloatingBubbleEnabled
        
        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (isEnabled) "On" else "Off"
        tile.updateTile()
    }
}
