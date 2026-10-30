package hu.toliver.muskli.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.data.db.MuskliDatabase
import hu.toliver.muskli.data.db.benchPress
import hu.toliver.muskli.data.db.dumbbellRow
import hu.toliver.muskli.data.db.secondaryMuscleEntities
import hu.toliver.muskli.data.db.toEntity
import hu.toliver.muskli.data.db.workout
import hu.toliver.muskli.data.repo.RoomWorkoutRepository
import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.domain.model.RestTimer
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.ImportError
import hu.toliver.muskli.domain.repo.ImportProblem
import hu.toliver.muskli.domain.repo.ImportResult
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeekStartDay
import hu.toliver.muskli.domain.settings.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JsonBackupRepositoryTest {
    private val clock = object : Clock {
        override fun nowEpochMs() = 5_000_000L

        override fun utcOffsetS(epochMs: Long) = 3_600
    }
    private val databases = mutableListOf<MuskliDatabase>()

    @After
    fun tearDown() = databases.forEach { it.close() }

    private fun newDb(): MuskliDatabase =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MuskliDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also { databases += it }

    private suspend fun MuskliDatabase.addTemplates(vararg templates: ExerciseTemplate) =
        templates.forEach { exerciseTemplateDao().upsert(it.toEntity(), it.secondaryMuscleEntities()) }

    private val unusedCatalogue = benchPress.copy(id = "plank", title = "Plank", type = ExerciseType.DURATION)
    private val archivedCustom = dumbbellRow.copy(
        id = "cable-fly",
        title = "Cable fly",
        primaryMuscle = MuscleGroup.CHEST,
        secondaryMuscles = emptySet(),
        equipment = Equipment.MACHINE,
        archived = true,
    )
    private val customSettings = Settings(
        weightUnit = WeightUnit.LB,
        defaultRestSeconds = null,
        previousMode = PreviousMode.ANYWHERE,
        keepScreenAwake = false,
        soundEnabled = false,
        vibrationEnabled = true,
        weekStart = WeekStartDay.SUNDAY,
        bodyWeightKg = 82.5,
    )

    /** Finished, soft-deleted, and live workouts; catalogue, custom, and archived exercises; non-default settings. */
    private suspend fun realisticSource(): Pair<MuskliDatabase, FakeSettings> {
        val db = newDb()
        db.addTemplates(benchPress, dumbbellRow, unusedCatalogue, archivedCustom)
        val workouts = RoomWorkoutRepository(db)
        workouts.save(workout("w1", startedAt = 10_000L, finishedAt = 20_000L))
        workouts.save(workout("w2", startedAt = 30_000L, finishedAt = 40_000L).copy(title = "Pull", notes = null))
        workouts.save(workout("w3", startedAt = 50_000L, finishedAt = 60_000L, deletedAt = 70_000L))
        workouts.save(
            workout("draft", startedAt = 80_000L, finishedAt = null)
                .copy(restTimer = RestTimer(endsAtEpochMs = 90_000L, durationS = 120)),
        )
        return db to FakeSettings(customSettings)
    }

    @Test
    fun `export then import into an empty database gives identical data (TC-DATA-05)`() = runTest {
        val (source, sourceSettings) = realisticSource()
        val exported = JsonBackupRepository(source, sourceSettings, clock).export()

        val target = newDb()
        val targetSettings = FakeSettings(Settings())
        val result = JsonBackupRepository(target, targetSettings, clock).import(exported)

        assertEquals(ImportResult.Imported(workouts = 4, customExercises = 2), result)
        assertEquals(source.backupDao().allWorkouts(), target.backupDao().allWorkouts())
        assertEquals(sourceSettings.settings.value, targetSettings.settings.value)
        assertEquals(exported, JsonBackupRepository(target, targetSettings, clock).export())
        val sourceTemplates = source.exerciseTemplateDao().getAll().associateBy { it.template.id }
        target.exerciseTemplateDao().getAll().forEach { imported ->
            val original = sourceTemplates.getValue(imported.template.id)
            assertEquals(original.template, imported.template)
            assertEquals(original.secondaryMuscles.toSet(), imported.secondaryMuscles.toSet())
        }
        assertEquals(
            "Only used catalogue exercises travel; the target gets the rest from its own catalogue",
            setOf(benchPress.id, dumbbellRow.id, archivedCustom.id),
            target.exerciseTemplateDao().getAll().map { it.template.id }.toSet(),
        )
    }

    @Test
    fun `import replaces workouts and custom exercises but keeps the device's catalogue`() = runTest {
        val (source, sourceSettings) = realisticSource()
        val exported = JsonBackupRepository(source, sourceSettings, clock).export()

        val target = newDb()
        val deviceBench = benchPress.copy(title = "Bench (device)", secondaryMuscles = setOf(MuscleGroup.TRICEPS))
        val oldCustom = dumbbellRow.copy(id = "old-custom", title = "Old custom")
        target.addTemplates(deviceBench, oldCustom)
        RoomWorkoutRepository(target).save(workout("old", startedAt = 1_000L).let { old ->
            old.copy(exercises = old.exercises.map { it.copy(exerciseTemplateId = oldCustom.id) })
        })

        val result = JsonBackupRepository(target, FakeSettings(Settings()), clock).import(exported)

        assertTrue(result is ImportResult.Imported)
        assertEquals(listOf("w1", "w2", "w3", "draft"), target.backupDao().allWorkouts().map { it.workout.id })
        val templates = target.exerciseTemplateDao().getAll().associateBy { it.template.id }
        assertEquals(setOf(benchPress.id, dumbbellRow.id, archivedCustom.id), templates.keys)
        assertEquals("Bench (device)", templates.getValue(benchPress.id).template.title)
        assertEquals(listOf(MuscleGroup.TRICEPS), templates.getValue(benchPress.id).secondaryMuscles.map { it.muscle })
    }

    @Test
    fun `a file with bad records applies nothing and reports each one (TC-DATA-06)`() = runTest {
        val (source, sourceSettings) = realisticSource()
        val exported = JsonBackupRepository(source, sourceSettings, clock).export()
        val broken = exported
            .replaceFirst("\"type\": \"warmup\"", "\"type\": \"superset\"")
            .replaceFirst("\"exerciseTemplateId\": \"dumbbell-row\"", "\"exerciseTemplateId\": \"missing\"")

        val target = newDb()
        target.addTemplates(benchPress)
        RoomWorkoutRepository(target).save(workout("keep", startedAt = 1_000L).let { keep ->
            keep.copy(exercises = keep.exercises.take(1))
        })
        val before = target.backupDao().allWorkouts()
        val targetSettings = FakeSettings(Settings())

        val result = JsonBackupRepository(target, targetSettings, clock).import(broken)

        assertEquals(
            ImportResult.Rejected(
                listOf(
                    ImportError(ImportProblem.UNKNOWN_EXERCISE, "workouts[0].exercises[1].exerciseTemplateId"),
                    ImportError(ImportProblem.INVALID_VALUE, "workouts[0].exercises[0].sets[0].type"),
                ).sortedBy { it.where },
            ),
            (result as ImportResult.Rejected).copy(errors = result.errors.sortedBy { it.where }),
        )
        assertEquals(before, target.backupDao().allWorkouts())
        assertEquals(listOf(benchPress.id), target.exerciseTemplateDao().getAll().map { it.template.id })
        assertEquals(Settings(), targetSettings.settings.value)
    }

    @Test
    fun `files that are not backups, too new, or clash with the catalogue are rejected`() = runTest {
        val (source, sourceSettings) = realisticSource()
        val exported = JsonBackupRepository(source, sourceSettings, clock).export()
        val target = newDb()
        val repo = JsonBackupRepository(target, FakeSettings(Settings()), clock)

        assertEquals(ImportProblem.MALFORMED, repo.import("not json").problems().single())
        assertEquals(ImportProblem.MALFORMED, repo.import("{\"schemaVersion\": 1}").problems().single())
        assertEquals(
            ImportProblem.UNSUPPORTED_VERSION,
            repo.import(exported.replaceFirst("\"schemaVersion\": 1", "\"schemaVersion\": 2")).problems().single(),
        )
        assertEquals(
            ImportProblem.MULTIPLE_DRAFTS,
            repo.import(exported.replace("\"endTimeMs\": 20000,", "")).problems().single(),
        )

        target.addTemplates(dumbbellRow.copy(isCustom = false))
        assertEquals(ImportProblem.ID_CONFLICT, repo.import(exported).problems().single())
        assertEquals(emptyList<Any>(), target.backupDao().allWorkouts())
    }

    private fun ImportResult.problems() = (this as ImportResult.Rejected).errors.map { it.problem }

    private class FakeSettings(initial: Settings) : SettingsRepository {
        override val settings = MutableStateFlow(initial)

        override suspend fun setWeightUnit(unit: WeightUnit) = settings.update { it.copy(weightUnit = unit) }

        override suspend fun setDefaultRestSeconds(seconds: Int?) = settings.update { it.copy(defaultRestSeconds = seconds) }

        override suspend fun setPreviousMode(mode: PreviousMode) = settings.update { it.copy(previousMode = mode) }

        override suspend fun setKeepScreenAwake(enabled: Boolean) = settings.update { it.copy(keepScreenAwake = enabled) }

        override suspend fun setSoundEnabled(enabled: Boolean) = settings.update { it.copy(soundEnabled = enabled) }

        override suspend fun setVibrationEnabled(enabled: Boolean) = settings.update { it.copy(vibrationEnabled = enabled) }

        override suspend fun setWeekStart(day: WeekStartDay) = settings.update { it.copy(weekStart = day) }

        override suspend fun setBodyWeightKg(kg: Double?) = settings.update { it.copy(bodyWeightKg = kg) }

        private fun MutableStateFlow<Settings>.update(change: (Settings) -> Settings) {
            value = change(value)
        }
    }
}
