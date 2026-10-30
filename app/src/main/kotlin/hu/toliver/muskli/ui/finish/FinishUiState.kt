package hu.toliver.muskli.ui.finish

sealed interface FinishUiState {
    data object Loading : FinishUiState

    /** [hours] and [minutes] are the typed duration; [editingPast] is true when saving an edited past workout. */
    data class Ready(
        val title: String,
        val notes: String,
        val startedAtEpochMs: Long,
        val hours: String,
        val minutes: String,
        val durationValid: Boolean,
        val editingPast: Boolean,
        val dialog: FinishDialog? = null,
    ) : FinishUiState

    /** Saved or discarded; the screen leaves. */
    data class Done(val outcome: FinishOutcome) : FinishUiState
}

sealed interface FinishDialog {
    /** D-011: uncompleted sets are discarded only after this confirmation. */
    data class DiscardSets(val count: Int) : FinishDialog

    /** Nothing is completed, so there is nothing to save. */
    data object DiscardWorkout : FinishDialog
}

sealed interface FinishOutcome {
    val workoutId: String

    data class Saved(override val workoutId: String) : FinishOutcome

    /** The workout was soft-deleted, so it can still be undone. */
    data class Discarded(override val workoutId: String) : FinishOutcome
}

sealed interface FinishEvent {
    data class TitleChanged(val text: String) : FinishEvent
    data class NotesChanged(val text: String) : FinishEvent
    data class StartChanged(val epochMs: Long) : FinishEvent
    data class HoursChanged(val text: String) : FinishEvent
    data class MinutesChanged(val text: String) : FinishEvent
    data object SaveClicked : FinishEvent
    data object DiscardSetsConfirmed : FinishEvent
    data object DiscardWorkoutConfirmed : FinishEvent
    data object DialogDismissed : FinishEvent
}
