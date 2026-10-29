package hu.toliver.muskli.domain.calc

import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType

sealed interface RestEffect {
    data class Start(val seconds: Int) : RestEffect
    data object None : RestEffect
}

/**
 * What completing [completed] does to the rest timer. [next] is the next set in workout order,
 * across exercises and supersets; null when [completed] was the last set of the workout (D-018).
 * [exerciseRestS] is the rest of the completed set's exercise (D-012); 0 means off for that
 * exercise, null falls back to [defaultRestS].
 */
fun restTimerEffect(
    completed: SetData,
    next: SetData?,
    exerciseRestS: Int?,
    defaultRestS: Int?,
): RestEffect {
    if (!completed.completed || next == null || next.type == SetType.DROPSET) return RestEffect.None
    val seconds = exerciseRestS ?: defaultRestS ?: return RestEffect.None
    return if (seconds > 0) RestEffect.Start(seconds) else RestEffect.None
}
