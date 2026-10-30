package hu.toliver.muskli.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.data.db.MuskliDatabase
import hu.toliver.muskli.data.db.dumbbellRow
import hu.toliver.muskli.data.db.secondaryMuscleEntities
import hu.toliver.muskli.data.db.toDomain
import hu.toliver.muskli.data.db.toEntity
import hu.toliver.muskli.data.db.toRows
import hu.toliver.muskli.data.db.workout
import hu.toliver.muskli.domain.catalogue.CatalogueBundle
import hu.toliver.muskli.domain.catalogue.CatalogueExercise
import hu.toliver.muskli.domain.model.ExerciseTemplate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogueLoaderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, MuskliDatabase::class.java).allowMainThreadQueries().build()
    private var now = 1_000L
    private val applier = CatalogueApplier(db) { now }
    private val loader = BundledCatalogueLoader(context, applier)
    private val dao = db.exerciseTemplateDao()

    private val bundled = CatalogueParser.parse(
        context.assets.open(BundledCatalogueLoader.ASSET_PATH).bufferedReader().use { it.readText() },
    )

    @After
    fun tearDown() = db.close()

    private suspend fun library(): Map<String, ExerciseTemplate> = dao.getAll().map { it.toDomain() }.associateBy { it.id }

    private fun CatalogueBundle.newer(transform: (List<CatalogueExercise>) -> List<CatalogueExercise>) =
        copy(version = version + 1, exercises = transform(exercises))

    @Test
    fun `first launch with an empty database loads the bundled catalogue`() = runTest {
        assertNull(db.catalogueMetaDao().version())

        assertTrue(loader.load())

        val library = library()
        assertEquals(bundled.exercises.map { it.id }.toSet(), library.keys)
        assertEquals(10, library.size)
        assertTrue(library.values.none { it.isCustom || it.archived })
        assertEquals(bundled.version, db.catalogueMetaDao().version())
    }

    @Test
    fun `second launch at the same version does not duplicate or rewrite`() = runTest {
        loader.load()
        val first = library()
        now = 2_000L

        assertFalse(loader.load())

        assertEquals(first, library())
    }

    @Test
    fun `newer bundle upserts by id`() = runTest {
        loader.load()
        val renamed = bundled.newer { list -> list.map { if (it.id == "plank") it.copy(title = "Front Plank") else it } }
        now = 2_000L

        assertTrue(applier.apply(renamed))

        val plank = library().getValue("plank")
        assertEquals("Front Plank", plank.title)
        assertEquals(1_000L, plank.createdAtEpochMs)
        assertEquals(2_000L, plank.updatedAtEpochMs)
        assertEquals(10, library().size)
        assertEquals(renamed.version, db.catalogueMetaDao().version())
    }

    @Test
    fun `older or equal bundle is not applied`() = runTest {
        val newer = bundled.newer { list -> list.map { it.copy(title = it.title + " v2") } }
        applier.apply(newer)
        val stored = library()

        assertFalse(applier.apply(newer.copy(exercises = bundled.exercises)))
        assertFalse(loader.load())

        assertEquals(stored, library())
        assertEquals(newer.version, db.catalogueMetaDao().version())
    }

    @Test
    fun `custom exercises are untouched by the loader`() = runTest {
        dao.upsert(dumbbellRow.toEntity(), dumbbellRow.secondaryMuscleEntities())
        loader.load()
        applier.apply(bundled.newer { emptyList() })

        assertEquals(dumbbellRow, library().getValue(dumbbellRow.id))
    }

    @Test
    fun `missing exercises are archived unless a workout references them`() = runTest {
        loader.load()
        val usedInWorkout = workout().let { w ->
            w.copy(exercises = w.exercises.map { it.copy(exerciseTemplateId = "plank") })
        }
        val rows = usedInWorkout.toRows()
        db.workoutDao().save(rows.workout, rows.exercises, rows.sets)

        applier.apply(bundled.newer { list -> list.filter { it.id != "plank" && it.id != "pull-up" } })

        val library = library()
        assertFalse(library.getValue("plank").archived)
        assertTrue(library.getValue("pull-up").archived)
        assertEquals(10, library.size)
    }
}
