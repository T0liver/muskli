package hu.toliver.muskli.ui.hevy

import hu.toliver.muskli.domain.importer.hevy.HevyParseError
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.settings.WeightUnit

sealed interface HevyImportUiState {
    data object Pick : HevyImportUiState

    /** Reading the file or writing the import. */
    data object Busy : HevyImportUiState

    /** Nothing was changed. */
    data class Rejected(val reason: HevyRejection) : HevyImportUiState

    data class Preview(
        val summary: HevyImportSummary,
        /** One row per distinct Hevy title, unmatched first (F-09 AC2). */
        val exercises: List<HevyExerciseRow>,
        val mapping: HevyMappingDialog? = null,
        /** Null until a measurements file is added. */
        val measurements: HevyMeasurements? = null,
    ) : HevyImportUiState {
        val canImport: Boolean get() = summary.newWorkouts > 0
    }

    /** [bodyWeight] is set only when the import wrote it. */
    data class Done(
        val workouts: Int,
        val newCustomExercises: Int,
        val duplicatesSkipped: Int,
        val bodyWeight: HevyWeight? = null,
    ) : HevyImportUiState

    /** Writing failed; the transaction rolled back, so nothing was changed. */
    data object Failed : HevyImportUiState
}

sealed interface HevyRejection {
    data class Errors(val errors: List<HevyParseError>) : HevyRejection

    data object MeasurementsFile : HevyRejection

    data object ReadFailed : HevyRejection
}

/** What the added measurements file offers (D-045). */
sealed interface HevyMeasurements {
    data class BodyWeight(val weight: HevyWeight, val measuredAtEpochMs: Long, val checked: Boolean) : HevyMeasurements

    data object NoBodyWeight : HevyMeasurements

    data object NotMeasurementsFile : HevyMeasurements

    data object ReadFailed : HevyMeasurements
}

/** A weight already converted to [unit] and formatted for display. */
data class HevyWeight(val value: String, val unit: WeightUnit)

data class HevyImportSummary(
    val workoutsInFile: Int,
    val newWorkouts: Int,
    val duplicatesSkipped: Int,
    val sets: Int,
    val rpeDropped: Int,
    val firstStartEpochMs: Long?,
    val lastStartEpochMs: Long?,
    val newCustomExercises: Int,
)

data class HevyExerciseRow(val hevyTitle: String, val resolution: HevyRowResolution, val unmatched: Boolean)

sealed interface HevyRowResolution {
    /** [check] marks an automatic normalised match the user should look at (D-043). */
    data class Library(val title: String, val check: Boolean) : HevyRowResolution

    data class NewCustom(val type: ExerciseType) : HevyRowResolution
}

data class HevyMappingDialog(
    val hevyTitle: String,
    val query: String,
    val createCustomSelected: Boolean,
    /** Tied candidates from matching, shown above the search results. */
    val candidates: List<HevyMappingOption>,
    val results: List<HevyMappingOption>,
)

data class HevyMappingOption(val templateId: String, val title: String, val selected: Boolean)

sealed interface HevyImportEvent {
    data class FilePicked(val uri: String) : HevyImportEvent
    data class RowClicked(val hevyTitle: String) : HevyImportEvent
    data class QueryChanged(val query: String) : HevyImportEvent
    data object CreateCustomChosen : HevyImportEvent
    data class TemplateChosen(val templateId: String) : HevyImportEvent
    data object MappingDismissed : HevyImportEvent
    data object ImportClicked : HevyImportEvent
    data class MeasurementsFilePicked(val uri: String) : HevyImportEvent
    data class BodyWeightToggled(val checked: Boolean) : HevyImportEvent
}
