package hu.toliver.muskli.domain.repo

/** The app's own one-file JSON backup (SPEC F-08). */
interface BackupRepository {
    /** Every workout (draft and soft-deleted included), custom exercises, the catalogue exercises they use, and settings. */
    suspend fun export(): String

    /**
     * Replaces all workouts and custom exercises with the file's, adds missing catalogue exercises, and restores
     * settings. Validates everything first; on any error nothing is applied.
     */
    suspend fun import(json: String): ImportResult
}

sealed interface ImportResult {
    data class Imported(val workouts: Int, val customExercises: Int) : ImportResult

    data class Rejected(val errors: List<ImportError>) : ImportResult
}

/** [where] is a JSON path such as `workouts[2].exercises[0].sets[1].type`, or the parser's message for [ImportProblem.MALFORMED]. */
data class ImportError(val problem: ImportProblem, val where: String)

enum class ImportProblem {
    /** Not JSON, or a field is missing or has the wrong type. */
    MALFORMED,

    /** `schemaVersion` is newer than this app understands. */
    UNSUPPORTED_VERSION,

    /** Unknown enum value, negative number, or an otherwise impossible value. */
    INVALID_VALUE,

    /** The same id appears twice in the file. */
    DUPLICATE_ID,

    /** A workout uses an exercise the file does not contain. */
    UNKNOWN_EXERCISE,

    /** More than one unfinished, non-deleted workout. */
    MULTIPLE_DRAFTS,

    /** A custom exercise in the file has the id of a catalogue exercise on this device. */
    ID_CONFLICT,
}
