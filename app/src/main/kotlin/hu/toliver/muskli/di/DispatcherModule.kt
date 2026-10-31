package hu.toliver.muskli.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/** For CPU-heavy work such as parsing a whole import file; tests replace it with a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WorkDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @WorkDispatcher
    fun work(): CoroutineDispatcher = Dispatchers.Default
}
