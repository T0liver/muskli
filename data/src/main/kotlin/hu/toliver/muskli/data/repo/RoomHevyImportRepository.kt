package hu.toliver.muskli.data.repo

import androidx.room.withTransaction
import hu.toliver.muskli.data.db.MuskliDatabase
import hu.toliver.muskli.data.db.secondaryMuscleEntities
import hu.toliver.muskli.data.db.toDomain
import hu.toliver.muskli.data.db.toEntity
import hu.toliver.muskli.data.db.toRows
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.repo.HevyImportRepository

class RoomHevyImportRepository(private val db: MuskliDatabase) : HevyImportRepository {
    override suspend fun existingWorkoutStartsEpochMs(): List<Long> = db.backupDao().allWorkoutStartTimes()

    override suspend fun allTemplates(): List<ExerciseTemplate> = db.exerciseTemplateDao().getAll().map { it.toDomain() }

    override suspend fun add(templates: List<ExerciseTemplate>, workouts: List<Workout>) = db.withTransaction {
        val dao = db.backupDao()
        val rowIds = dao.insertTemplatesIfMissing(templates.map { it.toEntity() })
        // An ignored row would leave workouts pointing at someone else's template; fail and roll back instead.
        check(-1L !in rowIds) { "An exercise template id already exists" }
        dao.insertSecondaryMusclesIfMissing(templates.flatMap { it.secondaryMuscleEntities() })
        val rows = workouts.map { it.toRows() }
        dao.insertWorkouts(rows.map { it.workout })
        dao.insertExercises(rows.flatMap { it.exercises })
        dao.insertSets(rows.flatMap { it.sets })
    }
}
