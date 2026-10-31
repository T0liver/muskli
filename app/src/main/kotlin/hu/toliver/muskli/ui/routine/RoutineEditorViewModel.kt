package hu.toliver.muskli.ui.routine

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.PlannedSet
import hu.toliver.muskli.domain.model.RoutineExercise
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.repo.RoutineRepository
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.domain.usecase.routine.CreateRoutineUseCase
import hu.toliver.muskli.domain.usecase.routine.DeleteRoutineUseCase
import hu.toliver.muskli.domain.usecase.routine.UpdateRoutineUseCase
import hu.toliver.muskli.ui.components.SetColumn
import hu.toliver.muskli.ui.workout.columnsFor
import hu.toliver.muskli.ui.workout.format
import hu.toliver.muskli.ui.workout.parseInto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class PlannedSetUi(
    val key: String,
    val number: Int,
    val type: SetType,
    val values: List<String>,
)

@Immutable
data class RoutineExerciseUi(
    val key: String,
    val name: String,
    val columns: List<SetColumn>,
    val rest: String,
    val restInvalid: Boolean,
    val sets: List<PlannedSetUi>,
)

sealed interface RoutineEditorUiState {
    data object Loading : RoutineEditorUiState

    /** Deleted while the editor was opening. */
    data object Missing : RoutineEditorUiState

    data class Ready(
        val isNew: Boolean,
        val title: String,
        val exercises: List<RoutineExerciseUi>,
        val weightUnit: WeightUnit,
        val showErrors: Boolean = false,
        val confirmDelete: Boolean = false,
    ) : RoutineEditorUiState {
        val titleMissing: Boolean get() = title.isBlank()
    }

    /** Saved or deleted; the caller leaves the editor. */
    data object Done : RoutineEditorUiState
}

sealed interface RoutineEditorEvent {
    data class TitleChanged(val text: String) : RoutineEditorEvent
    data class ExercisesPicked(val templateIds: List<String>) : RoutineEditorEvent
    data class ExerciseMoved(val exerciseKey: String, val toIndex: Int) : RoutineEditorEvent
    data class ExerciseRemoved(val exerciseKey: String) : RoutineEditorEvent
    data class RestChanged(val exerciseKey: String, val text: String) : RoutineEditorEvent
    data class AddSetClicked(val exerciseKey: String) : RoutineEditorEvent
    data class SetDeleted(val exerciseKey: String, val setKey: String) : RoutineEditorEvent
    data class SetTypeChosen(val exerciseKey: String, val setKey: String, val type: SetType) : RoutineEditorEvent
    data class ValueChanged(val exerciseKey: String, val setKey: String, val column: SetColumn, val text: String) : RoutineEditorEvent
    data object SaveClicked : RoutineEditorEvent
    data object DeleteClicked : RoutineEditorEvent
    data object DeleteConfirmed : RoutineEditorEvent
    data object DeleteDismissed : RoutineEditorEvent
}

/**
 * Creates a routine (null [routineId]) or edits one. The routine is saved as a whole (D-052), so
 * the form lives here until Save; Back leaves it unsaved, like the exercise form (D-054).
 */
