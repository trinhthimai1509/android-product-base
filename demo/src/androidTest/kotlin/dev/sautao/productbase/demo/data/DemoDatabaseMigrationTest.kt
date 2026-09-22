package dev.sautao.productbase.demo.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real migration against the real committed version 1 schema.
 *
 * This is the test that makes "never use destructive migration" an affordable policy: without
 * it, the only way to find out whether a migration works is to ship it.
 */
@RunWith(AndroidJUnit4::class)
class DemoDatabaseMigrationTest {
    private val databaseName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DemoDatabase::class.java,
    )

    @Test
    fun migrates1To2AndKeepsExistingRows() {
        helper.createDatabase(databaseName, 1).use { database ->
            database.execSQL(
                "INSERT INTO sample_entry (label, created_at) VALUES ('written at v1', 1755000000000)",
            )
        }

        // `validateDroppedTables = true` also checks the migrated schema against 2.json, so a
        // migration that runs but produces the wrong shape still fails here.
        val migrated = helper.runMigrationsAndValidate(databaseName, 2, true, MIGRATION_1_2)

        migrated.query("SELECT label, created_at, note FROM sample_entry").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("written at v1", cursor.getString(0))
            assertEquals(1_755_000_000_000L, cursor.getLong(1))
            // The column added by the migration is null for pre-existing rows, which is the
            // whole reason a nullable column is the safe migration shape.
            assertTrue(cursor.isNull(2))
        }
        migrated.close()
    }
}
