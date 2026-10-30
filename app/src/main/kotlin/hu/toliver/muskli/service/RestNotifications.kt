package hu.toliver.muskli.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import hu.toliver.muskli.MainActivity
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.model.RestTimer

/**
 * The two rest notifications: the ongoing countdown of [RestTimerService] and the alert at zero.
 * Platform APIs only, so no androidx.core dependency. A channel's sound and vibration are fixed
 * once created, so the alert has one channel per sound/vibration combination from Settings.
 */
object RestNotifications {
    const val COUNTDOWN_ID = 1
    private const val ALERT_ID = 2
    private const val COUNTDOWN_CHANNEL = "rest_countdown"

    private enum class AlertChannel(val id: String, val nameRes: Int, val sound: Boolean, val vibration: Boolean) {
        SOUND_AND_VIBRATION("rest_alert_sound_vibration", R.string.rest_channel_alert_sound_vibration, true, true),
        SOUND("rest_alert_sound", R.string.rest_channel_alert_sound, true, false),
        VIBRATION("rest_alert_vibration", R.string.rest_channel_alert_vibration, false, true),
        SILENT("rest_alert_silent", R.string.rest_channel_alert_silent, false, false),
    }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val countdown = NotificationChannel(
            COUNTDOWN_CHANNEL,
            context.getString(R.string.rest_channel_countdown),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }
        val alerts = AlertChannel.entries.map { channel ->
            NotificationChannel(channel.id, context.getString(channel.nameRes), NotificationManager.IMPORTANCE_HIGH).apply {
                if (!channel.sound) setSound(null, null)
                enableVibration(channel.vibration)
                if (channel.vibration) vibrationPattern = ALERT_VIBRATION
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        }
        manager.createNotificationChannels(listOf(countdown) + alerts)
    }

    /** Counts down on its own (a system chronometer), so it stays right while the process sleeps. */
    fun countdown(context: Context, workoutId: String, timer: RestTimer): Notification =
        builder(context, COUNTDOWN_CHANNEL)
            .setSmallIcon(R.drawable.ic_rest_timer)
            .setContentTitle(context.getString(R.string.rest_timer_label))
            .setWhen(timer.endsAtEpochMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(context))
            .addAction(action(context, workoutId, RestActionReceiver.ACTION_MINUS_15, R.string.rest_minus_15))
            .addAction(action(context, workoutId, RestActionReceiver.ACTION_PLUS_15, R.string.rest_plus_15))
            .addAction(action(context, workoutId, RestActionReceiver.ACTION_SKIP, R.string.rest_skip))
            .build()

    fun showAlert(context: Context, sound: Boolean, vibration: Boolean) {
        val channel = AlertChannel.entries.first { it.sound == sound && it.vibration == vibration }
        val alert = builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_rest_timer)
            .setContentTitle(context.getString(R.string.rest_over_title))
            .setContentText(context.getString(R.string.rest_over_text))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .applyPreOreoAlert(sound, vibration)
            .build()
        // Without POST_NOTIFICATIONS (Android 13+) this posts nothing; the app asks at workout start (D-015).
        context.getSystemService(NotificationManager::class.java).notify(ALERT_ID, alert)
    }

    fun cancelAlert(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(ALERT_ID)
    }

    /** Like tapping the launcher icon: brings the existing task forward instead of a second activity. */
    fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(context: Context, workoutId: String, action: String, titleRes: Int): Notification.Action {
        val intent = Intent(context, RestActionReceiver::class.java)
            .setAction(action)
            .putExtra(RestActionReceiver.EXTRA_WORKOUT_ID, workoutId)
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Action.Builder(null, context.getString(titleRes), pending).build()
    }

    @Suppress("DEPRECATION")
    private fun builder(context: Context, channelId: String): Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, channelId) else Notification.Builder(context)

    // Before channels, the notification itself carries sound, vibration, and heads-up priority.
    @Suppress("DEPRECATION")
    private fun Notification.Builder.applyPreOreoAlert(sound: Boolean, vibration: Boolean): Notification.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return this
        setPriority(Notification.PRIORITY_HIGH)
        setDefaults(if (sound) Notification.DEFAULT_SOUND else 0)
        setVibrate(if (vibration) ALERT_VIBRATION else null)
        return this
    }

    private val ALERT_VIBRATION = longArrayOf(0, 400, 200, 400)
}
