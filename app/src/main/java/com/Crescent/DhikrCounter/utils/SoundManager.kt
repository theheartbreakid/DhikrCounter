package com.Crescent.DhikrCounter.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.Crescent.DhikrCounter.R

class SoundManager(private val context: Context, private val settingsManager: SettingsManager) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val sounds = mutableMapOf<SoundType, Int>()

    enum class SoundType {
        INCREMENT, DECREMENT, RESET, GOAL_REACHED, FLOATING_POPUP, FLOATING_DISMISS
    }

    init {
        sounds[SoundType.INCREMENT] = soundPool.load(context, R.raw.increment, 1)
        sounds[SoundType.DECREMENT] = soundPool.load(context, R.raw.decrement, 1)
        sounds[SoundType.RESET] = soundPool.load(context, R.raw.reset, 1)
        sounds[SoundType.GOAL_REACHED] = soundPool.load(context, R.raw.goal_reached, 1)
        sounds[SoundType.FLOATING_POPUP] = soundPool.load(context, R.raw.floating_popup, 1)
        sounds[SoundType.FLOATING_DISMISS] = soundPool.load(context, R.raw.floating_dismiss, 1)
    }

    fun playSound(type: SoundType) {
        if (!settingsManager.isSoundFeedbackEnabled()) return
        
        // RINGER_MODE_NORMAL -> play sound
        // RINGER_MODE_VIBRATE -> no sound
        // RINGER_MODE_SILENT -> no sound
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        val soundId = sounds[type] ?: return
        val volume = settingsManager.getSoundVolume()
        soundPool.play(soundId, volume, volume, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
