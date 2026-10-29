package hu.toliver.muskli.domain.repo

import hu.toliver.muskli.domain.model.ExerciseTemplate
import kotlinx.coroutines.flow.Flow

interface ExerciseTemplateRepository {
    fun observeAll(): Flow<List<ExerciseTemplate>>

    suspend fun get(id: String): ExerciseTemplate?

    suspend fun upsert(template: ExerciseTemplate)
}
