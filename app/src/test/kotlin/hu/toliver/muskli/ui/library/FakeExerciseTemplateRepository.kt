package hu.toliver.muskli.ui.library

import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.usecase.library.ArchiveCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.library.CreateCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.library.EditCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.library.SearchExercisesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeExerciseTemplateRepository(templates: List<ExerciseTemplate>) : ExerciseTemplateRepository {
    val templates = MutableStateFlow(templates.associateBy { it.id })

    /** Ids in "most recently used first" order; the fake skips workouts. */
    val recentIds = MutableStateFlow<List<String>>(emptyList())

    override fun observeAll(): Flow<List<ExerciseTemplate>> = templates.map { it.values.toList() }

    override fun observeRecentlyUsed(limit: Int): Flow<List<ExerciseTemplate>> =
        combine(templates, recentIds) { all, ids -> ids.mapNotNull { all[it] }.filterNot { it.archived }.take(limit) }

    override suspend fun get(id: String): ExerciseTemplate? = templates.value[id]

    override suspend fun upsert(template: ExerciseTemplate) = templates.update { it + (template.id to template) }
}

fun template(
    id: String,
    title: String,
    muscle: MuscleGroup = MuscleGroup.CHEST,
    equipment: Equipment = Equipment.BARBELL,
    custom: Boolean = false,
) = ExerciseTemplate(
    id = id,
    title = title,
    type = ExerciseType.WEIGHT_REPS,
    primaryMuscle = muscle,
    secondaryMuscles = emptySet(),
    equipment = equipment,
    isCustom = custom,
    archived = false,
    createdAtEpochMs = 0,
    updatedAtEpochMs = 0,
)

val sampleLibrary = listOf(
    template("bench-press-barbell", "Bench Press (Barbell)"),
    template("bench-press-dumbbell", "Bench Press (Dumbbell)", equipment = Equipment.DUMBBELL),
    template("squat-barbell", "Squat (Barbell)", muscle = MuscleGroup.QUADRICEPS),
    template("pull-up", "Pull Up", muscle = MuscleGroup.LATS, equipment = Equipment.NONE),
)

fun libraryViewModel(repository: FakeExerciseTemplateRepository): LibraryViewModel {
    var nextId = 0
    val ids = IdGenerator { "custom-${nextId++}" }
    val clock = object : Clock {
        override fun nowEpochMs() = 1_000L

        override fun utcOffsetS(epochMs: Long) = 0
    }
    return LibraryViewModel(
        repository = repository,
        search = SearchExercisesUseCase(repository),
        createCustom = CreateCustomExerciseUseCase(repository, ids, clock),
        editCustom = EditCustomExerciseUseCase(repository, clock),
        archiveCustom = ArchiveCustomExerciseUseCase(repository, clock),
    )
}
