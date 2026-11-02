package com.siddhant.focusguard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

object AudioFocusHelper {

    private var activeFocusRequest: AudioFocusRequest? = null

    fun requestExclusiveFocus(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setOnAudioFocusChangeListener { /* maintain focus suppression */ }
                    .build()
                activeFocusRequest = focusRequest
                val result = audioManager.requestAudioFocus(focusRequest)
                Log.d(Config.LOG_TAG, "Requested audio focus, result=$result")
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
                Log.d(Config.LOG_TAG, "Requested legacy audio focus, result=$result")
            }
        } catch (e: Exception) {
            Log.w(Config.LOG_TAG, "Could not request audio focus", e)
        }
    }

    fun releaseFocus(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activeFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                activeFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            Log.d(Config.LOG_TAG, "Abandoned audio focus")
        } catch (e: Exception) {
            Log.w(Config.LOG_TAG, "Could not abandon audio focus", e)
        }
    }
}
