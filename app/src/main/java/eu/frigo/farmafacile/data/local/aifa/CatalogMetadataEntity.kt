package eu.frigo.farmafacile.data.local.aifa

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_metadata")
data class CatalogMetadataEntity(
    @PrimaryKey val key: String,
    val timestamp: Long,
    val count: Int
)
