package hu.toliver.muskli.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.usecase.library.ArchiveCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.library.CreateCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.library.EditCustomExerciseUseCase
import hu.toliver.muskli.domain.usecase.library.SearchExercisesUseCase
import hu.toliver.muskli.domain.usecase.library.UnarchiveCustomExerciseUseCase

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
}
