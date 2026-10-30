package hu.toliver.muskli

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/**
 * Screens of the hand-rolled back stack (no navigation library, owner choice in T-017). A null
 * `editId` means the live workout; otherwise the past workout being edited.
 */
sealed interface Route {
    data object Home : Route
    data object Workout : Route
    data object Library : Route
    data object Settings : Route
    data object History : Route
    data class AddExercises(val editId: String?) : Route
    data class ReplaceExercise(val editId: String?, val workoutExerciseId: String) : Route

    /** [entry] is unique per opening, so each Finish gets a fresh form. */
    data class Finish(val workoutId: String, val entry: Int) : Route
    data class WorkoutDetail(val workoutId: String) : Route
    data class EditWorkout(val workoutId: String, val isNew: Boolean) : Route
    data class ExerciseHistory(val exerciseTemplateId: String) : Route
}

// Ids are UUIDs or catalogue slugs, so the unit separator never occurs in them.
private const val SEP = "\u001F"

private fun Route.encode(): String = when (this) {
    Route.Home -> "home"
    Route.Workout -> "workout"
    Route.Library -> "library"
    Route.Settings -> "settings"
    Route.History -> "history"
    is Route.AddExercises -> listOf("add", editId.orEmpty()).joinToString(SEP)
    is Route.ReplaceExercise -> listOf("replace", editId.orEmpty(), workoutExerciseId).joinToString(SEP)
    is Route.Finish -> listOf("finish", workoutId, entry.toString()).joinToString(SEP)
    is Route.WorkoutDetail -> listOf("detail", workoutId).joinToString(SEP)
    is Route.EditWorkout -> listOf("edit", workoutId, isNew.toString()).joinToString(SEP)
    is Route.ExerciseHistory -> listOf("exercise", exerciseTemplateId).joinToString(SEP)
}

private fun decodeRoute(encoded: String): Route {
    val parts = encoded.split(SEP)
    return when (parts[0]) {
        "workout" -> Route.Workout
        "library" -> Route.Library
        "settings" -> Route.Settings
        "history" -> Route.History
        "add" -> Route.AddExercises(parts[1].ifEmpty { null })
        "replace" -> Route.ReplaceExercise(parts[1].ifEmpty { null }, parts[2])
        "finish" -> Route.Finish(parts[1], parts[2].toInt())
        "detail" -> Route.WorkoutDetail(parts[1])
        "edit" -> Route.EditWorkout(parts[1], parts[2].toBoolean())
        "exercise" -> Route.ExerciseHistory(parts[1])
        else -> Route.Home
    }
}

val RouteStackSaver: Saver<List<Route>, Any> = listSaver(
    save = { stack -> stack.map { it.encode() } },
    restore = { encoded -> encoded.map(::decodeRoute) },
)
