package hu.toliver.muskli.service

import android.content.BroadcastReceiver
import android.content.Context
import androidx.annotation.CallSuper
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import hu.toliver.muskli.domain.usecase.workout.AdjustRestTimerUseCase
import hu.toliver.muskli.domain.usecase.workout.SkipRestTimerUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The countdown notification's −15 s, +15 s, and Skip; same use cases as the in-app bar. */
@AndroidEntryPoint
class RestActionReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var adjustRest: AdjustRestTimerUseCase

    @Inject lateinit var skipRest: SkipRestTimerUseCase

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val workoutId = intent.getStringExtra(EXTRA_WORKOUT_ID) ?: return
        when (intent.action) {
            ACTION_MINUS_15 -> goAsyncWork { adjustRest(workoutId, -STEP_S) }
            ACTION_PLUS_15 -> goAsyncWork { adjustRest(workoutId, STEP_S) }
            ACTION_SKIP -> goAsyncWork { skipRest(workoutId) }
        }
    }

    companion object {
        const val ACTION_MINUS_15 = "hu.toliver.muskli.rest.MINUS_15"
        const val ACTION_PLUS_15 = "hu.toliver.muskli.rest.PLUS_15"
        const val ACTION_SKIP = "hu.toliver.muskli.rest.SKIP"
        const val EXTRA_WORKOUT_ID = "workoutId"
        private const val STEP_S = 15
    }
}

/**
 * Hilt injects in the generated `onReceive`, which the subclass must reach through `super`; Kotlin
 * cannot call the abstract platform method directly, so this gives it a body.
 */
abstract class HiltBroadcastReceiver : BroadcastReceiver() {
    @CallSuper
    override fun onReceive(context: Context, intent: Intent) = Unit
}

/** Runs [work] past `onReceive` without letting the system kill the process mid-write. */
internal fun BroadcastReceiver.goAsyncWork(work: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            work()
        } finally {
            pending.finish()
        }
    }
}
