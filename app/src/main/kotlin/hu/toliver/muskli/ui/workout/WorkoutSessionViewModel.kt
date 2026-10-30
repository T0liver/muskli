package hu.toliver.muskli.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.toliver.muskli.domain.calc.ExerciseSets
import hu.toliver.muskli.domain.calc.completedSetCount
import hu.toliver.muskli.domain.calc.kgToLb
import hu.toliver.muskli.domain.calc.sessionVolumeKg
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.repo.WorkoutRepository
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.domain.usecase.workout.AddExercisesUseCase
import hu.toliver.muskli.domain.usecase.workout.AddSetUseCase
import hu.toliver.muskli.domain.usecase.workout.ChangeSetTypeUseCase
import hu.toliver.muskli.domain.usecase.workout.CompleteSetUseCase
import hu.toliver.muskli.domain.usecase.workout.DeleteSetUseCase
import hu.toliver.muskli.domain.usecase.workout.EditSetValuesUseCase
import hu.toliver.muskli.domain.usecase.workout.PreviousSetsUseCase
import hu.toliver.muskli.domain.usecase.workout.RemoveExerciseUseCase
import hu.toliver.muskli.domain.usecase.workout.ReorderExerciseUseCase
import hu.toliver.muskli.domain.usecase.workout.ReplaceExerciseUseCase
import hu.toliver.muskli.domain.usecase.workout.SetValues
import hu.toliver.muskli.domain.usecase.workout.StartWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.UncompleteSetUseCase
import hu.toliver.muskli.ui.components.SetColumn
import hu.toliver.muskli.ui.components.WorkoutExerciseUi
import hu.toliver.muskli.ui.components.WorkoutSetUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.NumberFormat
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * The live workout, built from the persisted draft (ARCHITECTURE §4). Every event writes through
 * a T-012 use case at once, so killing the app loses nothing (AC-03).
 */
