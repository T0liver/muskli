package hu.toliver.muskli.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.domain.model.RestTimer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        context.deleteDatabase(MuskliDatabase.NAME)
    }

    @Test
    fun `TC-DATA-02 draft survives recreating the database connection`() = runTest {
        val draft = workout(id = "draft", finishedAt = null).copy(restTimer = RestTimer(endsAtEpochMs = 99_000L, durationS = 90))
        MuskliDatabase.build(context).run {
            listOf(benchPress, dumbbellRow).forEach { exerciseTemplateDao().upsert(it.toEntity(), it.secondaryMuscleEntities()) }
            val rows = draft.toRows()
            workoutDao().save(rows.workout, rows.exercises, rows.sets)
            close()
        }

        val reopened = MuskliDatabase.build(context)
        try {
            assertEquals(draft, reopened.workoutDao().observeActiveDraft().first()?.toDomain())
        } finally {
            reopened.close()
        }
    }
}
