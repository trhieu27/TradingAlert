package com.tradingalert.receiver.messaging

import android.content.Context

data class AlertPreferences(
    val volumePercent: Int,
    val soundDurationSeconds: Int,
)

object AlertPreferenceStore {
    const val DEFAULT_VOLUME_PERCENT = 80
    const val DEFAULT_SOUND_DURATION_SECONDS = 5
    const val MIN_SOUND_DURATION_SECONDS = 1
    const val MAX_SOUND_DURATION_SECONDS = 30
    const val KEY_VOLUME_PERCENT = "alert_volume_percent"
    const val KEY_SOUND_DURATION_SECONDS = "alert_sound_duration_seconds"

    fun load(context: Context): AlertPreferences {
        val preferences = context.getSharedPreferences(
            AlertMessagingService.PREFS_NAME,
            Context.MODE_PRIVATE,
        )
        return AlertPreferences(
            volumePercent = preferences.getInt(
                KEY_VOLUME_PERCENT,
                DEFAULT_VOLUME_PERCENT,
            ).coerceIn(0, 100),
            soundDurationSeconds = preferences.getInt(
                KEY_SOUND_DURATION_SECONDS,
                DEFAULT_SOUND_DURATION_SECONDS,
            ).coerceIn(MIN_SOUND_DURATION_SECONDS, MAX_SOUND_DURATION_SECONDS),
        )
    }

    fun setVolumePercent(context: Context, value: Int) {
        preferences(context).edit()
            .putInt(KEY_VOLUME_PERCENT, value.coerceIn(0, 100))
            .apply()
    }

    fun setSoundDurationSeconds(context: Context, value: Int) {
        preferences(context).edit()
            .putInt(
                KEY_SOUND_DURATION_SECONDS,
                value.coerceIn(MIN_SOUND_DURATION_SECONDS, MAX_SOUND_DURATION_SECONDS),
            )
            .apply()
    }

    private fun preferences(context: Context) = context.getSharedPreferences(
        AlertMessagingService.PREFS_NAME,
        Context.MODE_PRIVATE,
    )
}
