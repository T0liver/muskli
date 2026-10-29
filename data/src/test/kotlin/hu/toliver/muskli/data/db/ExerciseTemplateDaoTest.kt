package hu.toliver.muskli.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.domain.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseTemplateDaoTest {
    private lateinit var db: MuskliDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MuskliDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `template with secondary muscles reads back equal`() = runTest {
        val dao = db.exerciseTemplateDao()

        dao.upsert(benchPress.toEntity(), benchPress.secondaryMuscleEntities())
        dao.upsert(dumbbellRow.toEntity(), dumbbellRow.secondaryMuscleEntities())

        assertEquals(benchPress, dao.get(benchPress.id)?.toDomain())
        assertEquals(listOf(benchPress, dumbbellRow), dao.observeAll().first().map { it.toDomain() })
    }

    @Test
    fun `upsert replaces the secondary muscles`() = runTest {
        val dao = db.exerciseTemplateDao()
        dao.upsert(benchPress.toEntity(), benchPress.secondaryMuscleEntities())
        val edited = benchPress.copy(secondaryMuscles = setOf(MuscleGroup.FOREARMS), archived = true, updatedAtEpochMs = 9_000L)

        dao.upsert(edited.toEntity(), edited.secondaryMuscleEntities())

        assertEquals(edited, dao.get(benchPress.id)?.toDomain())
    }
}
