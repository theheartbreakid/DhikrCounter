package com.Crescent.DhikrCounter.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.Crescent.DhikrCounter.DhikrApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CounterTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val app = application as DhikrApplication
        val activeSessionId = app.settingsManager.activeSessionId
        if (activeSessionId != -1L) {
            CoroutineScope(Dispatchers.IO).launch {
                val session = app.sessionRepository.getSession(activeSessionId)
                if (session != null) {
                    app.sessionRepository.incrementCount(session.id, session.incrementValue)
                    app.historyRepository.logEvent(session.id, session.name, "COUNT_CHANGED_TILE", session.incrementValue)
                }
            }
        }
        val tile = qsTile
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }
    
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile
        if (tile != null) {
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }
}
