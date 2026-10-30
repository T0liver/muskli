package hu.toliver.muskli.data.seed

import hu.toliver.muskli.data.db.toEnum
import hu.toliver.muskli.domain.catalogue.CatalogueBundle
import hu.toliver.muskli.domain.catalogue.CatalogueExercise
import hu.toliver.muskli.domain.catalogue.SUPPORTED_CATALOGUE_FORMAT
import hu.toliver.muskli.domain.model.MuscleGroup
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads the catalogue format of docs/CATALOGUE.md with the platform `org.json`. Unknown fields are
 * ignored so a later optional field does not break this build. For an unknown `formatVersion` the
 * exercises are not read (they may use values this build lacks) and the bundle comes back empty,
 * which `planCatalogueUpdate` refuses.
 *
 * @throws org.json.JSONException or IllegalArgumentException for a malformed file.
 */
object CatalogueParser {
    fun parse(json: String): CatalogueBundle {
        val root = JSONObject(json)
        val formatVersion = root.getInt("formatVersion")
        val version = root.getInt("version")
        if (formatVersion != SUPPORTED_CATALOGUE_FORMAT) return CatalogueBundle(formatVersion, version, emptyList())
        val exercises = root.getJSONArray("exercises")
        return CatalogueBundle(
            formatVersion = formatVersion,
            version = version,
            exercises = List(exercises.length()) { exercises.getJSONObject(it).toExercise() },
        )
    }

    private fun JSONObject.toExercise() = CatalogueExercise(
        id = getString("id"),
        title = getString("title"),
        type = getString("type").toEnum(),
        primaryMuscle = getString("primaryMuscle").toEnum(),
        secondaryMuscles = getJSONArray("secondaryMuscles").strings().map { it.toEnum<MuscleGroup>() }.toSet(),
        equipment = getString("equipment").toEnum(),
        unilateral = if (has("unilateral")) getBoolean("unilateral") else false,
    )

    private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }
}
