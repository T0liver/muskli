package hu.toliver.muskli.domain.library

import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.MuscleGroup
import java.text.Normalizer
import java.util.Locale

/** Library query. Every set criterion must match (AND); [muscle] matches the primary muscle only. */
data class ExerciseFilter(
    val query: String = "",
    val muscle: MuscleGroup? = null,
    val equipment: Equipment? = null,
    val includeArchived: Boolean = false,
)

/** [templates] matching [filter], sorted by title ignoring case and diacritics. */
fun searchExercises(templates: List<ExerciseTemplate>, filter: ExerciseFilter): List<ExerciseTemplate> {
    val query = normalizeForSearch(filter.query)
    return templates
        .filter { filter.includeArchived || !it.archived }
        .filter { filter.muscle == null || it.primaryMuscle == filter.muscle }
        .filter { filter.equipment == null || it.equipment == filter.equipment }
        .map { normalizeForSearch(it.title) to it }
        .filter { (title, _) -> title.contains(query) }
        .sortedBy { (title, _) -> title }
        .map { (_, template) -> template }
}

private val combiningMarks = Regex("\\p{Mn}+")

/** Lowercase, trimmed, with diacritics removed ("Bíceps Cúrl" becomes "biceps curl"). */
fun normalizeForSearch(text: String): String =
    combiningMarks.replace(Normalizer.normalize(text.trim(), Normalizer.Form.NFD), "").lowercase(Locale.ROOT)
