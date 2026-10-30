package hu.toliver.muskli.domain.history

import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExerciseSessionsTest {
    private fun set(weightKg: Double, completed: Boolean = true) =
        WorkoutSet("s$weightKg", SetData(weightKg = weightKg, reps = 5, completed = completed), completedAtEpochMs = null)

    private fun exercise(templateId: String, vararg sets: WorkoutSet) =
        WorkoutExercise("e-$templateId-${sets.size}", templateId, supersetGroup = null, notes = null, restSeconds = null, sets = sets.toList())

    private fun workout(
        id: String,
        startedAt: Long,
        vararg exercises: WorkoutExercise,
        finishedAt: Long? = startedAt + 1,
        deletedAt: Long? = null,
    ) = Workout(
        id = id,
        title = "title $id",
        notes = null,
        startedAtEpochMs = startedAt,
        utcOffsetS = 3_600,
        finishedAtEpochMs = finishedAt,
        createdAtEpochMs = startedAt,
        updatedAtEpochMs = startedAt,
        deletedAtEpochMs = deletedAt,
        exercises = exercises.toList(),
    )

    @Test
    fun `sessions are the finished, non-deleted workouts with the exercise, newest first`() {
        val workouts = listOf(
            workout("old", 100, exercise("bench", set(60.0))),
            workout("other", 300, exercise("row", set(50.0))),
            workout("new", 200, exercise("bench", set(70.0))),
            workout("draft", 400, exercise("bench", set(80.0)), finishedAt = null),
            workout("deleted", 500, exercise("bench", set(90.0)), deletedAt = 600),
        )

        val sessions = exerciseSessions(workouts, "bench")

        assertEquals(
            listOf(
                ExerciseSession("new", "title new", 200, 3_600, listOf(set(70.0).data)),
                ExerciseSession("old", "title old", 100, 3_600, listOf(set(60.0).data)),
            ),
            sessions,
        )
    }

    @Test
    fun `only completed sets are listed, from every occurrence in the workout`() {
        val workouts = listOf(
            workout("w", 100, exercise("bench", set(60.0), set(65.0, completed = false)), exercise("row", set(50.0)), exercise("bench", set(70.0))),
        )

        val sessions = exerciseSessions(workouts, "bench")

        assertEquals(listOf(set(60.0).data, set(70.0).data), sessions.single().sets)
    }

    @Test
    fun `no history gives no sessions`() {
        assertEquals(emptyList(), exerciseSessions(emptyList(), "bench"))
    }
}
