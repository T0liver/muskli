package hu.toliver.muskli.data.seed

import java.net.HttpURLConnection
import java.net.URL

/** One GET of the published catalogue. Tests substitute a fake; production uses [HttpCatalogueSource]. */
fun interface CatalogueHttpSource {
    /** @throws java.io.IOException when the connection fails. */
    fun fetch(): CatalogueHttpResponse
}

data class CatalogueHttpResponse(val statusCode: Int, val body: String)

/**
 * GET of the published catalogue (D-025) through the platform `HttpURLConnection`.
 * Finite timeouts so a dead network ends the call instead of hanging the startup coroutine (D-035).
 */
class HttpCatalogueSource(
    private val url: String = PUBLISHED_CATALOGUE_URL,
    private val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = READ_TIMEOUT_MS,
) : CatalogueHttpSource {
    override fun fetch(): CatalogueHttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
        }
        try {
            val statusCode = connection.responseCode
            val body = if (statusCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                ""
            }
            return CatalogueHttpResponse(statusCode, body)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val PUBLISHED_CATALOGUE_URL = "https://t0liver.github.io/muskli/catalogue/v1/catalogue.json"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
    }
}
