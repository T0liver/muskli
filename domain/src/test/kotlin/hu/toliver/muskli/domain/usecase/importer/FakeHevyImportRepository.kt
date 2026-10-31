package hu.toliver.muskli.domain.usecase.importer

import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.repo.HevyImportRepository

class FakeHevyImportRepository(
    var templates: List<ExerciseTemplate> = emptyList(),
    var starts: List<Long> = emptyList(),
) : HevyImportRepository {
    val addedTemplates = mutableListOf<ExerciseTemplate>()
    val addedWorkouts = mutableListOf<Workout>()
    var failOnAdd: Exception? = null

    /** Shared with other fakes to check the order of writes. */
    var log: MutableList<String> = mutableListOf()

    override suspend fun existingWorkoutStartsEpochMs(): List<Long> = starts + addedWorkouts.map { it.startedAtEpochMs }

    override suspend fun allTemplates(): List<ExerciseTemplate> = templates + addedTemplates

    override suspend fun add(templates: List<ExerciseTemplate>, workouts: List<Workout>) {
        failOnAdd?.let { throw it }
        addedTemplates += templates
        addedWorkouts += workouts
        log += "add"
    }
}
