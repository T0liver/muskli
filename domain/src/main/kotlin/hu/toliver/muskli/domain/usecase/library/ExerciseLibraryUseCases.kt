package hu.toliver.muskli.domain.usecase.library

import hu.toliver.muskli.domain.library.ExerciseFilter
import hu.toliver.muskli.domain.library.searchExercises
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.repo.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The user-editable fields of a custom exercise. The primary muscle is dropped from [secondaryMuscles]. */
data class CustomExerciseInput(
    val title: String,
    val type: ExerciseType,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: Set<MuscleGroup>,
    val equipment: Equipment,
    val unilateral: Boolean = false,
)

class SearchExercisesUseCase(private val repository: ExerciseTemplateRepository) {
    operator fun invoke(filter: ExerciseFilter): Flow<List<ExerciseTemplate>> =
        repository.observeAll().map { searchExercises(it, filter) }
}

class CreateCustomExerciseUseCase(
    private val repository: ExerciseTemplateRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    /** @throws IllegalArgumentException when the title is blank. */
    suspend operator fun invoke(input: CustomExerciseInput): ExerciseTemplate {
        val now = clock.nowEpochMs()
        val template = input.applyTo(
            ExerciseTemplate(
                id = ids.newId(),
                title = "",
                type = input.type,
                primaryMuscle = input.primaryMuscle,
                secondaryMuscles = emptySet(),
                equipment = input.equipment,
                isCustom = true,
                archived = false,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
        repository.upsert(template)
        return template
    }
}

class EditCustomExerciseUseCase(
    private val repository: ExerciseTemplateRepository,
    private val clock: Clock,
) {
    /** Null when [id] is missing or not a custom exercise. @throws IllegalArgumentException when the title is blank. */
    suspend operator fun invoke(id: String, input: CustomExerciseInput): ExerciseTemplate? {
        val current = repository.get(id)?.takeIf { it.isCustom } ?: return null
        val edited = input.applyTo(current).copy(updatedAtEpochMs = clock.nowEpochMs())
        repository.upsert(edited)
        return edited
    }
}

class ArchiveCustomExerciseUseCase(
    private val repository: ExerciseTemplateRepository,
    private val clock: Clock,
) {
    /** False when [id] is missing or not a custom exercise. Archived rows stay so history keeps them. */
    suspend operator fun invoke(id: String): Boolean {
        val current = repository.get(id)?.takeIf { it.isCustom } ?: return false
        repository.upsert(current.copy(archived = true, updatedAtEpochMs = clock.nowEpochMs()))
        return true
    }
}

class UnarchiveCustomExerciseUseCase(
    private val repository: ExerciseTemplateRepository,
    private val clock: Clock,
) {
    /** False when [id] is missing, not custom, or not archived. The same row is kept, so workouts still reference it. */
    suspend operator fun invoke(id: String): Boolean {
        val current = repository.get(id)?.takeIf { it.isCustom && it.archived } ?: return false
        repository.upsert(current.copy(archived = false, updatedAtEpochMs = clock.nowEpochMs()))
        return true
    }
}

private fun CustomExerciseInput.applyTo(template: ExerciseTemplate): ExerciseTemplate {
    val trimmed = title.trim()
    require(trimmed.isNotEmpty()) { "Custom exercise title must not be blank" }
    return template.copy(
        title = trimmed,
        type = type,
        primaryMuscle = primaryMuscle,
        secondaryMuscles = secondaryMuscles - primaryMuscle,
        equipment = equipment,
        unilateral = unilateral,
    )
}
