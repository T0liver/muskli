package hu.toliver.muskli.data.repo

import androidx.room.withTransaction
import hu.toliver.muskli.data.db.MuskliDatabase
import hu.toliver.muskli.data.db.toDomain
import hu.toliver.muskli.data.db.toExerciseRecords
import hu.toliver.muskli.data.db.toRows
import hu.toliver.muskli.domain.calc.WorkoutExerciseRecord
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.repo.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWorkoutRepository(private val db: MuskliDatabase) : WorkoutRepository {
    private val dao = db.workoutDao()

    override fun observe(id: String): Flow<Workout?> = dao.observe(id).map { it?.toDomain() }

    override fun observeActiveDraft(): Flow<Workout?> = dao.observeActiveDraft().map { it?.toDomain() }

    override fun observeHistory(): Flow<List<Workout>> = dao.observeHistory().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(workout: Workout) {
        val rows = workout.toRows()
        dao.save(rows.workout, rows.exercises, rows.sets)
    }

    override suspend fun startDraftUnlessActive(draft: Workout): Workout = db.withTransaction {
        dao.activeDraft()?.toDomain() ?: draft.also { save(it) }
    }

    override suspend fun update(id: String, transform: (Workout) -> Workout): Workout? = db.withTransaction {
        val current = dao.get(id)?.toDomain() ?: return@withTransaction null
        val updated = transform(current)
        if (updated != current) save(updated)
        updated
    }

    override suspend fun softDelete(id: String, deletedAtEpochMs: Long) = dao.softDelete(id, deletedAtEpochMs)

    override suspend fun restore(id: String) = dao.restore(id)

    override suspend fun exerciseHistory(exerciseTemplateId: String): List<WorkoutExerciseRecord> =
        dao.finishedWorkoutsWithExercise(exerciseTemplateId).flatMap { it.toExerciseRecords(exerciseTemplateId) }
}
