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
import hu.toliver.muskli.data.backup.JsonBackupRepository
import hu.toliver.muskli.data.db.MuskliDatabase
import hu.toliver.muskli.data.repo.RoomExerciseTemplateRepository
import hu.toliver.muskli.data.repo.RoomRoutineRepository
import hu.toliver.muskli.data.repo.RoomWorkoutRepository
import hu.toliver.muskli.data.seed.BundledCatalogueLoader
import hu.toliver.muskli.data.seed.CatalogueApplier
import hu.toliver.muskli.data.seed.CatalogueHttpSource
import hu.toliver.muskli.data.seed.CatalogueSync
import hu.toliver.muskli.data.seed.HttpCatalogueSource
import hu.toliver.muskli.data.settings.DataStoreSettingsRepository
import hu.toliver.muskli.domain.repo.BackupRepository
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.repo.RoutineRepository
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.repo.WorkoutRepository
import hu.toliver.muskli.ui.backup.BackupDocuments
import hu.toliver.muskli.ui.backup.ContentResolverBackupDocuments
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

    @Provides
    fun routineRepository(db: MuskliDatabase): RoutineRepository = RoomRoutineRepository(db.routineDao())

    // Singleton: DataStore allows only one instance per file in a process.
    @Provides
    @Singleton
    fun settingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("settings") })

    @Provides
    fun settingsRepository(store: DataStore<Preferences>): SettingsRepository = DataStoreSettingsRepository(store)

    @Provides
    fun backupRepository(db: MuskliDatabase, settings: SettingsRepository, clock: Clock): BackupRepository =
        JsonBackupRepository(db, settings, clock)

    @Provides
    fun backupDocuments(documents: ContentResolverBackupDocuments): BackupDocuments = documents

    @Provides
    fun catalogueApplier(db: MuskliDatabase, clock: Clock) = CatalogueApplier(db, clock)

    @Provides
    fun bundledCatalogueLoader(@ApplicationContext context: Context, applier: CatalogueApplier) =
        BundledCatalogueLoader(context, applier)

    @Provides
    fun catalogueHttpSource(): CatalogueHttpSource = HttpCatalogueSource()

    @Provides
    fun catalogueSync(source: CatalogueHttpSource, applier: CatalogueApplier) = CatalogueSync(source, applier)
}
