package hu.toliver.muskli.domain.repo

import hu.toliver.muskli.domain.model.ExerciseTemplate
import kotlinx.coroutines.flow.Flow

interface ExerciseTemplateRepository {
    fun observeAll(): Flow<List<ExerciseTemplate>>

    /**
     * Distinct templates used in non-deleted workouts, the live draft included, most recently
     * started workout first, at most [limit]. Archived templates are left out.
     */
    fun observeRecentlyUsed(limit: Int): Flow<List<ExerciseTemplate>>

    suspend fun get(id: String): ExerciseTemplate?

    suspend fun upsert(template: ExerciseTemplate)
}
