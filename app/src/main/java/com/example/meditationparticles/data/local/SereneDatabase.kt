package com.example.meditationparticles.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AffirmationEntity::class, ThoughtDumpEntity::class, SessionEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class SereneDatabase : RoomDatabase() {
    abstract fun affirmationDao(): AffirmationDao
    abstract fun thoughtDumpDao(): ThoughtDumpDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var instance: SereneDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS thought_dumps (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        detail TEXT,
                        durationSeconds INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE thought_dumps ADD COLUMN logType TEXT NOT NULL DEFAULT 'THOUGHT_DUMP'
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    ALTER TABLE thought_dumps ADD COLUMN audioPath TEXT
                    """.trimIndent(),
                )
            }
        }

        fun getInstance(context: Context): SereneDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SereneDatabase::class.java,
                    "serene_interval.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
