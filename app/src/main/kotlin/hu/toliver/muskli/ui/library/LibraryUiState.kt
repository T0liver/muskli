package hu.toliver.muskli.ui.library

import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup

/** [BROWSE] edits custom exercises on tap; [PICK] toggles a multi-selection for "Add exercise". */
enum class LibraryMode { BROWSE, PICK }

data class LibraryUiState(
    val query: String = "",
    val muscle: MuscleGroup? = null,
    val equipment: Equipment? = null,
    /** When set, [results] are archived custom exercises only. */
    val showArchived: Boolean = false,
    /** Empty while a query or filter is active, so the list shows only matches. */
    val recents: List<ExerciseTemplate> = emptyList(),
    val results: List<ExerciseTemplate> = emptyList(),
    /** In tap order, so picked exercises are added in the order chosen. */
    val selectedIds: List<String> = emptyList(),
    val editor: ExerciseEditorState? = null,
)

/** The create/edit form. [editingId] is null when creating. */
data class ExerciseEditorState(
    val editingId: String? = null,
    val title: String = "",
    val type: ExerciseType = ExerciseType.WEIGHT_REPS,
    val primaryMuscle: MuscleGroup? = null,
    val secondaryMuscles: Set<MuscleGroup> = emptySet(),
    val equipment: Equipment? = null,
    /** Set after a save attempt with missing fields, so errors don't show before the user has tried. */
    val showErrors: Boolean = false,
) {
    val titleMissing get() = title.isBlank()
    val primaryMuscleMissing get() = primaryMuscle == null
    val equipmentMissing get() = equipment == null
}

sealed interface LibraryEvent {
    data class QueryChanged(val query: String) : LibraryEvent
    data class MuscleFilterChanged(val muscle: MuscleGroup?) : LibraryEvent
    data class EquipmentFilterChanged(val equipment: Equipment?) : LibraryEvent
    data class ShowArchivedChanged(val show: Boolean) : LibraryEvent
    data class RestoreClicked(val id: String) : LibraryEvent
    data class SelectionToggled(val id: String) : LibraryEvent
    data object CreateClicked : LibraryEvent
    data class EditClicked(val id: String) : LibraryEvent
    data class EditorTitleChanged(val title: String) : LibraryEvent
    data class EditorTypeChanged(val type: ExerciseType) : LibraryEvent
    data class EditorPrimaryMuscleChanged(val muscle: MuscleGroup) : LibraryEvent
    data class EditorSecondaryMuscleToggled(val muscle: MuscleGroup) : LibraryEvent
    data class EditorEquipmentChanged(val equipment: Equipment) : LibraryEvent
    data object EditorSave : LibraryEvent
    data object EditorArchive : LibraryEvent
    data object EditorDismiss : LibraryEvent
}
