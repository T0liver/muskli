package hu.toliver.muskli.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.repo.HevyImportRepository
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.repo.RoutineRepository
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.repo.WorkoutRepository
import hu.toliver.muskli.domain.usecase.importer.ApplyHevyImportUseCase
import hu.toliver.muskli.domain.usecase.importer.PreviewHevyImportUseCase
import hu.toliver.muskli.domain.usecase.library.ArchiveCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.library.CreateCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.library.EditCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.library.SearchExercisesUseCase
import hu.toliver.muskli.domain.usecase.library.UnarchiveCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.routine.CreateRoutineUseCase
import hu.toliver.muskli.domain.usecase.routine.DeleteRoutineUseCase
import hu.toliver.muskli.domain.usecase.routine.ObserveRoutinesUseCase
import hu.toliver.muskli.domain.usecase.routine.SaveWorkoutAsRoutineUseCase
import hu.toliver.muskli.domain.usecase.routine.StartWorkoutFromRoutineUseCase
import hu.toliver.muskli.domain.usecase.routine.UpdateRoutineUseCase
import hu.toliver.muskli.domain.usecase.workout.AddExercisesUseCase
import hu.toliver.muskli.domain.usecase.workout.AddSetUseCase
import hu.toliver.muskli.domain.usecase.workout.AdjustRestTimerUseCase
import hu.toliver.muskli.domain.usecase.workout.ChangeSetTypeUseCase
import hu.toliver.muskli.domain.usecase.workout.ChangeStartTimeUseCase
import hu.toliver.muskli.domain.usecase.workout.CompleteSetUseCase
import hu.toliver.muskli.domain.usecase.workout.DeleteSetUseCase
import hu.toliver.muskli.domain.usecase.workout.DeleteWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.EditSetValuesUseCase
import hu.toliver.muskli.domain.usecase.workout.FinishRestTimerUseCase
import hu.toliver.muskli.domain.usecase.workout.FinishWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.MarkSetCompletedUseCase
import hu.toliver.muskli.domain.usecase.workout.PreviousSetsBeforeUseCase
import hu.toliver.muskli.domain.usecase.workout.PreviousSetsUseCase
import hu.toliver.muskli.domain.usecase.workout.RemoveExerciseUseCase
import hu.toliver.muskli.domain.usecase.workout.ReorderExerciseUseCase
import hu.toliver.muskli.domain.usecase.workout.ReplaceExerciseUseCase
import hu.toliver.muskli.domain.usecase.workout.RestoreWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.SkipRestTimerUseCase
import hu.toliver.muskli.domain.usecase.workout.StartPastWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.StartWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.UncompleteSetUseCase

