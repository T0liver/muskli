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
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.repo.Clock
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class CatalogueSyncTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, MuskliDatabase::class.java).allowMainThreadQueries().build()
    private var now = 1_000L
    private val applier = CatalogueApplier(
        db,
        object : Clock {
            override fun nowEpochMs() = now

            override fun utcOffsetS(epochMs: Long) = 0
        },
    )
    private val source = FakeCatalogueHttpSource()
    private val sync = CatalogueSync(source, applier)
    private val dao = db.exerciseTemplateDao()

    @After
    fun tearDown() = db.close()

    private suspend fun library(): Map<String, ExerciseTemplate> = dao.getAll().map { it.toDomain() }.associateBy { it.id }

    private suspend fun seed(version: Int, vararg exercises: String) {
        assertTrue(applier.apply(CatalogueParser.parse(catalogueJson(version, *exercises))))
    }

    @Test
    fun `newer remote bundle upserts by id`() = runTest {
        seed(1, exerciseJson("bench-press", "Bench Press"))
        now = 2_000L
        source.response = CatalogueHttpResponse(200, catalogueJson(2, exerciseJson("bench-press", "Barbell Bench Press")))

        assertTrue(sync.sync())

        val bench = library().getValue("bench-press")
        assertEquals("Barbell Bench Press", bench.title)
        assertEquals(1_000L, bench.createdAtEpochMs)
        assertEquals(2_000L, bench.updatedAtEpochMs)
        assertFalse(bench.isCustom)
        assertEquals(2, db.catalogueMetaDao().version())
    }

    @Test
    fun `older or equal remote bundle is not applied`() = runTest {
        seed(3, exerciseJson("bench-press", "Bench Press"), exerciseJson("plank", "Plank"))
        val stored = library()

        source.response = CatalogueHttpResponse(200, catalogueJson(3, exerciseJson("bench-press", "Renamed")))
        assertFalse(sync.sync())
        source.response = CatalogueHttpResponse(200, catalogueJson(2, exerciseJson("bench-press", "Older")))
        assertFalse(sync.sync())

        assertEquals(stored, library())
        assertEquals(3, db.catalogueMetaDao().version())
        assertFalse(library().getValue("plank").archived)
    }

    @Test
    fun `transport failure leaves the stored catalogue unchanged`() = runTest {
        seed(1, exerciseJson("bench-press", "Bench Press"))
        val stored = library()
        source.failure = IOException("offline")

        assertFalse(sync.sync())

        assertEquals(stored, library())
        assertEquals(1, db.catalogueMetaDao().version())
    }

    @Test
    fun `non-200 response leaves the stored catalogue unchanged`() = runTest {
        seed(1, exerciseJson("bench-press", "Bench Press"))
        val stored = library()
        source.response = CatalogueHttpResponse(404, catalogueJson(9, exerciseJson("bench-press", "Should Not Apply")))

        assertFalse(sync.sync())

        assertEquals(stored, library())
        assertEquals(1, db.catalogueMetaDao().version())
    }

    @Test
    fun `malformed json leaves the stored catalogue unchanged`() = runTest {
        seed(1, exerciseJson("bench-press", "Bench Press"))
        val stored = library()
        source.response = CatalogueHttpResponse(200, "{")

        assertFalse(sync.sync())

        assertEquals(stored, library())
        assertEquals(1, db.catalogueMetaDao().version())
    }

    @Test
    fun `unknown format version leaves the stored catalogue unchanged`() = runTest {
        seed(1, exerciseJson("bench-press", "Bench Press"))
        val stored = library()
        source.response = CatalogueHttpResponse(
            200,
            catalogueJson(9, exerciseJson("bench-press", "Future"), formatVersion = 2),
        )

        assertFalse(sync.sync())

        assertEquals(stored, library())
        assertEquals(1, db.catalogueMetaDao().version())
    }

    @Test
    fun `custom exercises are unchanged after sync`() = runTest {
        dao.upsert(dumbbellRow.toEntity(), dumbbellRow.secondaryMuscleEntities())
        seed(1, exerciseJson("bench-press", "Bench Press"))
        source.response = CatalogueHttpResponse(
            200,
            catalogueJson(
                2,
                exerciseJson("bench-press", "Barbell Bench Press"),
                exerciseJson(dumbbellRow.id, "Hijacked custom"),
            ),
        )

        assertTrue(sync.sync())

        assertEquals(dumbbellRow, library().getValue(dumbbellRow.id))
        assertEquals("Barbell Bench Press", library().getValue("bench-press").title)
    }

    @Test
    fun `referenced published exercise missing from the bundle is kept and an unreferenced one is archived`() = runTest {
        seed(1, exerciseJson("bench-press", "Bench Press"), exerciseJson("plank", "Plank"), exerciseJson("pull-up", "Pull Up"))
        val usedInWorkout = workout().let { w ->
            w.copy(exercises = w.exercises.map { it.copy(exerciseTemplateId = "plank") })
        }
        val rows = usedInWorkout.toRows()
        db.workoutDao().save(rows.workout, rows.exercises, rows.sets)
        source.response = CatalogueHttpResponse(200, catalogueJson(2, exerciseJson("bench-press", "Bench Press")))

        assertTrue(sync.sync())

        val library = library()
        assertEquals(setOf("bench-press", "plank", "pull-up"), library.keys)
        assertFalse(library.getValue("plank").archived)
        assertTrue(library.getValue("pull-up").archived)
        assertEquals(2, db.catalogueMetaDao().version())
    }
}

private class FakeCatalogueHttpSource : CatalogueHttpSource {
    var response: CatalogueHttpResponse? = null
    var failure: Exception? = null

    override fun fetch(): CatalogueHttpResponse {
        failure?.let { throw it }
        return response ?: error("no scripted response")
    }
}

private fun exerciseJson(id: String, title: String) = """
    {"id":"$id","title":"$title","type":"weight_reps","primaryMuscle":"chest","secondaryMuscles":[],"equipment":"barbell"}
""".trimIndent()

private fun catalogueJson(version: Int, vararg exercises: String, formatVersion: Int = 1) = """
    {"formatVersion":$formatVersion,"version":$version,"license":"CC0-1.0","exercises":[${exercises.joinToString(",")}],"retiredIds":[]}
""".trimIndent()
