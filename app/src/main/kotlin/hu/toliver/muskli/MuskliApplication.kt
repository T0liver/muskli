package hu.toliver.muskli

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import hu.toliver.muskli.data.seed.BundledCatalogueLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MuskliApplication : Application() {
    @Inject lateinit var bundledCatalogue: BundledCatalogueLoader

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Off the main thread; a broken bundle keeps the current library and never blocks startup (D-021).
        appScope.launch {
            try {
                bundledCatalogue.load()
            } catch (e: Exception) {
                Log.w(TAG, "Bundled catalogue not applied", e)
            }
        }
    }

    private companion object {
        const val TAG = "MuskliApplication"
    }
}
