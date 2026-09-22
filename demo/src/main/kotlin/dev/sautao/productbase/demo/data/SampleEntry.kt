package dev.sautao.productbase.demo.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Sample data for the demo shell.
 *
 * Entities live in the product, never in `core:database` — the base owns how Room is configured,
 * not what a product stores.
 */
@Entity(tableName = "sample_entry")
data class SampleEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    /** Added in schema version 2; see MIGRATION_1_2. */
    @ColumnInfo(name = "note") val note: String? = null,
)

@Dao
interface SampleEntryDao {
    @Query("SELECT * FROM sample_entry ORDER BY created_at DESC")
    fun observeAll(): Flow<List<SampleEntryEntity>>

    @Insert
    suspend fun insert(entry: SampleEntryEntity)

    @Query("DELETE FROM sample_entry")
    suspend fun deleteAll()
}
