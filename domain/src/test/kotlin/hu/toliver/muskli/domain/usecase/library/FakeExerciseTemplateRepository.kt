package hu.toliver.muskli.domain.usecase.library

import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeExerciseTemplateRepository(initial: List<ExerciseTemplate> = emptyList()) : ExerciseTemplateRepository {
    val templates = MutableStateFlow(initial.associateBy { it.id })

    override fun observeAll(): Flow<List<ExerciseTemplate>> = templates.map { it.values.toList() }

    override fun observeRecentlyUsed(limit: Int): Flow<List<ExerciseTemplate>> = error("not used")

    override suspend fun get(id: String): ExerciseTemplate? = templates.value[id]

    override suspend fun upsert(template: ExerciseTemplate) {
        templates.value = templates.value + (template.id to template)
    }
}
