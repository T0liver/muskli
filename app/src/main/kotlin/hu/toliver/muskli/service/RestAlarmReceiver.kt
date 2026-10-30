package hu.toliver.muskli.service

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** The `setAlarmClock` backup: alerts even when the process slept through the end or was killed (T-009). */
@AndroidEntryPoint
class RestAlarmReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var restAlert: RestAlert

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val workoutId = intent.getStringExtra(EXTRA_WORKOUT_ID) ?: return
        val endsAt = intent.getLongExtra(EXTRA_ENDS_AT, -1L).takeIf { it >= 0 } ?: return
        goAsyncWork { restAlert.fire(workoutId, endsAt) }
    }

    companion object {
        const val EXTRA_WORKOUT_ID = "workoutId"
        const val EXTRA_ENDS_AT = "endsAt"
    }
}
