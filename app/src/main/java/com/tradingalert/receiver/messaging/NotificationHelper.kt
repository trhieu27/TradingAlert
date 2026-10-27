package com.tradingalert.receiver.messaging

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tradingalert.receiver.R
import com.tradingalert.receiver.ui.alertreceiver.AlertReceiverActivity

object NotificationHelper {
    const val CHANNEL_ID = "trading_alerts_v3"
    private const val PREVIOUS_CHANNEL_ID = "trading_alerts_v2"
    private const val LEGACY_CHANNEL_ID = "firebase_alerts"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activePlayer: MediaPlayer? = null
    private var stopSound: Runnable? = null

    fun soundUri(context: Context): Uri = Uri.parse(
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.trading_alert_chime}",
    )

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        notificationManager.deleteNotificationChannel(PREVIOUS_CHANNEL_ID)

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 180, 100, 180)
            setSound(null, null)
        }

        notificationManager.createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        message: PushMessage,
        notificationId: Int = nextId(),
        image: Bitmap? = null,
        playSound: Boolean = true,
    ): Boolean {
        val notificationManager = NotificationManagerCompat.from(context)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!notificationManager.areNotificationsEnabled()) return false
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
        ) {
            return false
        }

        val openApp = Intent(context, AlertReceiverActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_reference", message.reference)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alertPreferences = AlertPreferenceStore.load(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .apply {
                if (image == null) {
                    setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
                } else {
                    setLargeIcon(image)
                    setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(image)
                            .setBigContentTitle(message.title)
                            .setSummaryText(message.body),
                    )
                }
            }
            .build()

        notificationManager.notify(notificationId, notification)
        if (playSound) {
            playAlertSound(
                context = context,
                volumePercent = alertPreferences.volumePercent,
                durationSeconds = alertPreferences.soundDurationSeconds,
            )
        }
        return true
    }

    private fun playAlertSound(
        context: Context,
        volumePercent: Int,
        durationSeconds: Int,
    ) {
        mainHandler.post {
            stopActiveSound()
            if (volumePercent <= 0) return@post
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val player = MediaPlayer.create(
                context.applicationContext,
                R.raw.trading_alert_chime,
                audioAttributes,
                0,
            ) ?: return@post
            val volume = volumePercent.coerceIn(0, 100) / 100f
            player.setVolume(volume, volume)
            player.isLooping = true
            player.setOnErrorListener { mediaPlayer, _, _ ->
                if (activePlayer === mediaPlayer) activePlayer = null
                mediaPlayer.release()
                true
            }
            activePlayer = player
            player.start()

            val stopTask = Runnable { stopActiveSound() }
            stopSound = stopTask
            mainHandler.postDelayed(
                stopTask,
                durationSeconds.coerceIn(
                    AlertPreferenceStore.MIN_SOUND_DURATION_SECONDS,
                    AlertPreferenceStore.MAX_SOUND_DURATION_SECONDS,
                ) * 1_000L,
            )
        }
    }

    private fun stopActiveSound() {
        stopSound?.let(mainHandler::removeCallbacks)
        stopSound = null
        activePlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
            }
            player.release()
        }
        activePlayer = null
    }

    internal fun nextId(): Int = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
}
