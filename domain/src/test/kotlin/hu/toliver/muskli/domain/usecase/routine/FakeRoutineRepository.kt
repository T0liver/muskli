package hu.toliver.muskli.domain.usecase.routine

import hu.toliver.muskli.domain.model.Routine
import hu.toliver.muskli.domain.repo.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeRoutineRepository : RoutineRepository {
    val routines = MutableStateFlow<Map<String, Routine>>(emptyMap())

    override fun observeAll(): Flow<List<Routine>> =
        routines.map { all -> all.values.sortedWith(compareBy({ it.title.lowercase() }, { it.id })) }

    override fun observe(id: String): Flow<Routine?> = routines.map { it[id] }

    override suspend fun get(id: String): Routine? = routines.value[id]

    override suspend fun save(routine: Routine) {
        routines.value = routines.value + (routine.id to routine)
    }

    override suspend fun delete(id: String) {
        routines.value = routines.value - id
    }
}
