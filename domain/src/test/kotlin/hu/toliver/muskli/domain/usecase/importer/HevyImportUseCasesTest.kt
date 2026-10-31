package hu.toliver.muskli.domain.usecase.importer

import hu.toliver.muskli.domain.TestClock
import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.importer.hevy.HevyMatch
import hu.toliver.muskli.domain.importer.hevy.HevyProblem
import hu.toliver.muskli.domain.importer.hevy.HevyResolution
import hu.toliver.muskli.domain.importer.hevy.catalogueV1
import hu.toliver.muskli.domain.importer.hevy.template
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeekStartDay
import hu.toliver.muskli.domain.settings.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HevyImportUseCasesTest {
    private val clock = TestClock(now = 1_800_000_000_000L, offsetS = 3600)
    private var nextId = 0
    private val ids = IdGenerator { "id${nextId++}" }
    private val repository = FakeHevyImportRepository(templates = catalogueV1)
    private val settings = FakeSettings()

    private val preview = PreviewHevyImportUseCase(repository, ids, clock)
    private val apply = ApplyHevyImportUseCase(repository, settings, ids, clock)

    private val pushStart = 1773507900000L
    private val legsStart = 1773295200000L

    private fun resource(name: String) = requireNotNull(javaClass.getResource("/hevy/$name")).readText()

    private suspend fun ready(): HevyPreview.Ready = assertIs<HevyPreview.Ready>(preview(resource("workout_data.csv")))

    @Test
    fun `preview of the fixture matches against all templates and plans with the default resolutions`() = runTest {
        val ready = ready()

        assertEquals(listOf("Push, heavy", "Legs"), ready.workouts.map { it.title })
        assertEquals(HevyMatch.Exact("plank"), ready.matches["Plank"])
        assertEquals(HevyMatch.Normalised("barbell-bench-press"), ready.matches["Bench Press (Barbell)"])
        assertEquals(HevyMatch.Unmatched(emptyList()), ready.matches["Assisted Pull Up"])
        assertEquals(HevyResolution.CreateCustom, ready.defaultResolutions["Assisted Pull Up"])
        assertEquals(HevyResolution.UseTemplate("pull-up"), ready.defaultResolutions["Pull Up"])
        assertEquals(8, ready.matches.size)
        assertEquals(2, ready.plan.newWorkouts.size)
        assertEquals(3, ready.plan.newTemplates.size)
        assertEquals(13, ready.plan.setCount)
        assertEquals(emptyList(), repository.addedWorkouts, "preview writes nothing")
    }

    @Test
    fun `preview counts existing workouts in the same minute as duplicates`() = runTest {
        repository.starts = listOf(pushStart + 59_000)

        val plan = ready().plan

        assertEquals(1, plan.duplicatesSkipped)
        assertEquals(listOf("Legs"), plan.newWorkouts.map { it.title })
    }

    @Test
    fun `preview passes on parser rejections and measurement files`() = runTest {
        val rejected = assertIs<HevyPreview.Rejected>(preview("not,a,hevy,file"))
        assertEquals(listOf(HevyProblem.NOT_HEVY_EXPORT), rejected.errors.map { it.problem })

        val measurements = assertIs<HevyPreview.Measurements>(preview(resource("measurement_data.csv")))
        assertEquals(80.4, measurements.latestBodyWeight?.weightKg)
    }

    @Test
    fun `apply re-plans with fresh ids and adds the new rows`() = runTest {
        val ready = ready()

        val applied = apply(ready.workouts, ready.defaultResolutions, bodyWeightKg = null)

        assertEquals(2, applied.newWorkouts.size)
        assertEquals(3, applied.newTemplates.size)
        assertEquals(applied.newWorkouts, repository.addedWorkouts)
        assertEquals(applied.newTemplates, repository.addedTemplates)
        val previewIds = ready.plan.newWorkouts.map { it.id } + ready.plan.newTemplates.map { it.id }
        val appliedIds = applied.newWorkouts.map { it.id } + applied.newTemplates.map { it.id }
        assertTrue(appliedIds.none { it in previewIds }, "apply must not reuse the preview's ids")
    }

    @Test
    fun `apply re-reads existing starts, so a workout logged since the preview becomes a duplicate`() = runTest {
        val ready = ready()
        repository.starts = listOf(legsStart + 1_000)

        val applied = apply(ready.workouts, ready.defaultResolutions, bodyWeightKg = null)

        assertEquals(1, applied.duplicatesSkipped)
        assertEquals(listOf("Push, heavy"), repository.addedWorkouts.map { it.title })
    }

    @Test
    fun `apply builds resolutions only from template ids it read`() = runTest {
        val ready = ready()
        val resolutions = ready.defaultResolutions +
            mapOf(
                "Plank" to HevyResolution.UseTemplate("not-on-this-device"),
                "Lateral Raise (Dumbbell)" to HevyResolution.UseTemplate("gone-custom"),
            ) - "Pull Up"

        val applied = apply(ready.workouts, resolutions, bodyWeightKg = null)

        val used = applied.newWorkouts.flatMap { w -> w.exercises.map { it.exerciseTemplateId } }.toSet()
        val known = (catalogueV1 + applied.newTemplates).map { it.id }.toSet()
        assertTrue(used.all { it in known }, "unknown template ids: ${used - known}")
        assertEquals(
            listOf("barbell-bench-press", applied.newTemplates.single { it.title == "Lateral Raise (Dumbbell)" }.id, "pull-up", "plank", "barbell-bench-press"),
            applied.newWorkouts.first().exercises.map { it.exerciseTemplateId },
            "unknown ids and missing titles fall back to a fresh default resolution",
        )
    }

    @Test
    fun `apply keeps a user's mapping to a template it read, archived included`() = runTest {
        val archived = template("old-raise", "Side Raise", isCustom = true, archived = true)
        repository.templates = catalogueV1 + archived
        val ready = ready()

        val applied = apply(
            ready.workouts,
            ready.defaultResolutions + ("Lateral Raise (Dumbbell)" to HevyResolution.UseTemplate(archived.id)),
            bodyWeightKg = null,
        )

        assertEquals(archived.id, applied.newWorkouts.first().exercises[1].exerciseTemplateId)
        assertEquals(setOf("Goblet Squat (Kettlebell)", "Assisted Pull Up"), applied.newTemplates.map { it.title }.toSet())
    }

    @Test
    fun `new custom count follows the resolutions and equals the default plan`() = runTest {
        val ready = ready()

        assertEquals(ready.plan.newTemplates.size, ready.newCustomCount(ready.defaultResolutions))
        assertEquals(2, ready.newCustomCount(ready.defaultResolutions + ("Assisted Pull Up" to HevyResolution.UseTemplate("pull-up"))))
        assertEquals(4, ready.newCustomCount(ready.defaultResolutions + ("Plank" to HevyResolution.CreateCustom)))
    }

    @Test
    fun `new custom count leaves out titles used only by duplicates and treats a missing resolution as its default`() = runTest {
        repository.starts = listOf(legsStart)
        val ready = ready()

        assertEquals(1, ready.newCustomCount(ready.defaultResolutions))
        assertEquals(1, ready.newCustomCount(ready.defaultResolutions - "Lateral Raise (Dumbbell)"))
        assertEquals(1, ready.newCustomCount(ready.defaultResolutions + ("Rowing Machine" to HevyResolution.CreateCustom)))
    }

    @Test
    fun `TC-HEVY-21 applying the same file again adds nothing`() = runTest {
        val ready = ready()
        apply(ready.workouts, ready.defaultResolutions, bodyWeightKg = null)

        val again = apply(ready.workouts, ready.defaultResolutions, bodyWeightKg = null)

        assertEquals(0, again.newWorkouts.size)
        assertEquals(2, again.duplicatesSkipped)
        assertEquals(0, again.newTemplates.size)
        assertEquals(2, repository.addedWorkouts.size)
        assertEquals(3, repository.addedTemplates.size)
    }

    @Test
    fun `TC-HEVY-22 body weight is written after the import when requested`() = runTest {
        val log = mutableListOf<String>()
        repository.log = log
        settings.log = log
        val ready = ready()

        apply(ready.workouts, ready.defaultResolutions, bodyWeightKg = 80.4)

        assertEquals(80.4, settings.settings.value.bodyWeightKg)
        assertEquals(listOf("add", "setBodyWeightKg"), log)
    }

    @Test
    fun `TC-HEVY-22 body weight is unchanged when not requested`() = runTest {
        settings.settings.value = Settings(bodyWeightKg = 75.0)
        val ready = ready()

        apply(ready.workouts, ready.defaultResolutions, bodyWeightKg = null)

        assertEquals(75.0, settings.settings.value.bodyWeightKg)
        assertEquals(2, repository.addedWorkouts.size)
    }

    @Test
    fun `TC-HEVY-22 body weight is not written when the import fails`() = runTest {
        val ready = ready()
        repository.failOnAdd = IllegalStateException("disk full")

        assertThrows<IllegalStateException> { apply(ready.workouts, ready.defaultResolutions, bodyWeightKg = 80.4) }

        assertEquals(null, settings.settings.value.bodyWeightKg)
    }

    @Test
    fun `a body weight that Settings would reject fails before anything is written`() = runTest {
        val ready = ready()

        assertThrows<IllegalArgumentException> { apply(ready.workouts, ready.defaultResolutions, bodyWeightKg = 0.0) }

        assertEquals(emptyList(), repository.addedWorkouts)
        assertEquals(null, settings.settings.value.bodyWeightKg)
    }

    @Test
    fun `a measurements file applies only the body weight`() = runTest {
        apply(workouts = emptyList(), resolutions = emptyMap(), bodyWeightKg = 80.4)

        assertEquals(80.4, settings.settings.value.bodyWeightKg)
        assertEquals(emptyList(), repository.addedWorkouts)
    }

    private class FakeSettings : SettingsRepository {
        override val settings = MutableStateFlow(Settings())
        var log: MutableList<String> = mutableListOf()

        override suspend fun setWeightUnit(unit: WeightUnit) = error("unused")

        override suspend fun setDefaultRestSeconds(seconds: Int?) = error("unused")

        override suspend fun setPreviousMode(mode: PreviousMode) = error("unused")

        override suspend fun setKeepScreenAwake(enabled: Boolean) = error("unused")

        override suspend fun setSoundEnabled(enabled: Boolean) = error("unused")

        override suspend fun setVibrationEnabled(enabled: Boolean) = error("unused")

        override suspend fun setWeekStart(day: WeekStartDay) = error("unused")

        override suspend fun setBodyWeightKg(kg: Double?) {
            require(kg == null || kg > 0) { "Body weight must be positive" }
            settings.value = settings.value.copy(bodyWeightKg = kg)
            log += "setBodyWeightKg"
        }
    }
}
