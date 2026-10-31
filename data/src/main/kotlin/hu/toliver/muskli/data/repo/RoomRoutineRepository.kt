package hu.toliver.muskli.data.repo

import hu.toliver.muskli.data.db.dao.RoutineDao
import hu.toliver.muskli.data.db.toDomain
import hu.toliver.muskli.data.db.toRows
import hu.toliver.muskli.domain.model.Routine
import hu.toliver.muskli.domain.repo.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRoutineRepository(private val dao: RoutineDao) : RoutineRepository {
    override fun observeAll(): Flow<List<Routine>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observe(id: String): Flow<Routine?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun get(id: String): Routine? = dao.get(id)?.toDomain()

    override suspend fun save(routine: Routine) {
        val rows = routine.toRows()
        dao.save(rows.routine, rows.exercises, rows.sets)
    }

    override suspend fun delete(id: String) = dao.delete(id)
}