@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    workouts: WorkoutRepository,
    templates: ExerciseTemplateRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val startWorkout: StartWorkoutUseCase,
    private val addExercises: AddExercisesUseCase,
    private val replaceExercise: ReplaceExerciseUseCase,
    private val removeExercise: RemoveExerciseUseCase,
    private val reorderExercise: ReorderExerciseUseCase,
    private val addSet: AddSetUseCase,
    private val deleteSet: DeleteSetUseCase,
    private val changeSetType: ChangeSetTypeUseCase,
    private val editSetValues: EditSetValuesUseCase,
    private val completeSet: CompleteSetUseCase,
    private val uncompleteSet: UncompleteSetUseCase,
    private val previousSets: PreviousSetsUseCase,
) : ViewModel() {
    private val draft: StateFlow<Draft> = workouts.observeActiveDraft()
        .map<Workout?, Draft> { Draft.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Draft.Loading)
    private val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    private val local = MutableStateFlow(Local())
    private val previous = MutableStateFlow(PreviousCache())

    // Edits run one at a time in tap order: each replaces whole values, so a later write must not
    // be overtaken by an earlier one.
    private val writes = Mutex()

    private val ticks = flow {
        while (true) {
            emit(Unit)
            delay(TICK_MS)
        }
    }

    val uiState: StateFlow<LiveWorkoutUiState> = combine(
        combine(draft, previous, ::Pair),
        templates.observeAll().map { all -> all.associateBy(ExerciseTemplate::id) },
        settingsRepository.settings,
        local,
        ticks,
    ) { (draft, previous), templatesById, settings, local, _ ->
        when (draft) {
            Draft.Loading -> LiveWorkoutUiState.Loading
            is Draft.Loaded -> draft.workout?.let { toUiState(it, templatesById, settings, local, previous) } ?: LiveWorkoutUiState.NoWorkout
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LiveWorkoutUiState.Loading)

    init {
        // History changes only when a workout is finished, so each template is read once per draft.
        viewModelScope.launch {
            draft.collect { state ->
                val workout = (state as? Draft.Loaded)?.workout ?: return@collect
                if (previous.value.workoutId != workout.id) previous.value = PreviousCache(workout.id)
                workout.exercises.map { it.exerciseTemplateId }.distinct()
                    .filterNot { it in previous.value.byTemplate }
                    .forEach { templateId ->
                        val sets = previousSets(templateId)
                        previous.update {
                            if (it.workoutId == workout.id) it.copy(byTemplate = it.byTemplate + (templateId to sets)) else it
                        }
                    }
            }
        }
        // Typed text is in the old unit; show the stored values converted instead.
        viewModelScope.launch {
            settingsRepository.settings.map { it.weightUnit }.distinctUntilChanged().drop(1).collect {
                local.update { it.copy(textEdits = emptyMap()) }
            }
        }
    }

    fun onEvent(event: LiveWorkoutEvent) {
        when (event) {
            LiveWorkoutEvent.StartClicked -> write { startWorkout() }
            is LiveWorkoutEvent.ExercisesPicked -> edit { addExercises(it, event.templateIds) }
            is LiveWorkoutEvent.ReplacementPicked -> edit { replaceExercise(it, event.workoutExerciseId, event.templateId) }
            is LiveWorkoutEvent.ExerciseMenuClicked -> local.update { it.copy(exerciseMenuFor = event.workoutExerciseId) }
            LiveWorkoutEvent.ExerciseMenuDismissed -> local.update { it.copy(exerciseMenuFor = null) }
            is LiveWorkoutEvent.RemoveClicked -> {
                local.update { it.copy(exerciseMenuFor = null) }
                edit { removeExercise(it, event.workoutExerciseId) }
            }
            LiveWorkoutEvent.ReorderClicked -> local.update { it.copy(exerciseMenuFor = null, reorderOpen = true) }
            is LiveWorkoutEvent.ExerciseMoved -> edit { reorderExercise(it, event.workoutExerciseId, event.toIndex) }
            LiveWorkoutEvent.ReorderDismissed -> local.update { it.copy(reorderOpen = false) }
            is LiveWorkoutEvent.AddSetClicked -> edit { addSet(it, event.workoutExerciseId) }
            is LiveWorkoutEvent.SetDeleted -> edit { deleteSet(it, event.setId) }
            is LiveWorkoutEvent.SetTypeClicked -> local.update { it.copy(setTypeMenuFor = event.setId) }
            is LiveWorkoutEvent.SetTypeChosen -> {
                local.update { it.copy(setTypeMenuFor = null) }
                edit { changeSetType(it, event.setId, event.type) }
            }
            LiveWorkoutEvent.SetTypeMenuDismissed -> local.update { it.copy(setTypeMenuFor = null) }
            is LiveWorkoutEvent.ValueChanged -> valueChanged(event)
            // The rest effect is ignored until the timer exists (T-015).
            is LiveWorkoutEvent.CompletedChanged -> edit {
                if (event.completed) completeSet(it, event.setId, settings.value.defaultRestSeconds) else uncompleteSet(it, event.setId)
            }
        }
    }

    private fun valueChanged(event: LiveWorkoutEvent.ValueChanged) {
        val edits = local.updateAndGet {
            it.copy(textEdits = it.textEdits + (FieldKey(event.setId, event.column) to event.text))
        }.textEdits
        val workout = (draft.value as? Draft.Loaded)?.workout ?: return
        val set = workout.exercises.flatMap { it.sets }.firstOrNull { it.id == event.setId } ?: return
        // Every typed field of the set, not only this one: the draft may not show the last write yet.
        val unit = settings.value.weightUnit
        val data = edits.entries
            .filter { it.key.setId == event.setId }
            .fold(set.data) { data, (key, text) -> key.column.parseInto(data, text, unit) }
        val values = SetValues(weightKg = data.weightKg, reps = data.reps, durationS = data.durationS, distanceM = data.distanceM)
        edit { editSetValues(it, event.setId, values) }
    }

    private fun edit(block: suspend (workoutId: String) -> Unit) = write {
        val workoutId = (draft.value as? Draft.Loaded)?.workout?.id ?: return@write
        block(workoutId)
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { writes.withLock { block() } }
    }

    private fun toUiState(
        workout: Workout,
        templatesById: Map<String, ExerciseTemplate>,
        settings: Settings,
        local: Local,
        previous: PreviousCache,
    ): LiveWorkoutUiState.Active {
        val unit = settings.weightUnit
        val types = workout.exercises.map { templatesById[it.exerciseTemplateId]?.type ?: ExerciseType.WEIGHT_REPS }
        val exerciseSets = workout.exercises.mapIndexed { i, exercise -> ExerciseSets(types[i], exercise.sets.map { it.data }) }
        val volumeKg = sessionVolumeKg(exerciseSets, settings.bodyWeightKg)
        val elapsedS = ((clock.nowEpochMs() - workout.startedAtEpochMs) / 1000).coerceAtLeast(0)
        return LiveWorkoutUiState.Active(
            workoutId = workout.id,
            duration = formatDuration(elapsedS),
            volume = NumberFormat.getIntegerInstance().format((if (unit == WeightUnit.LB) kgToLb(volumeKg) else volumeKg).roundToLong()),
            completedSets = completedSetCount(exerciseSets),
            weightUnit = unit,
            exercises = workout.exercises.mapIndexed { i, exercise ->
                val columns = columnsFor(types[i])
                val lastTime = previous.byTemplate[exercise.exerciseTemplateId].orEmpty()
                var workingSets = 0
                WorkoutExerciseUi(
                    id = exercise.id,
                    name = templatesById[exercise.exerciseTemplateId]?.title.orEmpty(),
                    columns = columns,
                    sets = exercise.sets.mapIndexed { index, set ->
                        // Warm-ups show a marker and do not take a set number.
                        if (set.data.type != SetType.WARMUP) workingSets++
                        WorkoutSetUi(
                            id = set.id,
                            number = if (set.data.type == SetType.WARMUP) workingSets + 1 else workingSets,
                            type = set.data.type,
                            previous = lastTime.getOrNull(index)?.let { prev -> columns.map { it.format(prev, unit) } },
                            values = columns.map { local.textEdits[FieldKey(set.id, it)] ?: it.format(set.data, unit) },
                            completed = set.data.completed,
                        )
                    },
                )
            },
            keepScreenAwake = settings.keepScreenAwake,
            exerciseMenuFor = local.exerciseMenuFor,
            setTypeMenuFor = local.setTypeMenuFor,
            reorderOpen = local.reorderOpen,
        )
    }

    private sealed interface Draft {
        data object Loading : Draft

        data class Loaded(val workout: Workout?) : Draft
    }

    private data class FieldKey(val setId: String, val column: SetColumn)

    /** Last time's sets per template, for the draft [workoutId] only. */
    private data class PreviousCache(
        val workoutId: String? = null,
        val byTemplate: Map<String, List<SetData?>> = emptyMap(),
    )

    /** Screen-only state. [textEdits] holds exactly what was typed, so a field never reformats under the cursor. */
    private data class Local(
        val textEdits: Map<FieldKey, String> = emptyMap(),
        val exerciseMenuFor: String? = null,
        val setTypeMenuFor: String? = null,
        val reorderOpen: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val TICK_MS = 1_000L
    }
}
