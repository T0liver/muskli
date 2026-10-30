package hu.toliver.muskli.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single row: the catalogue version last applied, written in the same transaction as its exercises. */
@Entity(tableName = "catalogue_meta")
data class CatalogueMetaEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val version: Int,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
