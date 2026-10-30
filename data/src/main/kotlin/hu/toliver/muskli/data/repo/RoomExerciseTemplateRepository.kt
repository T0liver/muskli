package hu.toliver.muskli.data.repo

import hu.toliver.muskli.data.db.dao.ExerciseTemplateDao
import hu.toliver.muskli.data.db.secondaryMuscleEntities
import hu.toliver.muskli.data.db.toDomain
import hu.toliver.muskli.data.db.toEntity
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomExerciseTemplateRepository(private val dao: ExerciseTemplateDao) : ExerciseTemplateRepository {
    override fun observeAll(): Flow<List<ExerciseTemplate>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeRecentlyUsed(limit: Int): Flow<List<ExerciseTemplate>> =
        dao.observeRecentlyUsed(limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(id: String): ExerciseTemplate? = dao.get(id)?.toDomain()

    override suspend fun upsert(template: ExerciseTemplate) = dao.upsert(template.toEntity(), template.secondaryMuscleEntities())
}
