package com.safehaven.affirmations.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SereneDatabaseMigrationTest {
    @Test
    fun migrateAllTheWayFromVersion1() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(TEST_DB)
        createVersion1Database(context)

        val db = Room.databaseBuilder(context, SereneDatabase::class.java, TEST_DB)
            .addMigrations(*SERENE_DATABASE_MIGRATIONS)
            .build()
        try {
            val sqlite = db.openHelper.writableDatabase
            sqlite.query("SELECT text, listKind FROM affirmations").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getString(0) == "Keep going")
                check(cursor.getString(1) == "Affirmations")
            }
            sqlite.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_hearts_entries_personId'",
            ).use { cursor ->
                check(cursor.moveToFirst()) { "hearts personId index missing after upgrade" }
            }
        } finally {
            db.close()
        }
    }

    private fun createVersion1Database(context: Context) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DB)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS affirmations (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                text TEXT NOT NULL,
                                createdAt INTEGER NOT NULL,
                                sortOrder INTEGER NOT NULL
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            "INSERT INTO affirmations (text, createdAt, sortOrder) VALUES ('Keep going', 1, 0)",
                        )
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                })
                .build(),
        )
        helper.writableDatabase.close()
        helper.close()
    }

    private companion object {
        const val TEST_DB = "serene-migration-test"
    }
}
