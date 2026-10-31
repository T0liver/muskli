package hu.toliver.muskli.domain.repo

import hu.toliver.muskli.domain.model.Routine
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    /** Every routine, by title ignoring case, then id. */
    fun observeAll(): Flow<List<Routine>>

    fun observe(id: String): Flow<Routine?>

    suspend fun get(id: String): Routine?

    /** Writes the routine with all its exercises and sets, replacing what was stored for it. */
    suspend fun save(routine: Routine)

    /** Removes the routine for good; workouts started from it keep their sets and lose the `routineId` (D-052). */
    suspend fun delete(id: String)
}
