package hu.toliver.muskli.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hu.toliver.muskli.data.SystemWallClock
import hu.toliver.muskli.data.UuidIdGenerator
import hu.toliver.muskli.data.db.MuskliDatabase
import hu.toliver.muskli.data.repo.RoomExerciseTemplateRepository
import hu.toliver.muskli.data.repo.RoomWorkoutRepository
import hu.toliver.muskli.data.seed.BundledCatalogueLoader
import hu.toliver.muskli.data.seed.CatalogueApplier
import hu.toliver.muskli.data.settings.DataStoreSettingsRepository
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.repo.WorkoutRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MuskliDatabase = MuskliDatabase.build(context)

    @Provides
    fun clock(): Clock = SystemWallClock()

    @Provides
    fun idGenerator(): IdGenerator = UuidIdGenerator()

    @Provides
    fun exerciseTemplateRepository(db: MuskliDatabase): ExerciseTemplateRepository =
        RoomExerciseTemplateRepository(db.exerciseTemplateDao())

    @Provides
    fun workoutRepository(db: MuskliDatabase): WorkoutRepository = RoomWorkoutRepository(db)

    // Singleton: DataStore allows only one instance per file in a process.
    @Provides
    @Singleton
    fun settingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("settings") })

    @Provides
    fun settingsRepository(store: DataStore<Preferences>): SettingsRepository = DataStoreSettingsRepository(store)

    @Provides
    fun bundledCatalogueLoader(@ApplicationContext context: Context, db: MuskliDatabase, clock: Clock) =
        BundledCatalogueLoader(context, CatalogueApplier(db, clock))
}
