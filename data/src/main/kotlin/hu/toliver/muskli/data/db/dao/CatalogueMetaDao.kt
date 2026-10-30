package hu.toliver.muskli.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import hu.toliver.muskli.data.db.entity.CatalogueMetaEntity

@Dao
interface CatalogueMetaDao {
    /** Null until a catalogue has been applied. */
    @Query("SELECT version FROM catalogue_meta WHERE id = ${CatalogueMetaEntity.SINGLE_ROW_ID}")
    suspend fun version(): Int?

    @Upsert
    suspend fun setVersion(meta: CatalogueMetaEntity)
}
