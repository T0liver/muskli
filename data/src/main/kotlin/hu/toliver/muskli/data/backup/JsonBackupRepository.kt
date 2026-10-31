package hu.toliver.muskli.data.backup

import androidx.room.withTransaction
import hu.toliver.muskli.data.db.MuskliDatabase
import hu.toliver.muskli.domain.repo.BackupRepository
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.ImportError
import hu.toliver.muskli.domain.repo.ImportProblem
import hu.toliver.muskli.domain.repo.ImportResult
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.settings.Settings
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class JsonBackupRepository(
    private val db: MuskliDatabase,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) : BackupRepository {
    override suspend fun export(): String {
        val (templates, workouts, routines) = db.withTransaction {
            val dao = db.backupDao()
            Triple(dao.exportedTemplates(), dao.allWorkouts(), dao.allRoutines())
        }
        val file = BackupFile(
            schemaVersion = BackupFile.CURRENT_SCHEMA_VERSION,
            exportedAtMs = clock.nowEpochMs(),
            exercises = templates.map { it.toJson() },
            workouts = workouts.map { it.toJson() },
            settings = settingsRepository.settings.first().toJson(),
            routines = routines.map { it.toJson() },
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    override suspend fun import(json: String): ImportResult {
        val file = try {
            Companion.json.decodeFromString(BackupFile.serializer(), json)
        } catch (e: SerializationException) {
            return rejected(ImportProblem.MALFORMED, e.message.orEmpty())
        } catch (e: IllegalArgumentException) {
            return rejected(ImportProblem.MALFORMED, e.message.orEmpty())
        }
        val (backup, errors) = file.validate()
        if (backup == null) return ImportResult.Rejected(errors)

        val conflicts = db.withTransaction {
            val dao = db.backupDao()
            val catalogueIds = dao.catalogueTemplateIds().toSet()
            val conflicts = backup.templates.withIndex()
                .filter { (_, t) -> t.isCustom && t.id in catalogueIds }
                .map { (i, _) -> ImportError(ImportProblem.ID_CONFLICT, "exercises[$i].id") }
            if (conflicts.isNotEmpty()) return@withTransaction conflicts

            dao.deleteAllWorkouts()
            // Before the custom exercises, which routine exercises reference with RESTRICT.
            dao.deleteAllRoutines()
            dao.deleteCustomTemplates()
            val inserted = backup.templates.zip(dao.insertTemplatesIfMissing(backup.templates))
                .filter { (_, rowId) -> rowId != -1L }
                .mapTo(mutableSetOf()) { (t, _) -> t.id }
            // A catalogue exercise already on the device keeps its own muscles.
            dao.insertSecondaryMusclesIfMissing(backup.secondaryMuscles.filter { it.exerciseTemplateId in inserted })
            dao.insertRoutines(backup.routines)
            dao.insertRoutineExercises(backup.routineExercises)
            dao.insertRoutineSets(backup.routineSets)
            dao.insertWorkouts(backup.workouts)
            dao.insertExercises(backup.exercises)
            dao.insertSets(backup.sets)
            emptyList()
        }
        if (conflicts.isNotEmpty()) return ImportResult.Rejected(conflicts)

        // DataStore cannot join the Room transaction; the values were validated above, so these writes cannot reject them.
        backup.settings?.let { restore(it) }
        return ImportResult.Imported(
            workouts = backup.workouts.size,
            customExercises = backup.templates.count { it.isCustom },
        )
    }

    private suspend fun restore(settings: Settings) = with(settingsRepository) {
        setWeightUnit(settings.weightUnit)
        setDefaultRestSeconds(settings.defaultRestSeconds)
        setPreviousMode(settings.previousMode)
        setKeepScreenAwake(settings.keepScreenAwake)
        setSoundEnabled(settings.soundEnabled)
        setVibrationEnabled(settings.vibrationEnabled)
        setWeekStart(settings.weekStart)
        setBodyWeightKg(settings.bodyWeightKg)
    }

    private fun rejected(problem: ImportProblem, where: String) = ImportResult.Rejected(listOf(ImportError(problem, where)))

    private companion object {
        val json = Json {
            prettyPrint = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }
    }
}
