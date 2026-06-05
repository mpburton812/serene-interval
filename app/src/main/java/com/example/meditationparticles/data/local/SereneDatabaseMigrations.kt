package com.example.meditationparticles.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private class Migration1To2 : Migration(1, 2) {
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

private class Migration2To3 : Migration(2, 3) {
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

private class Migration3To4 : Migration(3, 4) {
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

private class Migration4To5 : Migration(4, 5) {
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

private class Migration5To6 : Migration(5, 6) {
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

private class Migration6To7 : Migration(6, 7) {
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

private class Migration7To8 : Migration(7, 8) {
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

private class Migration8To9 : Migration(8, 9) {
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

private class Migration9To10 : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE thought_dumps ADD COLUMN moodLevel INTEGER NOT NULL DEFAULT 3
            """.trimIndent(),
        )
    }
}

private class Migration10To11 : Migration(10, 11) {
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

private class Migration11To12 : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS thought_dumps_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                content TEXT NOT NULL,
                logType TEXT NOT NULL,
                moodLevel INTEGER,
                audioPath TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO thought_dumps_new (id, content, logType, moodLevel, audioPath, createdAt)
            SELECT id, content, logType, moodLevel, audioPath, createdAt FROM thought_dumps
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE thought_dumps")
        db.execSQL("ALTER TABLE thought_dumps_new RENAME TO thought_dumps")
        db.execSQL("ALTER TABLE meditation_reflections ADD COLUMN moodLevel INTEGER")
        db.execSQL("ALTER TABLE future_self_messages ADD COLUMN moodLevel INTEGER")
        db.execSQL("ALTER TABLE refactoring_entries ADD COLUMN moodLevel INTEGER")
        db.execSQL("ALTER TABLE center_of_gravity_entries ADD COLUMN moodLevel INTEGER")
        db.execSQL("ALTER TABLE nvc_entries ADD COLUMN moodLevel INTEGER")
    }
}

private class Migration12To13 : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE meditation_reflections ADD COLUMN audioPath TEXT
            """.trimIndent(),
        )
    }
}

private class Migration13To14 : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS thought_dumps_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                content TEXT NOT NULL,
                logType TEXT NOT NULL,
                moodLevel INTEGER,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO thought_dumps_new (id, content, logType, moodLevel, createdAt)
            SELECT id, content, logType, moodLevel, createdAt FROM thought_dumps
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE thought_dumps")
        db.execSQL("ALTER TABLE thought_dumps_new RENAME TO thought_dumps")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS future_self_messages_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                content TEXT NOT NULL,
                moodLevel INTEGER,
                scheduledAtMillis INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                delivered INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO future_self_messages_new (
                id, content, moodLevel, scheduledAtMillis, createdAtMillis, delivered
            )
            SELECT id, content, moodLevel, scheduledAtMillis, createdAtMillis, delivered
            FROM future_self_messages
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE future_self_messages")
        db.execSQL("ALTER TABLE future_self_messages_new RENAME TO future_self_messages")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS refactoring_entries_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                interpretation TEXT NOT NULL,
                actualFacts TEXT NOT NULL,
                explanation1 TEXT NOT NULL,
                explanation2 TEXT NOT NULL,
                explanation3 TEXT NOT NULL,
                moodLevel INTEGER,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO refactoring_entries_new (
                id, interpretation, actualFacts, explanation1, explanation2, explanation3,
                moodLevel, createdAt
            )
            SELECT id, interpretation, actualFacts, explanation1, explanation2, explanation3,
                moodLevel, createdAt
            FROM refactoring_entries
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE refactoring_entries")
        db.execSQL("ALTER TABLE refactoring_entries_new RENAME TO refactoring_entries")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS center_of_gravity_entries_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                thoughtsAndFeelings TEXT NOT NULL,
                bodyAndNeeds TEXT NOT NULL,
                moodLevel INTEGER,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO center_of_gravity_entries_new (
                id, thoughtsAndFeelings, bodyAndNeeds, moodLevel, createdAt
            )
            SELECT id, thoughtsAndFeelings, bodyAndNeeds, moodLevel, createdAt
            FROM center_of_gravity_entries
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE center_of_gravity_entries")
        db.execSQL("ALTER TABLE center_of_gravity_entries_new RENAME TO center_of_gravity_entries")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS nvc_entries_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                observation TEXT NOT NULL,
                feeling TEXT NOT NULL,
                need TEXT NOT NULL,
                request TEXT NOT NULL,
                moodLevel INTEGER,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO nvc_entries_new (
                id, observation, feeling, need, request, moodLevel, createdAt
            )
            SELECT id, observation, feeling, need, request, moodLevel, createdAt
            FROM nvc_entries
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE nvc_entries")
        db.execSQL("ALTER TABLE nvc_entries_new RENAME TO nvc_entries")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS meditation_reflections_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                reflection TEXT NOT NULL,
                moodLevel INTEGER,
                durationSeconds INTEGER NOT NULL,
                completedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO meditation_reflections_new (
                id, reflection, moodLevel, durationSeconds, completedAt
            )
            SELECT id, reflection, moodLevel, durationSeconds, completedAt
            FROM meditation_reflections
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE meditation_reflections")
        db.execSQL("ALTER TABLE meditation_reflections_new RENAME TO meditation_reflections")
    }
}

private class Migration14To15 : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE thought_dumps SET moodLevel = 4 WHERE moodLevel = 5")
        db.execSQL("UPDATE future_self_messages SET moodLevel = 4 WHERE moodLevel = 5")
        db.execSQL("UPDATE refactoring_entries SET moodLevel = 4 WHERE moodLevel = 5")
        db.execSQL("UPDATE center_of_gravity_entries SET moodLevel = 4 WHERE moodLevel = 5")
        db.execSQL("UPDATE nvc_entries SET moodLevel = 4 WHERE moodLevel = 5")
        db.execSQL("UPDATE meditation_reflections SET moodLevel = 4 WHERE moodLevel = 5")
    }
}

private class Migration15To16 : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS mood_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                moodLevel INTEGER NOT NULL,
                recordedAtMillis INTEGER NOT NULL,
                source TEXT NOT NULL,
                legacyTable TEXT,
                legacyRowId INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_mood_entries_legacyTable_legacyRowId
            ON mood_entries (legacyTable, legacyRowId)
            """.trimIndent(),
        )
    }
}

private class Migration16To17 : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS affirmations_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                text TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO affirmations_new (id, text, createdAt, sortOrder)
            SELECT id, text, createdAt, sortOrder FROM affirmations
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE affirmations")
        db.execSQL("ALTER TABLE affirmations_new RENAME TO affirmations")
    }
}

private class Migration17To18 : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS living_tree_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                colorArgb INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_living_tree_tags_name
            ON living_tree_tags (name)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS living_tree_people (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                angleRadians REAL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_living_tree_people_name
            ON living_tree_people (name)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS living_tree_person_tags (
                personId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                PRIMARY KEY(personId, tagId),
                FOREIGN KEY(personId) REFERENCES living_tree_people(id) ON DELETE CASCADE,
                FOREIGN KEY(tagId) REFERENCES living_tree_tags(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_living_tree_person_tags_tagId
            ON living_tree_person_tags (tagId)
            """.trimIndent(),
        )
    }
}

internal val SERENE_DATABASE_MIGRATIONS = arrayOf(
    Migration1To2(),
    Migration2To3(),
    Migration3To4(),
    Migration4To5(),
    Migration5To6(),
    Migration6To7(),
    Migration7To8(),
    Migration8To9(),
    Migration9To10(),
    Migration10To11(),
    Migration11To12(),
    Migration12To13(),
    Migration13To14(),
    Migration14To15(),
    Migration15To16(),
    Migration16To17(),
    Migration17To18(),
)
