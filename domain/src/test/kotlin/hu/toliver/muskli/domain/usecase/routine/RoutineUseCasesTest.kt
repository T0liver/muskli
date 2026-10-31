package hu.toliver.muskli.domain.usecase.routine

import hu.toliver.muskli.domain.TestClock
import hu.toliver.muskli.domain.model.PlannedSet
import hu.toliver.muskli.domain.model.Routine
import hu.toliver.muskli.domain.model.RoutineExercise
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Side
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.usecase.workout.FakeWorkoutRepository
import hu.toliver.muskli.domain.usecase.workout.StartWorkoutResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class RoutineUseCasesTest {
    private val routines = FakeRoutineRepository()
    private val workouts = FakeWorkoutRepository()
    private val clock = TestClock(now = 5_000_000L, offsetS = 3_600)
    private var nextId = 0
    private val ids = IdGenerator { "id${nextId++}" }

    private val create = CreateRoutineUseCase(routines, ids, clock)
    private val update = UpdateRoutineUseCase(routines, clock)
    private val delete = DeleteRoutineUseCase(routines)
    private val observe = ObserveRoutinesUseCase(routines)
    private val start = StartWorkoutFromRoutineUseCase(routines, workouts, ids, clock)
    private val saveAsRoutine = SaveWorkoutAsRoutineUseCase(workouts, routines, ids, clock)

    private val bench = RoutineExercise(
        exerciseTemplateId = "bench",
        supersetGroup = 1,
        notes = "pause at the chest",
        restSeconds = 120,
        sets = listOf(
            PlannedSet(SetType.WARMUP, weightKg = 40.0, reps = 10),
            PlannedSet(weightKg = 80.0, reps = 5),
        ),
    )
    private val plank = RoutineExercise(exerciseTemplateId = "plank", sets = listOf(PlannedSet(durationS = 60)))

    private fun routine(id: String = "r1", title: String = "Push A", exercises: List<RoutineExercise> = listOf(bench, plank)) =
        Routine(id, title, createdAtEpochMs = 1_000L, updatedAtEpochMs = 1_000L, exercises = exercises)

    private fun workout(id: String, finishedAt: Long?, deletedAt: Long? = null, exercises: List<WorkoutExercise> = emptyList()) = Workout(
        id = id,
        title = "Monday",
        notes = "good session",
        startedAtEpochMs = 1_000_000L,
        utcOffsetS = 0,
        finishedAtEpochMs = finishedAt,
        createdAtEpochMs = 1_000_000L,
        updatedAtEpochMs = 1_000_000L,
        deletedAtEpochMs = deletedAt,
        exercises = exercises,
    )

    @Test
    fun `TC-ROUTINE-01 create trims the title and stamps both times`() = runTest {
        val created = create("  Push A ", listOf(bench, plank))

        assertEquals(Routine("id0", "Push A", 5_000_000L, 5_000_000L, listOf(bench, plank)), created)
        assertEquals(created, routines.get("id0"))
    }

    @Test
    fun `TC-ROUTINE-01 a blank title is rejected and nothing is written`() = runTest {
        assertThrows<IllegalArgumentException> { create("   ", listOf(bench)) }
        assertEquals(emptyMap(), routines.routines.value)
    }

    @Test
    fun `TC-ROUTINE-02 update replaces title and exercises and keeps the creation time`() = runTest {
        routines.save(routine())

        val updated = update("r1", " Push B ", listOf(plank))

        assertEquals(Routine("r1", "Push B", 1_000L, 5_000_000L, listOf(plank)), updated)
        assertEquals(updated, routines.get("r1"))
    }

    @Test
    fun `TC-ROUTINE-02 updating an unknown routine returns null and a blank title is rejected`() = runTest {
        routines.save(routine())

        assertNull(update("missing", "Push", listOf(bench)))
        assertThrows<IllegalArgumentException> { update("r1", "", listOf(bench)) }
        assertEquals(routine(), routines.get("r1"))
    }

    @Test
    fun `TC-ROUTINE-03 routines are listed by title ignoring case, and delete removes one`() = runTest {
        routines.save(routine("r1", "pull"))
        routines.save(routine("r2", "Legs"))
        routines.save(routine("r3", "Push"))

        assertEquals(listOf("r2", "r1", "r3"), observe().first().map { it.id })

        delete("r1")

        assertEquals(listOf("r2", "r3"), observe().first().map { it.id })
    }

    @Test
    fun `TC-ROUTINE-04 starting from a routine creates a draft with its exercises and uncompleted planned sets`() = runTest {
        routines.save(routine())

        val result = start("r1")

        val draft = assertIs<StartWorkoutResult.Started>(result).workout
        val expected = Workout(
            id = "id0",
            title = "Push A",
            notes = null,
            startedAtEpochMs = 5_000_000L,
            utcOffsetS = 3_600,
            finishedAtEpochMs = null,
            createdAtEpochMs = 5_000_000L,
            updatedAtEpochMs = 5_000_000L,
            deletedAtEpochMs = null,
            exercises = listOf(
                WorkoutExercise(
                    id = "id1",
                    exerciseTemplateId = "bench",
                    supersetGroup = 1,
                    notes = "pause at the chest",
                    restSeconds = 120,
                    sets = listOf(
                        WorkoutSet("id2", SetData(SetType.WARMUP, weightKg = 40.0, reps = 10), completedAtEpochMs = null),
                        WorkoutSet("id3", SetData(SetType.NORMAL, weightKg = 80.0, reps = 5), completedAtEpochMs = null),
                    ),
                ),
                WorkoutExercise(
                    id = "id4",
                    exerciseTemplateId = "plank",
                    supersetGroup = null,
                    notes = null,
                    restSeconds = null,
                    sets = listOf(WorkoutSet("id5", SetData(durationS = 60), completedAtEpochMs = null)),
                ),
            ),
            restTimer = null,
            routineId = "r1",
        )
        assertEquals(expected, draft)
        assertEquals(expected, workouts.observeActiveDraft().first())
    }

    @Test
    fun `TC-ROUTINE-04 a planned exercise without sets starts with one empty normal set`() = runTest {
        routines.save(routine(exercises = listOf(RoutineExercise(exerciseTemplateId = "bench", sets = emptyList()))))

        val draft = (start("r1") as StartWorkoutResult.Started).workout

        assertEquals(listOf(SetData()), draft.exercises.single().sets.map { it.data })
    }

    @Test
    fun `TC-ROUTINE-05 starting from a routine while a draft exists returns that draft and writes nothing`() = runTest {
        routines.save(routine())
        val existing = workout("live", finishedAt = null)
        workouts.save(existing)
        val writes = workouts.writes

        val result = start("r1")

        assertEquals(StartWorkoutResult.Resumed(existing), result)
        assertEquals(writes, workouts.writes)
        assertEquals(listOf("live"), workouts.workouts.value.keys.toList())
    }

    @Test
    fun `TC-ROUTINE-05 starting from an unknown routine returns null and writes nothing`() = runTest {
        assertNull(start("missing"))
        assertEquals(0, workouts.writes)
    }

    @Test
    fun `TC-ROUTINE-06 a finished workout is saved as a routine with its exercises, types, and values`() = runTest {
        val finished = workout(
            "w1",
            finishedAt = 2_000_000L,
            exercises = listOf(
                WorkoutExercise(
                    id = "e1",
                    exerciseTemplateId = "bench",
                    supersetGroup = 1,
                    notes = "pause at the chest",
                    restSeconds = 120,
                    sets = listOf(
                        WorkoutSet("s1", SetData(SetType.WARMUP, weightKg = 40.0, reps = 10, completed = true), 1_100_000L),
                        WorkoutSet("s2", SetData(weightKg = 80.0, reps = 5, completed = true, side = Side.LEFT), 1_200_000L),
                    ),
                ),
                WorkoutExercise(
                    id = "e2",
                    exerciseTemplateId = "plank",
                    supersetGroup = null,
                    notes = null,
                    restSeconds = null,
                    sets = listOf(WorkoutSet("s3", SetData(durationS = 60, completed = true), 1_300_000L)),
                ),
            ),
        )
        workouts.save(finished)

        val saved = saveAsRoutine("w1", " Push A ")

        assertEquals(Routine("id0", "Push A", 5_000_000L, 5_000_000L, listOf(bench, plank)), saved)
        assertEquals(saved, routines.get("id0"))
        assertEquals(finished, workouts.observe("w1").first())
    }

    @Test
    fun `TC-ROUTINE-06 a draft, deleted, or unknown workout is not saved as a routine`() = runTest {
        workouts.save(workout("draft", finishedAt = null))
        workouts.save(workout("deleted", finishedAt = 2_000_000L, deletedAt = 3_000_000L))

        assertNull(saveAsRoutine("draft", "Push"))
        assertNull(saveAsRoutine("deleted", "Push"))
        assertNull(saveAsRoutine("missing", "Push"))
        assertEquals(emptyMap(), routines.routines.value)
    }

    @Test
    fun `TC-ROUTINE-06 a blank title is rejected when saving a workout as a routine`() = runTest {
        workouts.save(workout("w1", finishedAt = 2_000_000L))

        assertThrows<IllegalArgumentException> { saveAsRoutine("w1", " ") }
        assertEquals(emptyMap(), routines.routines.value)
    }
}
