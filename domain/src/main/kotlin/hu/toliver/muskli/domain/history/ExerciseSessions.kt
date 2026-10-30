package hu.toliver.muskli.domain.history

import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.Workout

/** One workout in an exercise's history, with that exercise's completed sets. */
data class ExerciseSession(
    val workoutId: String,
    val workoutTitle: String,
    val startedAtEpochMs: Long,
    val utcOffsetS: Int,
    val sets: List<SetData>,
)

/**
 * The exercise history screen (F-06 AC4): finished, non-deleted workouts that contain
 * [exerciseTemplateId], newest start first. Sets of every occurrence in a workout are listed in order.
 */
fun exerciseSessions(workouts: List<Workout>, exerciseTemplateId: String): List<ExerciseSession> = workouts
    .filter { it.finishedAtEpochMs != null && it.deletedAtEpochMs == null }
    .sortedByDescending { it.startedAtEpochMs }
    .mapNotNull { workout ->
        val sets = workout.exercises
            .filter { it.exerciseTemplateId == exerciseTemplateId }
            .flatMap { exercise -> exercise.sets.map { it.data }.filter { it.completed } }
        if (sets.isEmpty()) null else ExerciseSession(workout.id, workout.title, workout.startedAtEpochMs, workout.utcOffsetS, sets)
    }
