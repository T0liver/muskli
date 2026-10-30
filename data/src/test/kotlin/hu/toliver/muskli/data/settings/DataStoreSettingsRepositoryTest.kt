package hu.toliver.muskli.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeekStartDay
import hu.toliver.muskli.domain.settings.WeightUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

// Robolectric, not plain JVM: with SDK_INT 0 DataStore falls back to File.renameTo, which cannot replace a file on Windows.
@RunWith(AndroidJUnit4::class)
class DataStoreSettingsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    private fun file() = File(folder.root, "settings.preferences_pb")

    private fun open(): DataStoreSettingsRepository {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }
        return DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = scope, produceFile = ::file))
    }

    /** Simulates an app restart: only one DataStore may own a file at a time. */
    private fun reopen(): DataStoreSettingsRepository {
        scopes.removeLast().cancel()
        return open()
    }

    @Test
    fun emptyStoreEmitsDefaults() = runBlocking {
        assertEquals(Settings(), open().settings.first())
    }

    @Test
    fun everyFieldPersistsAcrossRestart() = runBlocking {
        val repo = open()
        repo.setWeightUnit(WeightUnit.LB)
        repo.setDefaultRestSeconds(150)
        repo.setPreviousMode(PreviousMode.ROUTINE_SCOPED)
        repo.setKeepScreenAwake(false)
        repo.setSoundEnabled(false)
        repo.setVibrationEnabled(false)
        repo.setWeekStart(WeekStartDay.SUNDAY)
        repo.setBodyWeightKg(82.5)

        val expected = Settings(
            weightUnit = WeightUnit.LB,
            defaultRestSeconds = 150,
            previousMode = PreviousMode.ROUTINE_SCOPED,
            keepScreenAwake = false,
            soundEnabled = false,
            vibrationEnabled = false,
            weekStart = WeekStartDay.SUNDAY,
            bodyWeightKg = 82.5,
        )
        assertEquals(expected, repo.settings.first())
        assertEquals(expected, reopen().settings.first())
    }

    @Test
    fun restOffIsDistinctFromNeverSet() = runBlocking {
        open().setDefaultRestSeconds(null)

        assertNull(reopen().settings.first().defaultRestSeconds)
    }

    @Test
    fun clearingBodyWeightReturnsToUnset() = runBlocking {
        val repo = open()
        repo.setBodyWeightKg(80.0)
        repo.setBodyWeightKg(null)

        assertNull(reopen().settings.first().bodyWeightKg)
    }

    @Test
    fun nonPositiveValuesAreRejected() {
        val repo = open()
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.setBodyWeightKg(0.0) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.setDefaultRestSeconds(0) } }
    }

    @Test
    fun flowEmitsOnChange() = runBlocking {
        val repo = open()
        val seen = mutableListOf<Settings>()
        val job = launch { repo.settings.take(2).toList(seen) }
        while (seen.isEmpty()) yield()

        repo.setKeepScreenAwake(false)
        job.join()

        assertEquals(listOf(true, false), seen.map { it.keepScreenAwake })
    }
}
