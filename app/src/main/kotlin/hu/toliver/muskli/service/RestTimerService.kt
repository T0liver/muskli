package hu.toliver.muskli.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import dagger.hilt.android.AndroidEntryPoint
import hu.toliver.muskli.domain.model.RestTimer
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The `specialUse` foreground service half of D-015: shows the countdown and alerts on time while
 * the CPU is awake. It follows the stored rest and stops itself when the rest is gone, so callers
 * only ever start it. Its handler runs on uptime and stalls in deep sleep; the alarm covers that.
 */
@AndroidEntryPoint
class RestTimerService : Service() {
    @Inject lateinit var workouts: WorkoutRepository

    @Inject lateinit var restAlert: RestAlert

    @Inject lateinit var clock: Clock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var following: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val workoutId = intent?.getStringExtra(EXTRA_WORKOUT_ID)
        val endsAt = intent?.getLongExtra(EXTRA_ENDS_AT, -1L) ?: -1L
        val durationS = intent?.getIntExtra(EXTRA_DURATION_S, -1) ?: -1
        if (workoutId == null || endsAt < 0 || durationS < 0) {
            stop()
            return START_NOT_STICKY
        }
        // Must be called promptly after startForegroundService, before the draft is read.
        val notification = RestNotifications.countdown(this, workoutId, RestTimer(endsAt, durationS))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(RestNotifications.COUNTDOWN_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(RestNotifications.COUNTDOWN_ID, notification)
        }
        if (following == null) following = scope.launch { follow() }
        return START_NOT_STICKY
    }

    private suspend fun follow() {
        workouts.observeActiveDraft()
            .map { draft -> draft?.restTimer?.let { draft.id to it } }
            .distinctUntilChanged()
            .collect { rest ->
                handler.removeCallbacksAndMessages(null)
                if (rest == null) {
                    stop()
                    return@collect
                }
                val (workoutId, timer) = rest
                getSystemService(NotificationManager::class.java)
                    .notify(RestNotifications.COUNTDOWN_ID, RestNotifications.countdown(this, workoutId, timer))
                val delayMs = (timer.endsAtEpochMs - clock.nowEpochMs()).coerceAtLeast(0)
                handler.postDelayed({ scope.launch { restAlert.fire(workoutId, timer.endsAtEpochMs) } }, delayMs)
            }
    }

    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_WORKOUT_ID = "workoutId"
        private const val EXTRA_ENDS_AT = "endsAt"
        private const val EXTRA_DURATION_S = "durationS"

        fun intent(context: Context, workoutId: String, timer: RestTimer): Intent =
            Intent(context, RestTimerService::class.java)
                .putExtra(EXTRA_WORKOUT_ID, workoutId)
                .putExtra(EXTRA_ENDS_AT, timer.endsAtEpochMs)
                .putExtra(EXTRA_DURATION_S, timer.durationS)
    }
}
