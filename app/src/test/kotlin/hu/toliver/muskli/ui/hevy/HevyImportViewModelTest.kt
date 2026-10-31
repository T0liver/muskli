package hu.toliver.muskli.ui.hevy

import hu.toliver.muskli.domain.importer.hevy.HevyProblem
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.HevyImportRepository
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.domain.usecase.importer.ApplyHevyImportUseCase
import hu.toliver.muskli.domain.usecase.importer.PreviewHevyImportUseCase
import hu.toliver.muskli.domain.usecase.library.SearchExercisesUseCase
import hu.toliver.muskli.ui.backup.BackupDocuments
import hu.toliver.muskli.ui.library.FakeExerciseTemplateRepository
import hu.toliver.muskli.ui.library.sampleLibrary
import hu.toliver.muskli.ui.settings.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HevyImportViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val templates = FakeExerciseTemplateRepository(sampleLibrary)
    private val hevy = FakeHevyRepository(templates)
    private val settings = FakeSettingsRepository()
    private val documents = FakeDocuments()
    private val clock = object : Clock {
        override fun nowEpochMs() = 1_800_000_000_000L

        override fun utcOffsetS(epochMs: Long) = 0
    }
    private var nextId = 0
    private val ids = IdGenerator { "id-${nextId++}" }
    private val viewModel by lazy {
        HevyImportViewModel(
            preview = PreviewHevyImportUseCase(hevy, ids, clock),
            apply = ApplyHevyImportUseCase(hevy, settings, ids, clock),
            search = SearchExercisesUseCase(templates),
            settings = settings,
            documents = documents,
            work = dispatcher,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        documents.files["content://workouts"] = WORKOUTS_CSV
        documents.files["content://measurements"] = MEASUREMENTS_CSV
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun preview(): HevyImportUiState.Preview = viewModel.uiState.value as HevyImportUiState.Preview

    @Test
    fun `starts at the file picker`() {
        assertEquals(HevyImportUiState.Pick, viewModel.uiState.value)
    }

    @Test
    fun `a non-Hevy file is rejected with the parser errors and nothing is written`() {
        documents.files["content://other"] = "name,value\nx,1\n"

        viewModel.onEvent(HevyImportEvent.FilePicked("content://other"))

        val reason = (viewModel.uiState.value as HevyImportUiState.Rejected).reason as HevyRejection.Errors
        assertEquals(listOf(HevyProblem.NOT_HEVY_EXPORT), reason.errors.map { it.problem })
        assertEquals(emptyList<Workout>(), hevy.added)
    }

    @Test
    fun `a measurements file and an unreadable file are rejected with their own message`() {
        documents.files["content://measurements"] = "\"date\",\"weight_kg\"\n\"1 Mar 2026, 08:30\",80.4\n"

        viewModel.onEvent(HevyImportEvent.FilePicked("content://measurements"))
        assertEquals(HevyImportUiState.Rejected(HevyRejection.MeasurementsFile), viewModel.uiState.value)

        viewModel.onEvent(HevyImportEvent.FilePicked("content://missing"))
        assertEquals(HevyImportUiState.Rejected(HevyRejection.ReadFailed), viewModel.uiState.value)
    }

    @Test
    fun `preview shows the default plan with unmatched exercises first`() {
        viewModel.onEvent(HevyImportEvent.FilePicked("content://workouts"))

        val state = preview()
        assertEquals(
            HevyImportSummary(
                workoutsInFile = 2,
                newWorkouts = 2,
                duplicatesSkipped = 0,
                sets = 5,
                rpeDropped = 1,
                firstStartEpochMs = PULL_START,
                lastStartEpochMs = PUSH_START,
                newCustomExercises = 2,
            ),
            state.summary,
        )
        assertEquals(
            listOf(
                HevyExerciseRow("Cable Fly", HevyRowResolution.NewCustom(ExerciseType.WEIGHT_REPS), unmatched = true),
                HevyExerciseRow("Dead Hang", HevyRowResolution.NewCustom(ExerciseType.DURATION), unmatched = true),
                HevyExerciseRow("Bench Press (Barbell)", HevyRowResolution.Library("Bench Press (Barbell)", check = false), unmatched = false),
                HevyExerciseRow("Pull-Up", HevyRowResolution.Library("Pull Up", check = true), unmatched = false),
            ),
            state.exercises,
        )
        assertTrue(state.canImport)
        assertNull(state.mapping)
    }

    @Test
    fun `mapping an unmatched exercise to a library exercise lowers the new custom count and the import uses it`() {
        viewModel.onEvent(HevyImportEvent.FilePicked("content://workouts"))

        viewModel.onEvent(HevyImportEvent.RowClicked("Cable Fly"))
        viewModel.onEvent(HevyImportEvent.QueryChanged("bench"))
        val dialog = requireNotNull(preview().mapping)
        assertTrue(dialog.createCustomSelected)
        assertEquals(listOf("bench-press-barbell", "bench-press-dumbbell"), dialog.results.map { it.templateId })

        viewModel.onEvent(HevyImportEvent.TemplateChosen("bench-press-dumbbell"))
        val state = preview()
        assertNull(state.mapping)
        assertEquals(1, state.summary.newCustomExercises)
        assertEquals(HevyRowResolution.Library("Bench Press (Dumbbell)", check = false), state.exercises.first().resolution)

        viewModel.onEvent(HevyImportEvent.ImportClicked)
        assertEquals(HevyImportUiState.Done(workouts = 2, newCustomExercises = 1, duplicatesSkipped = 0), viewModel.uiState.value)
        val push = hevy.added.single { it.title == "Push" }
        assertEquals(listOf("bench-press-barbell", "bench-press-dumbbell"), push.exercises.map { it.exerciseTemplateId })
    }

    @Test
    fun `an automatic match can be changed to a new custom exercise`() {
        viewModel.onEvent(HevyImportEvent.FilePicked("content://workouts"))

        viewModel.onEvent(HevyImportEvent.RowClicked("Pull-Up"))
        assertTrue(requireNotNull(preview().mapping).results.single { it.templateId == "pull-up" }.selected)
        viewModel.onEvent(HevyImportEvent.CreateCustomChosen)

        assertEquals(3, preview().summary.newCustomExercises)
        assertEquals(HevyRowResolution.NewCustom(ExerciseType.REPS_ONLY), preview().exercises.single { it.hevyTitle == "Pull-Up" }.resolution)
    }

    @Test
    fun `dismissing the mapping dialog keeps the resolution`() {
        viewModel.onEvent(HevyImportEvent.FilePicked("content://workouts"))
        val before = preview()

        viewModel.onEvent(HevyImportEvent.RowClicked("Cable Fly"))
        viewModel.onEvent(HevyImportEvent.MappingDismissed)

        assertEquals(before, preview())
    }

    @Test
    fun `apply success shows the result, and the same file again has nothing to import`() {
        viewModel.onEvent(HevyImportEvent.FilePicked("content://workouts"))
        viewModel.onEvent(HevyImportEvent.ImportClicked)

        assertEquals(HevyImportUiState.Done(workouts = 2, newCustomExercises = 2, duplicatesSkipped = 0), viewModel.uiState.value)
        assertEquals(setOf("Cable Fly", "Dead Hang"), hevy.addedTemplates.map { it.title }.toSet())

        viewModel.onEvent(HevyImportEvent.FilePicked("content://workouts"))
        val again = preview()
        assertEquals(0, again.summary.newWorkouts)
        assertEquals(2, again.summary.duplicatesSkipped)
        assertEquals(0, again.summary.newCustomExercises)
        assertFalse(again.canImport)

        viewModel.onEvent(HevyImportEvent.ImportClicked)
        assertEquals(again, viewModel.uiState.value)
        assertEquals(2, hevy.added.size)
    }

    @Test
    fun `a failed write shows that nothing was changed`() {
        hevy.failOnAdd = true
        viewModel.onEvent(HevyImportEvent.FilePicked("content://workouts"))

        viewModel.onEvent(HevyImportEvent.ImportClicked)

        assertEquals(HevyImportUiState.Failed, viewModel.uiState.value)
        assertEquals(emptyList<Workout>(), hevy.added)
        assertNull(settings.settings.value.bodyWeightKg)
    }

    private fun pickBoth() {
        viewModel.onEvent(HevyImportEvent.FilePicked("content://workouts"))
        viewModel.onEvent(HevyImportEvent.MeasurementsFilePicked("content://measurements"))
    }

    @Test
    fun `TC-HEVY-22 with body weight unset the latest weight is offered checked and the import sets it in kg`() {
        pickBoth()

        val weight = HevyWeight("80.4", WeightUnit.KG)
        assertEquals(HevyMeasurements.BodyWeight(weight, MEASURED_AT, checked = true), preview().measurements)

        viewModel.onEvent(HevyImportEvent.ImportClicked)
        assertEquals(HevyImportUiState.Done(workouts = 2, newCustomExercises = 2, duplicatesSkipped = 0, bodyWeight = weight), viewModel.uiState.value)
        assertEquals(80.4, settings.settings.value.bodyWeightKg)
    }

    @Test
    fun `TC-HEVY-22 with body weight set the offer starts unchecked and the import leaves it unchanged`() {
        settings.settings.update { it.copy(bodyWeightKg = 75.0) }
        pickBoth()

        assertEquals(false, (preview().measurements as HevyMeasurements.BodyWeight).checked)

        viewModel.onEvent(HevyImportEvent.ImportClicked)
        assertEquals(HevyImportUiState.Done(workouts = 2, newCustomExercises = 2, duplicatesSkipped = 0), viewModel.uiState.value)
        assertEquals(75.0, settings.settings.value.bodyWeightKg)
    }

    @Test
    fun `with the lb setting the offer shows pounds and the stored weight stays kg`() {
        settings.settings.update { it.copy(weightUnit = WeightUnit.LB) }
        pickBoth()

        val weight = HevyWeight("177.3", WeightUnit.LB)
        assertEquals(HevyMeasurements.BodyWeight(weight, MEASURED_AT, checked = true), preview().measurements)

        viewModel.onEvent(HevyImportEvent.ImportClicked)
        assertEquals(weight, (viewModel.uiState.value as HevyImportUiState.Done).bodyWeight)
        assertEquals(80.4, settings.settings.value.bodyWeightKg)
    }

    @Test
    fun `the weight is passed only when the box is checked`() {
        pickBoth()

        viewModel.onEvent(HevyImportEvent.BodyWeightToggled(false))
        assertEquals(false, (preview().measurements as HevyMeasurements.BodyWeight).checked)
        viewModel.onEvent(HevyImportEvent.ImportClicked)

        assertNull((viewModel.uiState.value as HevyImportUiState.Done).bodyWeight)
        assertNull(settings.settings.value.bodyWeightKg)
    }

    @Test
    fun `checking the box when body weight is set overwrites it`() {
        settings.settings.update { it.copy(bodyWeightKg = 75.0) }
        pickBoth()

        viewModel.onEvent(HevyImportEvent.BodyWeightToggled(true))
        viewModel.onEvent(HevyImportEvent.ImportClicked)

        assertEquals(80.4, settings.settings.value.bodyWeightKg)
    }

    @Test
    fun `a wrong measurements file keeps the workout preview and passes no weight`() {
        documents.files["content://no-weight"] = "\"date\",\"weight_kg\"\n\"5 Mar 2026, 00:00\",\n"
        viewModel.onEvent(HevyImportEvent.FilePicked("content://workouts"))
        val before = preview()

        viewModel.onEvent(HevyImportEvent.MeasurementsFilePicked("content://no-weight"))
        assertEquals(before.copy(measurements = HevyMeasurements.NoBodyWeight), preview())

        viewModel.onEvent(HevyImportEvent.MeasurementsFilePicked("content://workouts"))
        assertEquals(before.copy(measurements = HevyMeasurements.NotMeasurementsFile), preview())

        viewModel.onEvent(HevyImportEvent.MeasurementsFilePicked("content://missing"))
        assertEquals(before.copy(measurements = HevyMeasurements.ReadFailed), preview())

        viewModel.onEvent(HevyImportEvent.ImportClicked)
        assertEquals(HevyImportUiState.Done(workouts = 2, newCustomExercises = 2, duplicatesSkipped = 0), viewModel.uiState.value)
        assertNull(settings.settings.value.bodyWeightKg)
    }

    @Test
    fun `a later measurements file replaces the earlier offer`() {
        documents.files["content://no-weight"] = "\"date\",\"weight_kg\"\n\"5 Mar 2026, 00:00\",\n"
        pickBoth()

        viewModel.onEvent(HevyImportEvent.MeasurementsFilePicked("content://no-weight"))
        viewModel.onEvent(HevyImportEvent.ImportClicked)

        assertNull(settings.settings.value.bodyWeightKg)
    }

    @Test
    fun `a zero latest weight is not offered and the import still succeeds`() {
        documents.files["content://zero"] = "\"date\",\"weight_kg\"\n\"5 Mar 2026, 00:00\",0\n"
        viewModel.onEvent(HevyImportEvent.FilePicked("content://workouts"))
        viewModel.onEvent(HevyImportEvent.MeasurementsFilePicked("content://zero"))

        assertEquals(HevyMeasurements.NoBodyWeight, preview().measurements)

        viewModel.onEvent(HevyImportEvent.ImportClicked)
        assertEquals(HevyImportUiState.Done(workouts = 2, newCustomExercises = 2, duplicatesSkipped = 0), viewModel.uiState.value)
        assertNull(settings.settings.value.bodyWeightKg)
    }

    /** Shares templates with the library fake, so search sees the customs an import created. */
    private class FakeHevyRepository(private val library: FakeExerciseTemplateRepository) : HevyImportRepository {
        val added = mutableListOf<Workout>()
        val addedTemplates = mutableListOf<ExerciseTemplate>()
        var failOnAdd = false

        override suspend fun existingWorkoutStartsEpochMs(): List<Long> = added.map { it.startedAtEpochMs }

        override suspend fun allTemplates(): List<ExerciseTemplate> = library.templates.value.values.toList()

        override suspend fun add(templates: List<ExerciseTemplate>, workouts: List<Workout>) {
            check(!failOnAdd) { "disk full" }
            library.templates.update { current -> current + templates.associateBy { it.id } }
            addedTemplates += templates
            added += workouts
        }
    }

    private class FakeDocuments : BackupDocuments {
        val files = mutableMapOf<String, String>()

        override suspend fun write(uri: String, text: String) = error("unused")

        override suspend fun read(uri: String) = files[uri] ?: throw IOException("gone")
    }

    private companion object {
        // 14 Mar 2026 18:05 and 12 Mar 2026 07:00 UTC; the test clock's offset is 0.
        const val PUSH_START = 1_773_511_500_000L
        const val PULL_START = 1_773_298_800_000L

        // 1 Mar 2026 08:30 UTC, the latest row with a weight in the measurements fixture.
        const val MEASURED_AT = 1_772_353_800_000L

        // Same rows as domain/src/test/resources/hevy/measurement_data.csv, trimmed to the columns the parser needs.
        val MEASUREMENTS_CSV = """
            "date","weight_kg","fat_percent","waist_cm"
            "20 Feb 2026, 00:00",81.2,,
            "1 Mar 2026, 08:30",80.4,18.5,
            "05 Mar 2026, 00:00",,,88
        """.trimIndent() + "\n"

        val WORKOUTS_CSV = """
            "title","start_time","end_time","description","exercise_title","superset_id","exercise_notes","set_index","set_type","weight_kg","reps","distance_km","duration_seconds","rpe"
            "Push","14 Mar 2026, 18:05","14 Mar 2026, 19:00","","Bench Press (Barbell)",,"",0,"normal",80,5,,,8
            "Push","14 Mar 2026, 18:05","14 Mar 2026, 19:00","","Bench Press (Barbell)",,"",1,"normal",80,5,,,
            "Push","14 Mar 2026, 18:05","14 Mar 2026, 19:00","","Cable Fly",,"",0,"normal",15,12,,,
            "Pull","12 Mar 2026, 07:00","12 Mar 2026, 07:40","","Pull-Up",,"",0,"normal",,8,,,
            "Pull","12 Mar 2026, 07:00","12 Mar 2026, 07:40","","Dead Hang",,"",0,"normal",,,,60,
        """.trimIndent() + "\n"
    }
}
