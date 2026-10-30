package hu.toliver.muskli.ui.backup

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/** Documents picked through the system file picker, addressed by their content URI string. */
interface BackupDocuments {
    /** @throws IOException when the document cannot be written. */
    suspend fun write(uri: String, text: String)

    /** @throws IOException when the document cannot be read. */
    suspend fun read(uri: String): String
}

// core-ktx's toUri is only on the classpath transitively, not a declared dependency.
@SuppressLint("UseKtx")
class ContentResolverBackupDocuments @Inject constructor(
    @ApplicationContext private val context: Context,
) : BackupDocuments {
    override suspend fun write(uri: String, text: String) = withContext(Dispatchers.IO) {
        // "wt" truncates, so exporting onto an existing, longer file leaves no old bytes behind.
        val stream = context.contentResolver.openOutputStream(Uri.parse(uri), "wt") ?: throw IOException("No stream for $uri")
        stream.bufferedWriter().use { it.write(text) }
    }

    override suspend fun read(uri: String): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(Uri.parse(uri)) ?: throw IOException("No stream for $uri")
        stream.bufferedReader().use { it.readText() }
    }
}
