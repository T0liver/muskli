package hu.toliver.muskli.domain.importer.hevy

import hu.toliver.muskli.domain.library.normalizeForSearch
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.SetData

/** How a Hevy exercise title resolved against the templates on the device (D-043). */
sealed interface HevyMatch {
    data class Exact(val templateId: String) : HevyMatch

    data class Normalised(val templateId: String) : HevyMatch

    /** [candidateIds] are the tied templates, offered first in the mapping UI; empty when nothing matched. */
    data class Unmatched(val candidateIds: List<String>) : HevyMatch
}

/** What the user chose for one Hevy title. */
sealed interface HevyResolution {
    data class UseTemplate(val templateId: String) : HevyResolution

    data object CreateCustom : HevyResolution
}

/**
 * Resolves every title against all [templates], archived included (docs/HEVY-IMPORT.md §3).
 * No fuzzy matching: a wrong automatic match silently corrupts history, a manual one does not.
 */
fun matchHevyExercises(titles: Collection<String>, templates: List<ExerciseTemplate>): Map<String, HevyMatch> {
    val byTitle = templates.groupBy { normalizeForSearch(it.title) }
    val byTokens = templates.groupBy { titleTokens(it.title) }
    return titles.associateWith { title ->
        val exact = byTitle[normalizeForSearch(title)].orEmpty()
        val candidates = exact.ifEmpty { byTokens[titleTokens(title)].orEmpty() }
        val preferred = candidates.filterNot { it.archived }.ifEmpty { candidates }
        when {
            preferred.size != 1 -> HevyMatch.Unmatched(preferred.map { it.id })
            exact.isNotEmpty() -> HevyMatch.Exact(preferred.single().id)
            else -> HevyMatch.Normalised(preferred.single().id)
        }
    }
}

private val apostrophes = Regex("['’]")

/** Sorted token multiset, so word order and punctuation do not matter ("A-B (C)" equals "C A B"). */
private fun titleTokens(title: String): List<String> =
    apostrophes.replace(normalizeForSearch(title), "")
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .split(' ')
        .filter { it.isNotEmpty() }
        .sorted()

/** In order of precedence when a title has several. */
private val equipmentTokens = listOf(
    "barbell" to Equipment.BARBELL,
    "dumbbell" to Equipment.DUMBBELL,
    "kettlebell" to Equipment.KETTLEBELL,
    "machine" to Equipment.MACHINE,
    "cable" to Equipment.MACHINE,
    "smith" to Equipment.MACHINE,
    "band" to Equipment.RESISTANCE_BAND,
    "plate" to Equipment.PLATE,
    "suspension" to Equipment.SUSPENSION,
    "trx" to Equipment.SUSPENSION,
    "bodyweight" to Equipment.NONE,
)

/**
 * Type and equipment for a new custom exercise from its title and all of its [sets] in the file.
 * Reps alone give REPS_ONLY rather than a body-weight type, so no volume is invented.
 */
fun inferCustomExercise(title: String, sets: List<SetData>): Pair<ExerciseType, Equipment> {
    val tokens = titleTokens(title)
    val weight = sets.any { it.weightKg != null }
    val distance = sets.any { it.distanceM != null }
    val duration = sets.any { it.durationS != null }
    val reps = sets.any { it.reps != null }
    val type = when {
        distance && weight -> ExerciseType.SHORT_DISTANCE_WEIGHT
        distance -> ExerciseType.DISTANCE_DURATION
        duration && weight -> ExerciseType.WEIGHT_DURATION
        duration -> ExerciseType.DURATION
        weight && "assisted" in tokens -> ExerciseType.BODYWEIGHT_ASSISTED_REPS
        weight -> ExerciseType.WEIGHT_REPS
        reps -> ExerciseType.REPS_ONLY
        else -> ExerciseType.WEIGHT_REPS
    }
    val equipment = equipmentTokens.firstOrNull { (token, _) -> token in tokens }?.second
        ?: if (weight) Equipment.OTHER else Equipment.NONE
    return type to equipment
}

/** Matched titles use their template; unmatched ones default to a new custom exercise (F-09 AC2). */
fun defaultResolutions(matches: Map<String, HevyMatch>): Map<String, HevyResolution> =
    matches.mapValues { (_, match) ->
        when (match) {
            is HevyMatch.Exact -> HevyResolution.UseTemplate(match.templateId)
            is HevyMatch.Normalised -> HevyResolution.UseTemplate(match.templateId)
            is HevyMatch.Unmatched -> HevyResolution.CreateCustom
        }
    }
