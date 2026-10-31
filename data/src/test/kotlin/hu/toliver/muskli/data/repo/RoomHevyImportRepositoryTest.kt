package hu.toliver.muskli.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.data.db.MuskliDatabase
import hu.toliver.muskli.data.db.benchPress
import hu.toliver.muskli.data.db.dumbbellRow
import hu.toliver.muskli.data.db.secondaryMuscleEntities
import hu.toliver.muskli.data.db.toDomain
import hu.toliver.muskli.data.db.toEntity
import hu.toliver.muskli.data.db.workout
import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.importer.hevy.HevyMatch
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeekStartDay
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.domain.usecase.importer.ApplyHevyImportUseCase
import hu.toliver.muskli.domain.usecase.importer.HevyPreview
import hu.toliver.muskli.domain.usecase.importer.PreviewHevyImportUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomHevyImportRepositoryTest {
    private val clock = object : Clock {
        override fun nowEpochMs() = 1_800_000_000_000L

        override fun utcOffsetS(epochMs: Long) = 3_600
    }
    private var nextId = 0
    private val ids = IdGenerator { "hevy-${nextId++}" }

    private val db: MuskliDatabase =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MuskliDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val repository = RoomHevyImportRepository(db)
    private val preview = PreviewHevyImportUseCase(repository, ids, clock)
    private val apply = ApplyHevyImportUseCase(repository, UnusedSettings, ids, clock)

    @After
    fun tearDown() = db.close()

    private val pushStart = 1773507900000L
    private val legsStart = 1773295200000L

    /** The catalogue exercises the fixture matches (docs/HEVY-IMPORT.md §8). */
    private val catalogue = listOf(
        catalogueTemplate("barbell-bench-press", "Barbell Bench Press", ExerciseType.WEIGHT_REPS),
        catalogueTemplate("pull-up", "Pull-Up", ExerciseType.BODYWEIGHT_REPS),
        catalogueTemplate("dumbbell-farmers-walk", "Dumbbell Farmer's Walk", ExerciseType.SHORT_DISTANCE_WEIGHT),
        catalogueTemplate("plank", "Plank", ExerciseType.DURATION),
        catalogueTemplate("rowing-machine", "Rowing Machine", ExerciseType.DISTANCE_DURATION),
    )

    private fun catalogueTemplate(id: String, title: String, type: ExerciseType) =
        ExerciseTemplate(id, title, type, MuscleGroup.OTHER, setOf(MuscleGroup.CARDIO), Equipment.NONE, isCustom = false, archived = false, 0L, 0L)

    private val fixtureText = requireNotNull(javaClass.getResource("/hevy/workout_data.csv")).readText()

    private suspend fun seed() {
        (catalogue + benchPress + dumbbellRow).forEach { db.exerciseTemplateDao().upsert(it.toEntity(), it.secondaryMuscleEntities()) }
        val workouts = RoomWorkoutRepository(db)
        workouts.save(workout("finished", startedAt = 10_000L, finishedAt = 20_000L))
        workouts.save(workout("draft", startedAt = 80_000L, finishedAt = null))
    }

    private suspend fun ready() = preview(fixtureText) as HevyPreview.Ready

    private suspend fun templatesById() = db.exerciseTemplateDao().getAll().associate { it.template.id to it.toDomain() }

    private suspend fun workoutsById() = db.backupDao().allWorkouts().associate { it.workout.id to it.toDomain() }

    @Test
    fun `TC-HEVY-20 apply adds the workouts and customs and leaves existing rows unchanged`() = runTest {
        seed()
        val workoutsBefore = workoutsById()
        val templatesBefore = templatesById()
        val ready = ready()

        val applied = apply(ready.workouts, ready.defaultResolutions, bodyWeightKg = null)

        assertEquals(2, applied.newWorkouts.size)
        assertEquals(3, applied.newTemplates.size)
        val workoutsAfter = workoutsById()
        assertEquals(workoutsBefore, workoutsAfter.filterKeys { it in workoutsBefore })
        assertEquals(applied.newWorkouts, applied.newWorkouts.map { workoutsAfter.getValue(it.id) })
        assertEquals(4, workoutsAfter.size)
        val templatesAfter = templatesById()
        assertEquals(templatesBefore, templatesAfter.filterKeys { it in templatesBefore })
        assertEquals(applied.newTemplates, applied.newTemplates.map { templatesAfter.getValue(it.id) })
        assertEquals(templatesBefore.size + 3, templatesAfter.size)
    }

    @Test
    fun `TC-HEVY-20 a failure after the first insert writes nothing`() = runTest {
        seed()
        val workoutsBefore = db.backupDao().allWorkouts()
        val templatesBefore = templatesById()
        val plan = ready().plan
        val broken = plan.newWorkouts.mapIndexed { i, w ->
            if (i == 0) w else w.copy(exercises = w.exercises.map { it.copy(exerciseTemplateId = "missing") })
        }

        val failure = runCatching { repository.add(plan.newTemplates, broken) }.exceptionOrNull()

        assertNotNull("the foreign key on the second workout's exercises must fail the insert", failure)
        assertEquals(workoutsBefore, db.backupDao().allWorkouts())
        assertEquals(templatesBefore, templatesById())
    }

    @Test
    fun `add never replaces an existing template`() = runTest {
        seed()
        val workoutsBefore = db.backupDao().allWorkouts()
        val plan = ready().plan
        val clash = plan.newTemplates.first().copy(id = benchPress.id)

        val failure = runCatching { repository.add(listOf(clash), plan.newWorkouts) }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(benchPress, templatesById().getValue(benchPress.id))
        assertEquals(workoutsBefore, db.backupDao().allWorkouts())
    }

    @Test
    fun `TC-HEVY-21 applying the fixture a second time adds nothing`() = runTest {
        seed()
        val first = ready()
        apply(first.workouts, first.defaultResolutions, bodyWeightKg = null)
        val workoutsBefore = db.backupDao().allWorkouts()
        val templatesBefore = templatesById()

        val second = ready()
        val applied = apply(second.workouts, second.defaultResolutions, bodyWeightKg = null)

        assertEquals(0, second.plan.newWorkouts.size)
        assertEquals(2, second.plan.duplicatesSkipped)
        assertEquals(0, second.plan.newTemplates.size)
        assertTrue(second.matches.getValue("Lateral Raise (Dumbbell)") is HevyMatch.Exact)
        assertEquals(0, applied.newWorkouts.size)
        assertEquals(2, applied.duplicatesSkipped)
        assertEquals(0, applied.newTemplates.size)
        assertEquals(workoutsBefore, db.backupDao().allWorkouts())
        assertEquals(templatesBefore, templatesById())
    }

    @Test
    fun `drafts and soft-deleted workouts count as existing starts (D-044)`() = runTest {
        seed()
        val workouts = RoomWorkoutRepository(db)
        workouts.update("draft") { it.copy(startedAtEpochMs = pushStart + 37_000) }
        workouts.save(workout("deleted", startedAt = legsStart, finishedAt = legsStart + 1_000, deletedAt = legsStart + 2_000))

        val plan = ready().plan

        assertEquals(2, plan.duplicatesSkipped)
        assertEquals(0, plan.newWorkouts.size)
    }

    private object UnusedSettings : SettingsRepository {
        override val settings: Flow<Settings> = flowOf(Settings())

        override suspend fun setWeightUnit(unit: WeightUnit) = error("unused")

        override suspend fun setDefaultRestSeconds(seconds: Int?) = error("unused")

        override suspend fun setPreviousMode(mode: PreviousMode) = error("unused")

        override suspend fun setKeepScreenAwake(enabled: Boolean) = error("unused")

        override suspend fun setSoundEnabled(enabled: Boolean) = error("unused")

        override suspend fun setVibrationEnabled(enabled: Boolean) = error("unused")

        override suspend fun setWeekStart(day: WeekStartDay) = error("unused")

        override suspend fun setBodyWeightKg(kg: Double?) = error("unused")
    }
}
