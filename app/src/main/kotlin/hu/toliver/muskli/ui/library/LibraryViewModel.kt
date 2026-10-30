package hu.toliver.muskli.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.toliver.muskli.domain.library.ExerciseFilter
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.usecase.library.ArchiveCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.library.CreateCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.library.CustomExerciseInput
import hu.toliver.muskli.domain.usecase.library.EditCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.library.SearchExercisesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: ExerciseTemplateRepository,
    private val search: SearchExercisesUseCase,
    private val createCustom: CreateCustomExerciseUseCase,
    private val editCustom: EditCustomExerciseUseCase,
    private val archiveCustom: ArchiveCustomExerciseUseCase,
) : ViewModel() {
    private val filter = MutableStateFlow(ExerciseFilter())
    private val selection = MutableStateFlow<List<String>>(emptyList())
    private val editor = MutableStateFlow<ExerciseEditorState?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        filter,
        filter.flatMapLatest { search(it) },
        repository.observeRecentlyUsed(RECENTS_LIMIT),
        selection,
        editor,
    ) { filter, results, recents, selected, editor ->
        val unfiltered = filter.query.isBlank() && filter.muscle == null && filter.equipment == null
        LibraryUiState(
            query = filter.query,
            muscle = filter.muscle,
            equipment = filter.equipment,
            recents = if (unfiltered) recents else emptyList(),
            results = results,
            selectedIds = selected,
            editor = editor,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryUiState())

    fun onEvent(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.QueryChanged -> filter.update { it.copy(query = event.query) }
            is LibraryEvent.MuscleFilterChanged -> filter.update { it.copy(muscle = event.muscle) }
            is LibraryEvent.EquipmentFilterChanged -> filter.update { it.copy(equipment = event.equipment) }
            is LibraryEvent.SelectionToggled -> selection.update { if (event.id in it) it - event.id else it + event.id }
            LibraryEvent.CreateClicked -> editor.value = ExerciseEditorState()
            is LibraryEvent.EditClicked -> viewModelScope.launch {
                val template = repository.get(event.id)?.takeIf { it.isCustom } ?: return@launch
                editor.value = ExerciseEditorState(
                    editingId = template.id,
                    title = template.title,
                    type = template.type,
                    primaryMuscle = template.primaryMuscle,
                    secondaryMuscles = template.secondaryMuscles,
                    equipment = template.equipment,
                )
            }
            is LibraryEvent.EditorTitleChanged -> editor.update { it?.copy(title = event.title) }
            is LibraryEvent.EditorTypeChanged -> editor.update { it?.copy(type = event.type) }
            is LibraryEvent.EditorPrimaryMuscleChanged -> editor.update {
                it?.copy(primaryMuscle = event.muscle, secondaryMuscles = it.secondaryMuscles - event.muscle)
            }
            is LibraryEvent.EditorSecondaryMuscleToggled -> editor.update {
                it?.copy(secondaryMuscles = if (event.muscle in it.secondaryMuscles) it.secondaryMuscles - event.muscle else it.secondaryMuscles + event.muscle)
            }
            is LibraryEvent.EditorEquipmentChanged -> editor.update { it?.copy(equipment = event.equipment) }
            LibraryEvent.EditorSave -> save()
            LibraryEvent.EditorArchive -> archive()
            LibraryEvent.EditorDismiss -> editor.value = null
        }
    }

    private fun save() {
        val form = editor.value ?: return
        val primary = form.primaryMuscle
        val equipment = form.equipment
        if (form.titleMissing || primary == null || equipment == null) {
            editor.value = form.copy(showErrors = true)
            return
        }
        val input = CustomExerciseInput(form.title, form.type, primary, form.secondaryMuscles, equipment)
        viewModelScope.launch {
            if (form.editingId == null) {
                // A new exercise made from the picker is almost always the one the user wants to add.
                val created = createCustom(input)
                selection.update { it + created.id }
            } else {
                editCustom(form.editingId, input)
            }
            editor.value = null
        }
    }

    private fun archive() {
        val id = editor.value?.editingId ?: return
        viewModelScope.launch {
            archiveCustom(id)
            selection.update { it - id }
            editor.value = null
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val RECENTS_LIMIT = 10
    }
}
