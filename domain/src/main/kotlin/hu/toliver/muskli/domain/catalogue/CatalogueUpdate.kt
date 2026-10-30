package hu.toliver.muskli.domain.catalogue

import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup

/** The catalogue format this build understands (`formatVersion`, docs/CATALOGUE.md §2). */
const val SUPPORTED_CATALOGUE_FORMAT = 1

/** A parsed catalogue file, bundled or downloaded (D-021, D-025). */
data class CatalogueBundle(
    val formatVersion: Int,
    val version: Int,
    val exercises: List<CatalogueExercise>,
)

data class CatalogueExercise(
    val id: String,
    val title: String,
    val type: ExerciseType,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: Set<MuscleGroup>,
    val equipment: Equipment,
    /** Optional in the file; absent means false (docs/CATALOGUE.md §2). */
    val unilateral: Boolean = false,
)

fun CatalogueExercise.toTemplate(nowEpochMs: Long) = ExerciseTemplate(
    id = id,
    title = title,
    type = type,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles,
    equipment = equipment,
    isCustom = false,
    archived = false,
    createdAtEpochMs = nowEpochMs,
    updatedAtEpochMs = nowEpochMs,
    unilateral = unilateral,
)

/** What applying a bundle writes, in one transaction together with [version]. */
data class CatalogueUpdate(
    val version: Int,
    val upserts: List<ExerciseTemplate>,
    val archiveIds: List<String>,
)

/**
 * Plans applying [bundle] over the [existing] library, or null when it must not be applied: an
 * unknown format, or a version not newer than [storedVersion] (null for a library never loaded).
 * Custom exercises are never touched. A published exercise missing from the bundle is archived
 * when no workout references it ([referencedIds]) and kept as it is otherwise (D-021).
 */
fun planCatalogueUpdate(
    bundle: CatalogueBundle,
    storedVersion: Int?,
    existing: List<ExerciseTemplate>,
    referencedIds: Set<String>,
    nowEpochMs: Long,
): CatalogueUpdate? {
    if (bundle.formatVersion != SUPPORTED_CATALOGUE_FORMAT) return null
    if (storedVersion != null && bundle.version <= storedVersion) return null

    val existingById = existing.associateBy { it.id }
    val upserts = bundle.exercises.mapNotNull { exercise ->
        val current = existingById[exercise.id]
        when {
            current == null -> exercise.toTemplate(nowEpochMs)
            current.isCustom -> null
            current == exercise.toTemplate(current.updatedAtEpochMs).copy(createdAtEpochMs = current.createdAtEpochMs) -> null
            else -> exercise.toTemplate(nowEpochMs).copy(createdAtEpochMs = current.createdAtEpochMs)
        }
    }
    val bundleIds = bundle.exercises.mapTo(HashSet()) { it.id }
    val archiveIds = existing
        .filter { !it.isCustom && !it.archived && it.id !in bundleIds && it.id !in referencedIds }
        .map { it.id }
    return CatalogueUpdate(bundle.version, upserts, archiveIds)
}