@HiltViewModel(assistedFactory = RoutineEditorViewModel.Factory::class)
class RoutineEditorViewModel @AssistedInject constructor(
    @Assisted private val routineId: String?,
    private val routines: RoutineRepository,
    private val templates: ExerciseTemplateRepository,
    private val settingsRepository: SettingsRepository,
    private val createRoutine: CreateRoutineUseCase,
    private val updateRoutine: UpdateRoutineUseCase,
    private val deleteRoutine: DeleteRoutineUseCase,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(routineId: String?): RoutineEditorViewModel
    }

    private val form = MutableStateFlow<Form?>(null)
    private val state = MutableStateFlow<RoutineEditorUiState>(RoutineEditorUiState.Loading)
    val uiState: StateFlow<RoutineEditorUiState> = state.asStateFlow()

    // Rows have no ids in a routine (D-052); these keys only keep Compose rows apart while editing.
    private var nextKey = 0

    init {
        viewModelScope.launch {
            val unit = settingsRepository.settings.first().weightUnit
            if (routineId == null) {
                publish(Form(title = "", exercises = emptyList(), unit = unit))
                return@launch
            }
            val routine = routines.get(routineId)
            if (routine == null) {
                state.value = RoutineEditorUiState.Missing
                return@launch
            }
            publish(Form(title = routine.title, exercises = routine.exercises.map { exerciseForm(it) }, unit = unit))
        }
    }

    fun onEvent(event: RoutineEditorEvent) {
        when (event) {
            is RoutineEditorEvent.TitleChanged -> edit { it.copy(title = event.text) }
            is RoutineEditorEvent.ExercisesPicked -> viewModelScope.launch {
                val added = event.templateIds.map { exerciseForm(RoutineExercise(it, sets = listOf(PlannedSet()))) }
                edit { it.copy(exercises = it.exercises + added) }
            }
            is RoutineEditorEvent.ExerciseMoved -> edit { f ->
                val from = f.exercises.indexOfFirst { it.key == event.exerciseKey }
                if (from < 0 || event.toIndex !in f.exercises.indices) return@edit f
                f.copy(exercises = f.exercises.toMutableList().apply { add(event.toIndex, removeAt(from)) })
            }
            is RoutineEditorEvent.ExerciseRemoved -> edit { f -> f.copy(exercises = f.exercises.filterNot { it.key == event.exerciseKey }) }
            is RoutineEditorEvent.RestChanged -> exercise(event.exerciseKey) { it.copy(rest = event.text) }
            is RoutineEditorEvent.AddSetClicked -> exercise(event.exerciseKey) { e ->
                // Like Add set in a workout (D-050): the row above's values, as a normal set.
                val unit = form.value?.unit ?: return@exercise e
                val copied = e.sets.lastOrNull()?.toPlannedSet(unit)?.copy(type = SetType.NORMAL) ?: PlannedSet()
                e.copy(sets = e.sets + SetForm(newKey(), copied))
            }
            is RoutineEditorEvent.SetDeleted -> exercise(event.exerciseKey) { e -> e.copy(sets = e.sets.filterNot { it.key == event.setKey }) }
            is RoutineEditorEvent.SetTypeChosen -> set(event.exerciseKey, event.setKey) { it.copy(base = it.base.copy(type = event.type)) }
            is RoutineEditorEvent.ValueChanged -> set(event.exerciseKey, event.setKey) { it.copy(edits = it.edits + (event.column to event.text)) }
            RoutineEditorEvent.SaveClicked -> save()
            RoutineEditorEvent.DeleteClicked -> ready { it.copy(confirmDelete = true) }
            RoutineEditorEvent.DeleteDismissed -> ready { it.copy(confirmDelete = false) }
            RoutineEditorEvent.DeleteConfirmed -> {
                val id = routineId ?: return
                viewModelScope.launch {
                    deleteRoutine(id)
                    state.value = RoutineEditorUiState.Done
                }
            }
        }
    }

    private fun save() {
        val current = form.value ?: return
        val invalidRest = current.exercises.any { parseRest(it.rest) == RestInput.Invalid }
        if (current.title.isBlank() || invalidRest) {
            ready { it.copy(showErrors = true) }
            return
        }
        val exercises = current.exercises.map { it.toRoutineExercise(current.unit) }
        viewModelScope.launch {
            if (routineId == null) {
                createRoutine(current.title, exercises)
            } else if (updateRoutine(routineId, current.title, exercises) == null) {
                state.value = RoutineEditorUiState.Missing
                return@launch
            }
            state.value = RoutineEditorUiState.Done
        }
    }

    private suspend fun exerciseForm(exercise: RoutineExercise): ExerciseForm {
        val template = templates.get(exercise.exerciseTemplateId)
        val columns = columnsFor(template?.type ?: ExerciseType.WEIGHT_REPS)
        return ExerciseForm(
            key = newKey(),
            name = template?.title.orEmpty(),
            columns = columns,
            base = exercise,
            rest = exercise.restSeconds?.toString().orEmpty(),
            sets = exercise.sets.map { SetForm(newKey(), it) },
        )
    }

    private fun newKey() = "k${nextKey++}"

    private fun exercise(key: String, transform: (ExerciseForm) -> ExerciseForm) =
        edit { f -> f.copy(exercises = f.exercises.map { if (it.key == key) transform(it) else it }) }

    private fun set(exerciseKey: String, setKey: String, transform: (SetForm) -> SetForm) =
        exercise(exerciseKey) { e -> e.copy(sets = e.sets.map { if (it.key == setKey) transform(it) else it }) }

    private fun edit(transform: (Form) -> Form) {
        if (state.value !is RoutineEditorUiState.Ready) return
        val current = form.value ?: return
        publish(transform(current))
    }

    private fun publish(next: Form) {
        form.value = next
        val previous = state.value as? RoutineEditorUiState.Ready
        state.value = RoutineEditorUiState.Ready(
            isNew = routineId == null,
            title = next.title,
            exercises = next.exercises.map { it.toUi(next.unit) },
            weightUnit = next.unit,
            showErrors = previous?.showErrors ?: false,
            confirmDelete = previous?.confirmDelete ?: false,
        )
    }

    private fun ready(transform: (RoutineEditorUiState.Ready) -> RoutineEditorUiState.Ready) =
        state.update { if (it is RoutineEditorUiState.Ready) transform(it) else it }

    private data class Form(val title: String, val exercises: List<ExerciseForm>, val unit: WeightUnit)

    /** [base] keeps the superset, notes, and values of columns this type does not show, so saving does not drop them. */
    private data class ExerciseForm(
        val key: String,
        val name: String,
        val columns: List<SetColumn>,
        val base: RoutineExercise,
        val rest: String,
        val sets: List<SetForm>,
    ) {
        fun toUi(unit: WeightUnit): RoutineExerciseUi {
            var workingSets = 0
            return RoutineExerciseUi(
                key = key,
                name = name,
                columns = columns,
                rest = rest,
                restInvalid = parseRest(rest) == RestInput.Invalid,
                sets = sets.map { set ->
                    // Warm-ups show a marker and do not take a set number, as in the live workout.
                    if (set.base.type != SetType.WARMUP) workingSets++
                    PlannedSetUi(
                        key = set.key,
                        number = if (set.base.type == SetType.WARMUP) workingSets + 1 else workingSets,
                        type = set.base.type,
                        values = set.values(columns, unit),
                    )
                },
            )
        }

        fun toRoutineExercise(unit: WeightUnit) = base.copy(
            restSeconds = (parseRest(rest) as RestInput.Valid).seconds,
            sets = sets.map { it.toPlannedSet(unit) },
        )
    }

    /**
     * [edits] holds exactly what was typed per column, so a field never reformats under the cursor;
     * untouched columns keep the stored value, so a kg value shown rounded in lb is not changed.
     */
    private data class SetForm(val key: String, val base: PlannedSet, val edits: Map<SetColumn, String> = emptyMap()) {
        fun values(columns: List<SetColumn>, unit: WeightUnit): List<String> {
            val data = base.toSetData()
            return columns.map { edits[it] ?: it.format(data, unit) }
        }

        fun toPlannedSet(unit: WeightUnit): PlannedSet {
            val data = edits.entries.fold(base.toSetData()) { data, (column, text) -> column.parseInto(data, text, unit) }
            return PlannedSet(type = base.type, weightKg = data.weightKg, reps = data.reps, durationS = data.durationS, distanceM = data.distanceM)
        }
    }
}

private sealed interface RestInput {
    data class Valid(val seconds: Int?) : RestInput

    data object Invalid : RestInput
}

/** Empty is the default rest (null), 0 is off, as in a workout exercise (D-052). */
private fun parseRest(text: String): RestInput {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return RestInput.Valid(null)
    return trimmed.toIntOrNull()?.takeIf { it >= 0 }?.let { RestInput.Valid(it) } ?: RestInput.Invalid
}

private fun PlannedSet.toSetData() = SetData(type = type, weightKg = weightKg, reps = reps, durationS = durationS, distanceM = distanceM)
