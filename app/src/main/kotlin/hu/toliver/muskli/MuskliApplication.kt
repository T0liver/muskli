package hu.toliver.muskli

import android.app.Application
import android.util.Log
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import hu.toliver.muskli.data.seed.BundledCatalogueLoader
import hu.toliver.muskli.data.seed.CatalogueSync
import hu.toliver.muskli.service.RestNotifications
import hu.toliver.muskli.service.RestTimerSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MuskliApplication : Application() {
    // Lazy, so the database and the HTTP source are built on a worker thread instead of before the first frame (AC-08).
    @Inject lateinit var bundledCatalogue: Lazy<BundledCatalogueLoader>

    @Inject lateinit var catalogueSync: Lazy<CatalogueSync>

    @Inject lateinit var restTimerSync: Lazy<RestTimerSync>

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Off the main thread. The bundle runs first so a newer published file replaces it;
        // either failure keeps the current library and never blocks startup (D-021, D-035).
        appScope.launch {
            try {
                bundledCatalogue.get().load()
            } catch (e: Exception) {
                Log.w(TAG, "Bundled catalogue not applied", e)
            }
            catalogueSync.get().sync()
        }
        RestNotifications.createChannels(this)
        // From process start, so a rest still running after the process died is re-armed (AC-03).
        appScope.launch { restTimerSync.get().run() }
    }

    private companion object {
        const val TAG = "MuskliApplication"
    }
}
