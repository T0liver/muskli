package hu.toliver.muskli.ui.backup

import hu.toliver.muskli.domain.repo.BackupRepository
import hu.toliver.muskli.domain.repo.ImportError
import hu.toliver.muskli.domain.repo.ImportProblem
import hu.toliver.muskli.domain.repo.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    private val backup = FakeBackup()
    private val documents = FakeDocuments()
    private val viewModel by lazy { BackupViewModel(backup, documents) }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `export writes the backup to the picked document`() {
        viewModel.onEvent(BackupEvent.ExportTo("content://out"))

        assertEquals(mapOf("content://out" to "{exported}"), documents.files)
        assertEquals(BackupMessage.Exported, viewModel.uiState.value.message)
        viewModel.onEvent(BackupEvent.MessageShown)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `a failed write is reported`() {
        documents.fail = true

        viewModel.onEvent(BackupEvent.ExportTo("content://out"))

        assertEquals(BackupMessage.ExportFailed, viewModel.uiState.value.message)
        assertEquals(false, viewModel.uiState.value.busy)
    }

    @Test
    fun `a picked file is imported only after confirmation`() {
        documents.files["content://in"] = "{file}"

        viewModel.onEvent(BackupEvent.ImportPicked("content://in"))
        assertEquals("content://in", viewModel.uiState.value.pendingImportUri)
        assertEquals(emptyList<String>(), backup.imported)

        viewModel.onEvent(BackupEvent.ImportConfirmed)
        assertEquals(listOf("{file}"), backup.imported)
        assertNull(viewModel.uiState.value.pendingImportUri)
        assertEquals(BackupMessage.Imported(workouts = 3), viewModel.uiState.value.message)
    }

    @Test
    fun `cancelling the confirmation imports nothing`() {
        viewModel.onEvent(BackupEvent.ImportPicked("content://in"))
        viewModel.onEvent(BackupEvent.ImportCancelled)

        assertNull(viewModel.uiState.value.pendingImportUri)
        assertEquals(emptyList<String>(), backup.imported)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `a rejected file shows the error report and an unreadable one says so`() {
        val errors = listOf(ImportError(ImportProblem.INVALID_VALUE, "workouts[0].title"))
        backup.result = ImportResult.Rejected(errors)
        documents.files["content://bad"] = "{bad}"

        viewModel.onEvent(BackupEvent.ImportPicked("content://bad"))
        viewModel.onEvent(BackupEvent.ImportConfirmed)
        assertEquals(BackupMessage.Rejected(errors), viewModel.uiState.value.message)

        viewModel.onEvent(BackupEvent.ImportPicked("content://missing"))
        viewModel.onEvent(BackupEvent.ImportConfirmed)
        assertEquals(BackupMessage.ReadFailed, viewModel.uiState.value.message)
    }

    private class FakeBackup : BackupRepository {
        val imported = mutableListOf<String>()
        var result: ImportResult = ImportResult.Imported(workouts = 3, customExercises = 1)

        override suspend fun export() = "{exported}"

        override suspend fun import(json: String): ImportResult = result.also { imported += json }
    }

    private class FakeDocuments : BackupDocuments {
        val files = mutableMapOf<String, String>()
        var fail = false

        override suspend fun write(uri: String, text: String) {
            if (fail) throw IOException("disk full")
            files[uri] = text
        }

        override suspend fun read(uri: String) = files[uri] ?: throw IOException("gone")
    }
}
