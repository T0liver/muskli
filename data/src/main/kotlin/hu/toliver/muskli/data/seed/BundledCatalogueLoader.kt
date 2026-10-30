package hu.toliver.muskli.data.seed

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the catalogue bundled in assets (copied from `catalogue/catalogue.json` at build time) so
 * the first launch works offline (D-021). Safe to run on every startup: an equal version is a no-op.
 */
class BundledCatalogueLoader(
    private val context: Context,
    private val applier: CatalogueApplier,
) {
    /** True when the bundled catalogue was newer and got applied. Throws if the asset is unreadable or malformed. */
    suspend fun load(): Boolean {
        val json = withContext(Dispatchers.IO) {
            context.assets.open(ASSET_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        return applier.apply(CatalogueParser.parse(json))
    }

    companion object {
        const val ASSET_PATH = "catalogue/catalogue.json"
    }
}
