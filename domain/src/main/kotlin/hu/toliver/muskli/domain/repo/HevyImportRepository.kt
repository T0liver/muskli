package hu.toliver.muskli.domain.repo

import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.Workout

/** Reads and the one insert-only write a Hevy import needs (docs/HEVY-IMPORT.md §4, D-044). */
interface HevyImportRepository {
    /** Start of every workout row: finished, draft, and soft-deleted, so a deleted import stays deleted. */
    suspend fun existingWorkoutStartsEpochMs(): List<Long>

    /** Every exercise template on the device, catalogue and custom, archived included. */
    suspend fun allTemplates(): List<ExerciseTemplate>

    /** Inserts [templates] and [workouts] in one transaction; on any failure nothing is written. Never updates or deletes. */
    suspend fun add(templates: List<ExerciseTemplate>, workouts: List<Workout>)
}
