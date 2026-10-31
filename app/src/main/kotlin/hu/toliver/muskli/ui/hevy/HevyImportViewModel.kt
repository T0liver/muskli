package hu.toliver.muskli.ui.hevy

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.toliver.muskli.di.WorkDispatcher
import hu.toliver.muskli.domain.importer.hevy.HevyMatch
import hu.toliver.muskli.domain.importer.hevy.HevyResolution
import hu.toliver.muskli.domain.importer.hevy.inferCustomExercise
import hu.toliver.muskli.domain.library.ExerciseFilter
import hu.toliver.muskli.domain.library.searchExercises
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.usecase.importer.ApplyHevyImportUseCase
import hu.toliver.muskli.domain.usecase.importer.HevyPreview
import hu.toliver.muskli.domain.usecase.importer.PreviewHevyImportUseCase
import hu.toliver.muskli.domain.usecase.library.SearchExercisesUseCase
import hu.toliver.muskli.ui.backup.BackupDocuments
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class HevyImportViewModel @Inject constructor(
    private val preview: PreviewHevyImportUseCase,
    private val apply: ApplyHevyImportUseCase,
    private val search: SearchExercisesUseCase,
    private val documents: BackupDocuments,
    @WorkDispatcher private val work: CoroutineDispatcher,
) : ViewModel() {
    private val state = MutableStateFlow<HevyImportUiState>(HevyImportUiState.Pick)
    val uiState: StateFlow<HevyImportUiState> = state.asStateFlow()

    private var ready: HevyPreview.Ready? = null
    private var resolutions: Map<String, HevyResolution> = emptyMap()
    private var templates: Map<String, ExerciseTemplate> = emptyMap()

    /** Title and query of the open mapping dialog. */
    private var mapping: Pair<String, String>? = null

    fun onEvent(event: HevyImportEvent) {
        when (event) {
            is HevyImportEvent.FilePicked -> read(event.uri)
            is HevyImportEvent.RowClicked -> {
                mapping = event.hevyTitle to ""
                render()
            }
            is HevyImportEvent.QueryChanged -> {
                mapping = mapping?.copy(second = event.query)
                render()
            }
            HevyImportEvent.CreateCustomChosen -> choose(HevyResolution.CreateCustom)
            is HevyImportEvent.TemplateChosen -> choose(HevyResolution.UseTemplate(event.templateId))
            HevyImportEvent.MappingDismissed -> {
                mapping = null
                render()
            }
            HevyImportEvent.ImportClicked -> import()
        }
    }

    private fun read(uri: String) {
        if (state.value == HevyImportUiState.Busy) return
        state.value = HevyImportUiState.Busy
        viewModelScope.launch {
            val text = try {
                documents.read(uri)
            } catch (e: IOException) {
                state.value = HevyImportUiState.Rejected(HevyRejection.ReadFailed)
                return@launch
            }
            // A years-long export is parsed and matched in one go; keep it off the main thread.
            when (val result = withContext(work) { preview(text) }) {
                is HevyPreview.Rejected -> state.value = HevyImportUiState.Rejected(HevyRejection.Errors(result.errors))
                is HevyPreview.Measurements -> state.value = HevyImportUiState.Rejected(HevyRejection.MeasurementsFile)
                is HevyPreview.Ready -> {
                    templates = search(ExerciseFilter(includeArchived = true)).first().associateBy { it.id }
                    ready = result
                    resolutions = result.defaultResolutions
                    mapping = null
                    render()
                }
            }
        }
    }

    private fun choose(resolution: HevyResolution) {
        val title = mapping?.first ?: return
        resolutions = resolutions + (title to resolution)
        mapping = null
        render()
    }

    private fun import() {
        val current = ready ?: return
        if ((state.value as? HevyImportUiState.Preview)?.canImport != true) return
        state.value = HevyImportUiState.Busy
        viewModelScope.launch {
            state.value = try {
                val plan = withContext(work) { apply(current.workouts, resolutions, bodyWeightKg = null) }
                HevyImportUiState.Done(plan.newWorkouts.size, plan.newTemplates.size, plan.duplicatesSkipped)
            } catch (e: IllegalStateException) {
                HevyImportUiState.Failed
            } catch (e: SQLiteException) {
                HevyImportUiState.Failed
            }
        }
    }

    private fun render() {
        val current = ready ?: return
        val plan = current.plan
        state.value = HevyImportUiState.Preview(
            summary = HevyImportSummary(
                workoutsInFile = plan.workoutsInFile,
                newWorkouts = plan.newWorkouts.size,
                duplicatesSkipped = plan.duplicatesSkipped,
                sets = plan.setCount,
                rpeDropped = plan.rpeDropped,
                firstStartEpochMs = plan.firstStartEpochMs,
                lastStartEpochMs = plan.lastStartEpochMs,
                newCustomExercises = current.newCustomCount(resolutions),
            ),
            exercises = current.matches
                .map { (title, match) -> HevyExerciseRow(title, rowResolution(current, title, match), match is HevyMatch.Unmatched) }
                .sortedBy { !it.unmatched },
            mapping = mapping?.let { (title, query) -> mappingDialog(current, title, query) },
        )
    }

    private fun rowResolution(current: HevyPreview.Ready, title: String, match: HevyMatch): HevyRowResolution =
        when (val resolution = resolutions[title] ?: current.defaultResolutions.getValue(title)) {
            is HevyResolution.UseTemplate -> HevyRowResolution.Library(
                title = templates[resolution.templateId]?.title.orEmpty(),
                check = match == HevyMatch.Normalised(resolution.templateId),
            )
            HevyResolution.CreateCustom -> {
                val sets = current.workouts.flatMap { w -> w.exercises.filter { it.title == title }.flatMap { e -> e.sets.map { it.data } } }
                HevyRowResolution.NewCustom(inferCustomExercise(title, sets).first)
            }
        }

    private fun mappingDialog(current: HevyPreview.Ready, title: String, query: String): HevyMappingDialog {
        val chosen = resolutions[title]
        val candidateIds = (current.matches[title] as? HevyMatch.Unmatched)?.candidateIds.orEmpty()
        fun option(template: ExerciseTemplate) =
            HevyMappingOption(template.id, template.title, selected = chosen == HevyResolution.UseTemplate(template.id))
        return HevyMappingDialog(
            hevyTitle = title,
            query = query,
            createCustomSelected = chosen == HevyResolution.CreateCustom,
            candidates = candidateIds.mapNotNull { templates[it] }.map(::option),
            results = searchExercises(templates.values.toList(), ExerciseFilter(query = query))
                .filterNot { it.id in candidateIds }
                .map(::option),
        )
    }
}
