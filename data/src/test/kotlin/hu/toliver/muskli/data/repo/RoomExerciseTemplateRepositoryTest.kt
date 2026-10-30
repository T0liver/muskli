package hu.toliver.muskli.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.data.db.MuskliDatabase
import hu.toliver.muskli.data.db.benchPress
import hu.toliver.muskli.data.db.dumbbellRow
import hu.toliver.muskli.data.db.toRows
import hu.toliver.muskli.data.db.workout
import hu.toliver.muskli.domain.model.Workout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomExerciseTemplateRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MuskliDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = RoomExerciseTemplateRepository(db.exerciseTemplateDao())
    private val squat = benchPress.copy(id = "squat", title = "Squat", secondaryMuscles = emptySet())

    @After
    fun tearDown() = db.close()

    private suspend fun save(workout: Workout) {
        val rows = workout.toRows()
        db.workoutDao().save(rows.workout, rows.exercises, rows.sets)
    }

    private fun Workout.using(vararg templateIds: String) =
        copy(exercises = templateIds.mapIndexed { i, templateId -> exercises[0].copy(id = "$id-e$i", exerciseTemplateId = templateId, sets = emptyList()) })

    @Test
    fun `upsert and get round-trip through the repository`() = runTest {
        repository.upsert(benchPress)

        assertEquals(benchPress, repository.get(benchPress.id))
        assertEquals(listOf(benchPress), repository.observeAll().first())
    }

    @Test
    fun `recently used is distinct, newest workout first, draft included, deleted workouts left out`() = runTest {
        listOf(benchPress, dumbbellRow, squat).forEach { repository.upsert(it) }
        save(workout(id = "old", startedAt = 1_000L).using(benchPress.id, squat.id))
        save(workout(id = "newer", startedAt = 2_000L).using(dumbbellRow.id))
        save(workout(id = "draft", startedAt = 3_000L, finishedAt = null).using(squat.id))
        save(workout(id = "deleted", startedAt = 4_000L, deletedAt = 4_500L).using(benchPress.id))

        assertEquals(listOf(squat, dumbbellRow, benchPress), repository.observeRecentlyUsed(limit = 10).first())
        assertEquals(listOf(squat, dumbbellRow), repository.observeRecentlyUsed(limit = 2).first())
    }

    @Test
    fun `recently used leaves out archived exercises`() = runTest {
        listOf(benchPress, dumbbellRow).forEach { repository.upsert(it) }
        save(workout().using(benchPress.id, dumbbellRow.id))
        repository.upsert(dumbbellRow.copy(archived = true))

        assertEquals(listOf(benchPress), repository.observeRecentlyUsed(limit = 10).first())
    }
}
