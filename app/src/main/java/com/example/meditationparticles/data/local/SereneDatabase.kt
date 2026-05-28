package com.example.meditationparticles.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AffirmationEntity::class,
        ThoughtDumpEntity::class,
        MeditationReflectionEntity::class,
        SessionEntity::class,
        FutureSelfMessageEntity::class,
        RefactoringEntryEntity::class,
        CenterOfGravityEntryEntity::class,
        NvcEntryEntity::class,
        OneNoteSyncMappingEntity::class,
        OneNoteSyncQueueEntity::class,
    ],
    version = 11,
    exportSchema = false,
)
abstract class SereneDatabase : RoomDatabase() {
    abstract fun affirmationDao(): AffirmationDao
    abstract fun thoughtDumpDao(): ThoughtDumpDao
    abstract fun meditationReflectionDao(): MeditationReflectionDao
    abstract fun sessionDao(): SessionDao
    abstract fun futureSelfMessageDao(): FutureSelfMessageDao
    abstract fun refactoringEntryDao(): RefactoringEntryDao
    abstract fun centerOfGravityEntryDao(): CenterOfGravityEntryDao
    abstract fun nvcEntryDao(): NvcEntryDao
    abstract fun oneNoteSyncDao(): OneNoteSyncDao

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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS future_self_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        audioPath TEXT,
                        scheduledAtMillis INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        delivered INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS refactoring_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        interpretation TEXT NOT NULL,
                        interpretationAudioPath TEXT,
                        actualFacts TEXT NOT NULL,
                        actualFactsAudioPath TEXT,
                        explanation1 TEXT NOT NULL,
                        explanation1AudioPath TEXT,
                        explanation2 TEXT NOT NULL,
                        explanation2AudioPath TEXT,
                        explanation3 TEXT NOT NULL,
                        explanation3AudioPath TEXT,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS center_of_gravity_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        thoughtsAndFeelings TEXT NOT NULL,
                        thoughtsAndFeelingsAudioPath TEXT,
                        bodyAndNeeds TEXT NOT NULL,
                        bodyAndNeedsAudioPath TEXT,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS nvc_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        observation TEXT NOT NULL,
                        observationAudioPath TEXT,
                        feeling TEXT NOT NULL,
                        feelingAudioPath TEXT,
                        need TEXT NOT NULL,
                        needAudioPath TEXT,
                        request TEXT NOT NULL,
                        requestAudioPath TEXT,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS one_note_sync_mappings (
                        localEntryId INTEGER NOT NULL,
                        entryType TEXT NOT NULL,
                        oneNotePageId TEXT,
                        syncStatus TEXT NOT NULL,
                        lastError TEXT,
                        syncedAt INTEGER,
                        PRIMARY KEY (localEntryId, entryType)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS one_note_sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        localEntryId INTEGER NOT NULL,
                        entryType TEXT NOT NULL,
                        enqueuedAt INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE thought_dumps ADD COLUMN moodLevel INTEGER NOT NULL DEFAULT 3
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS meditation_reflections (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        reflection TEXT NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                    )
                    .build()
                    .also { instance = it }
            }
    }
}
