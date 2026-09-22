package dev.sautao.productbase.demo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.sautao.productbase.core.database.InstantConverters

/**
 * Sample database for the demo shell.
 *
 * Version 2 exists to demonstrate the migration path, not because the demo needs a second
 * column: schemas for both versions are committed under `demo/schemas`, and
 * `SampleEntryMigrationTest` runs the real migration against the real version 1 schema.
 */
@Database(entities = [SampleEntryEntity::class], version = 2, exportSchema = true)
@TypeConverters(InstantConverters::class)
abstract class DemoDatabase : RoomDatabase() {
    abstract fun sampleEntryDao(): SampleEntryDao
}

/**
 * Adds a nullable column — the one migration shape that is always safe, because existing rows
 * need no value.
 *
 * Destructive fallback is deliberately never configured anywhere in this repository: a missing
 * migration must fail loudly in development rather than delete a user's data in production.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sample_entry ADD COLUMN note TEXT")
    }
}

val DEMO_DATABASE_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
