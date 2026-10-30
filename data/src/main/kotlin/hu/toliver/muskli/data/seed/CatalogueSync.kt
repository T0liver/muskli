package hu.toliver.muskli.data.seed

import android.util.Log
import hu.toliver.muskli.domain.catalogue.CatalogueBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full sync of the published catalogue (D-021). A strictly newer bundle is applied through
 * [CatalogueApplier] (same upsert rules as the bundled loader). Transport errors, a non-200
 * response, malformed JSON, and an unknown format version leave the stored library as it is.
 * Does not throw, so a failure cannot crash startup.
 */
class CatalogueSync(
    private val source: CatalogueHttpSource,
    private val applier: CatalogueApplier,
) {
    /** True when a newer bundle was applied. */
    suspend fun sync(): Boolean = withContext(Dispatchers.IO) {
        val bundle = try {
            val response = source.fetch()
            if (response.statusCode != HTTP_OK) {
                Log.w(TAG, "Catalogue sync skipped: HTTP ${response.statusCode}")
                return@withContext false
            }
            CatalogueParser.parse(response.body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Catalogue sync failed", e)
            return@withContext false
        }
        applyOrKeep(bundle)
    }

    private suspend fun applyOrKeep(bundle: CatalogueBundle): Boolean = try {
        applier.apply(bundle)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Catalogue sync failed", e)
        false
    }

    private companion object {
        const val TAG = "CatalogueSync"
        const val HTTP_OK = 200
    }
}
