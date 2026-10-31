package hu.toliver.muskli.domain.usecase.importer

import hu.toliver.muskli.domain.importer.hevy.HevyBodyWeight
import hu.toliver.muskli.domain.importer.hevy.HevyImportPlan
import hu.toliver.muskli.domain.importer.hevy.HevyMatch
import hu.toliver.muskli.domain.importer.hevy.HevyParseError
import hu.toliver.muskli.domain.importer.hevy.HevyParseResult
import hu.toliver.muskli.domain.importer.hevy.HevyResolution
import hu.toliver.muskli.domain.importer.hevy.HevyWorkout
import hu.toliver.muskli.domain.importer.hevy.defaultResolutions
import hu.toliver.muskli.domain.importer.hevy.matchHevyExercises
import hu.toliver.muskli.domain.importer.hevy.parseHevyExport
import hu.toliver.muskli.domain.importer.hevy.planHevyImport
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.HevyImportRepository
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.repo.SettingsRepository

sealed interface HevyPreview {
    data class Rejected(val errors: List<HevyParseError>) : HevyPreview

    data class Measurements(val latestBodyWeight: HevyBodyWeight?) : HevyPreview

    data class Ready(
        val workouts: List<HevyWorkout>,
        val matches: Map<String, HevyMatch>,
        val defaultResolutions: Map<String, HevyResolution>,
        val plan: HevyImportPlan,
    ) : HevyPreview {
        /**
         * New custom exercises [resolutions] would create for the workouts [plan] imports, so the
         * preview can recount after a mapping change without re-reading the device (D-059). A missing
         * resolution counts as its default, as in apply.
         */
        fun newCustomCount(resolutions: Map<String, HevyResolution>): Int {
            // Duplicate status depends only on the start minute (D-044), so the plan's minutes pick the imported workouts.
            val importedMinutes = plan.newWorkouts.mapTo(HashSet()) { Math.floorDiv(it.startedAtEpochMs, MINUTE_MS) }
            return workouts
                .filter { Math.floorDiv(it.startedAtEpochMs, MINUTE_MS) in importedMinutes }
                .flatMapTo(LinkedHashSet()) { w -> w.exercises.map { it.title } }
                .count { (resolutions[it] ?: defaultResolutions[it]) == HevyResolution.CreateCustom }
        }
    }
}

private const val MINUTE_MS = 60_000L

/** Parses, matches, and plans with the default resolutions; writes nothing. */
class PreviewHevyImportUseCase(
    private val repository: HevyImportRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(text: String): HevyPreview =
        when (val parsed = parseHevyExport(text, clock)) {
            is HevyParseResult.Rejected -> HevyPreview.Rejected(parsed.errors)
            is HevyParseResult.Measurements -> HevyPreview.Measurements(parsed.latestBodyWeight)
            is HevyParseResult.Workouts -> {
                val matches = matchHevyExercises(titlesOf(parsed.workouts), repository.allTemplates())
                val defaults = defaultResolutions(matches)
                val plan = planHevyImport(parsed.workouts, defaults, repository.existingWorkoutStartsEpochMs(), ids, clock)
                HevyPreview.Ready(parsed.workouts, matches, defaults, plan)
            }
        }
}

/**
 * Adds the import in one transaction, then sets the body weight when [bodyWeightKg] is non-null
 * (D-045). It re-reads starts and templates and re-plans, so ids are fresh and a workout logged
 * since the preview is still a duplicate. A [HevyResolution.UseTemplate] for an id it did not read,
 * or a missing resolution, falls back to the fresh default, so no row can point at a missing
 * template (D-058). Returns the plan it applied.
 *
 * @throws IllegalArgumentException when [bodyWeightKg] is not positive, before anything is written.
 */
class ApplyHevyImportUseCase(
    private val repository: HevyImportRepository,
    private val settings: SettingsRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        workouts: List<HevyWorkout>,
        resolutions: Map<String, HevyResolution>,
        bodyWeightKg: Double?,
    ): HevyImportPlan {
        require(bodyWeightKg == null || bodyWeightKg > 0) { "Body weight must be positive" }
        val templates = repository.allTemplates()
        val readIds = templates.mapTo(HashSet()) { it.id }
        val titles = titlesOf(workouts)
        val fresh = defaultResolutions(matchHevyExercises(titles, templates))
        val checked = titles.associateWith { title ->
            when (val chosen = resolutions[title]) {
                HevyResolution.CreateCustom -> chosen
                is HevyResolution.UseTemplate -> chosen.takeIf { it.templateId in readIds } ?: fresh.getValue(title)
                null -> fresh.getValue(title)
            }
        }
        val plan = planHevyImport(workouts, checked, repository.existingWorkoutStartsEpochMs(), ids, clock)
        repository.add(plan.newTemplates, plan.newWorkouts)
        // DataStore cannot join the Room transaction, so it is written only after the commit.
        bodyWeightKg?.let { settings.setBodyWeightKg(it) }
        return plan
    }
}

private fun titlesOf(workouts: List<HevyWorkout>): Set<String> =
    workouts.flatMapTo(LinkedHashSet()) { w -> w.exercises.map { it.title } }
