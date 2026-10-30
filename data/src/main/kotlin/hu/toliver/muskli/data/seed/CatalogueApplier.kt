package hu.toliver.muskli.data.seed

import androidx.room.withTransaction
import hu.toliver.muskli.data.db.MuskliDatabase
import hu.toliver.muskli.data.db.entity.CatalogueMetaEntity
import hu.toliver.muskli.data.db.secondaryMuscleEntities
import hu.toliver.muskli.data.db.toDomain
import hu.toliver.muskli.data.db.toEntity
import hu.toliver.muskli.domain.catalogue.CatalogueBundle
import hu.toliver.muskli.domain.catalogue.planCatalogueUpdate
import hu.toliver.muskli.domain.repo.Clock

/** Applies a catalogue bundle from any source (bundled asset here, network sync in T-021). */
class CatalogueApplier(
    private val db: MuskliDatabase,
    private val clock: Clock,
) {
    /** True when [bundle] was applied; false when it was not newer or not a known format. */
    suspend fun apply(bundle: CatalogueBundle): Boolean = db.withTransaction {
        val templates = db.exerciseTemplateDao()
        val now = clock.nowEpochMs()
        val update = planCatalogueUpdate(
            bundle = bundle,
            storedVersion = db.catalogueMetaDao().version(),
            existing = templates.getAll().map { it.toDomain() },
            referencedIds = templates.referencedTemplateIds().toSet(),
            nowEpochMs = now,
        ) ?: return@withTransaction false
        update.upserts.forEach { templates.upsert(it.toEntity(), it.secondaryMuscleEntities()) }
        // Stay under SQLite's bound-variable limit for large catalogues.
        update.archiveIds.chunked(500).forEach { templates.archive(it, now) }
        db.catalogueMetaDao().setVersion(CatalogueMetaEntity(version = update.version))
        true
    }
}
