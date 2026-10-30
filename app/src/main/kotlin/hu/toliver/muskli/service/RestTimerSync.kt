package hu.toliver.muskli.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.toliver.muskli.domain.calc.remainingMs
import hu.toliver.muskli.domain.model.RestTimer
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.WorkoutRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

sealed interface RestSync {
    data class Arm(val workoutId: String, val timer: RestTimer) : RestSync

    data object Disarm : RestSync

    /** The rest is over but still stored: its alarm or service is firing now, or the app was force-stopped meanwhile. */
    data object Leave : RestSync
}

/** What the alarm and service should do for the draft's stored rest at [nowEpochMs]. */
fun restSync(workoutId: String?, timer: RestTimer?, nowEpochMs: Long): RestSync = when {
    workoutId == null || timer == null -> RestSync.Disarm
    timer.remainingMs(nowEpochMs) > 0 -> RestSync.Arm(workoutId, timer)
    else -> RestSync.Leave
}

/**
 * Keeps the alarm and the service in step with the rest stored in the draft, whoever changed it:
 * the live screen, a notification action, or a relaunch after the process died (AC-03). An
 * over rest is left alone rather than cancelled, so a firing alarm still gets to alert.
 */
class RestTimerSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workouts: WorkoutRepository,
    private val clock: Clock,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    suspend fun run() {
        workouts.observeActiveDraft()
            .map { it?.id to it?.restTimer }
            .distinctUntilChanged()
            .collect { (workoutId, timer) ->
                when (val action = restSync(workoutId, timer, clock.nowEpochMs())) {
                    is RestSync.Arm -> arm(action.workoutId, action.timer)
                    RestSync.Disarm -> alarms.cancel(alarmIntent(workoutId = null, endsAt = -1L))
                    RestSync.Leave -> Unit
                }
            }
    }

    private fun arm(workoutId: String, timer: RestTimer) {
        val alarm = alarmIntent(workoutId, timer.endsAtEpochMs)
        // USE_EXACT_ALARM grants this on Android 13+; the fallback only matters if the permission is revoked on 12.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timer.endsAtEpochMs, alarm)
        } else {
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(timer.endsAtEpochMs, RestNotifications.openApp(context)), alarm)
        }
        RestNotifications.cancelAlert(context)
        val service = RestTimerService.intent(context, workoutId, timer)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service) else context.startService(service)
        } catch (e: IllegalStateException) {
            // Not allowed from the background (Android 12+); the alarm still alerts.
            Log.w(TAG, "Rest timer service not started", e)
        }
    }

    // One alarm at a time: the same request code and component, so setting replaces and cancel matches.
    private fun alarmIntent(workoutId: String?, endsAt: Long): PendingIntent {
        val intent = Intent(context, RestAlarmReceiver::class.java)
            .putExtra(RestAlarmReceiver.EXTRA_WORKOUT_ID, workoutId)
            .putExtra(RestAlarmReceiver.EXTRA_ENDS_AT, endsAt)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private companion object {
        const val TAG = "RestTimerSync"
    }
}