// :domain has no javax.inject, so use cases are constructed here.
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    fun searchExercises(repository: ExerciseTemplateRepository) = SearchExercisesUseCase(repository)

    @Provides
    fun createCustomExercise(repository: ExerciseTemplateRepository, ids: IdGenerator, clock: Clock) =
        CreateCustomExerciseUseCase(repository, ids, clock)

    @Provides
    fun editCustomExercise(repository: ExerciseTemplateRepository, clock: Clock) = EditCustomExerciseUseCase(repository, clock)

    @Provides
    fun archiveCustomExercise(repository: ExerciseTemplateRepository, clock: Clock) =
        ArchiveCustomExerciseUseCase(repository, clock)

    @Provides
    fun unarchiveCustomExercise(repository: ExerciseTemplateRepository, clock: Clock) =
        UnarchiveCustomExerciseUseCase(repository, clock)

    @Provides
    fun startWorkout(repository: WorkoutRepository, ids: IdGenerator, clock: Clock) = StartWorkoutUseCase(repository, ids, clock)

    @Provides
    fun addExercises(repository: WorkoutRepository, ids: IdGenerator, clock: Clock) = AddExercisesUseCase(repository, ids, clock)

    @Provides
    fun replaceExercise(repository: WorkoutRepository, ids: IdGenerator, clock: Clock) = ReplaceExerciseUseCase(repository, ids, clock)

    @Provides
    fun removeExercise(repository: WorkoutRepository, clock: Clock) = RemoveExerciseUseCase(repository, clock)

    @Provides
    fun reorderExercise(repository: WorkoutRepository, clock: Clock) = ReorderExerciseUseCase(repository, clock)

    @Provides
    fun addSet(repository: WorkoutRepository, ids: IdGenerator, clock: Clock) = AddSetUseCase(repository, ids, clock)

    @Provides
    fun deleteSet(repository: WorkoutRepository, clock: Clock) = DeleteSetUseCase(repository, clock)

    @Provides
    fun changeSetType(repository: WorkoutRepository, clock: Clock) = ChangeSetTypeUseCase(repository, clock)

    @Provides
    fun editSetValues(repository: WorkoutRepository, clock: Clock) = EditSetValuesUseCase(repository, clock)

    @Provides
    fun completeSet(repository: WorkoutRepository, clock: Clock) = CompleteSetUseCase(repository, clock)

    @Provides
    fun uncompleteSet(repository: WorkoutRepository, clock: Clock) = UncompleteSetUseCase(repository, clock)

    @Provides
    fun previousSets(repository: WorkoutRepository) = PreviousSetsUseCase(repository)

    @Provides
    fun adjustRestTimer(repository: WorkoutRepository, clock: Clock) = AdjustRestTimerUseCase(repository, clock)

    @Provides
    fun skipRestTimer(repository: WorkoutRepository, clock: Clock) = SkipRestTimerUseCase(repository, clock)

    @Provides
    fun finishRestTimer(repository: WorkoutRepository) = FinishRestTimerUseCase(repository)

    @Provides
    fun finishWorkout(repository: WorkoutRepository, clock: Clock) = FinishWorkoutUseCase(repository, clock)

    @Provides
    fun deleteWorkout(repository: WorkoutRepository, clock: Clock) = DeleteWorkoutUseCase(repository, clock)

    @Provides
    fun restoreWorkout(repository: WorkoutRepository) = RestoreWorkoutUseCase(repository)

    @Provides
    fun startPastWorkout(repository: WorkoutRepository, ids: IdGenerator, clock: Clock) = StartPastWorkoutUseCase(repository, ids, clock)

    @Provides
    fun markSetCompleted(repository: WorkoutRepository, clock: Clock) = MarkSetCompletedUseCase(repository, clock)

    @Provides
    fun previousSetsBefore(repository: WorkoutRepository) = PreviousSetsBeforeUseCase(repository)

    @Provides
    fun changeStartTime(repository: WorkoutRepository, clock: Clock) = ChangeStartTimeUseCase(repository, clock)

    @Provides
    fun observeRoutines(repository: RoutineRepository) = ObserveRoutinesUseCase(repository)

    @Provides
    fun createRoutine(repository: RoutineRepository, ids: IdGenerator, clock: Clock) = CreateRoutineUseCase(repository, ids, clock)

    @Provides
    fun updateRoutine(repository: RoutineRepository, clock: Clock) = UpdateRoutineUseCase(repository, clock)

    @Provides
    fun deleteRoutine(repository: RoutineRepository) = DeleteRoutineUseCase(repository)

    @Provides
    fun startWorkoutFromRoutine(routines: RoutineRepository, workouts: WorkoutRepository, ids: IdGenerator, clock: Clock) =
        StartWorkoutFromRoutineUseCase(routines, workouts, ids, clock)

    @Provides
    fun saveWorkoutAsRoutine(workouts: WorkoutRepository, routines: RoutineRepository, ids: IdGenerator, clock: Clock) =
        SaveWorkoutAsRoutineUseCase(workouts, routines, ids, clock)

    @Provides
    fun previewHevyImport(repository: HevyImportRepository, ids: IdGenerator, clock: Clock) =
        PreviewHevyImportUseCase(repository, ids, clock)

    @Provides
    fun applyHevyImport(repository: HevyImportRepository, settings: SettingsRepository, ids: IdGenerator, clock: Clock) =
        ApplyHevyImportUseCase(repository, settings, ids, clock)
}
