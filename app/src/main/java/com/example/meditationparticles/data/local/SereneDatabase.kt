package com.example.meditationparticles.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AffirmationEntity::class,
        AffirmationReviewSessionEntity::class,
        ThoughtDumpEntity::class,
        MeditationReflectionEntity::class,
        SessionEntity::class,
        FutureSelfMessageEntity::class,
        RefactoringEntryEntity::class,
        CenterOfGravityEntryEntity::class,
        NvcEntryEntity::class,
        HeartsEntryEntity::class,
        OneNoteSyncMappingEntity::class,
        OneNoteSyncQueueEntity::class,
        MoodEntryEntity::class,
        LivingTreeTagEntity::class,
        LivingTreePersonEntity::class,
        LivingTreePersonTagCrossRef::class,
    ],
    version = 24,
    exportSchema = false,
)
abstract class SereneDatabase : RoomDatabase() {
    abstract fun affirmationDao(): AffirmationDao
    abstract fun affirmationReviewSessionDao(): AffirmationReviewSessionDao
    abstract fun thoughtDumpDao(): ThoughtDumpDao
    abstract fun meditationReflectionDao(): MeditationReflectionDao
    abstract fun sessionDao(): SessionDao
    abstract fun futureSelfMessageDao(): FutureSelfMessageDao
    abstract fun refactoringEntryDao(): RefactoringEntryDao
    abstract fun centerOfGravityEntryDao(): CenterOfGravityEntryDao
    abstract fun nvcEntryDao(): NvcEntryDao
    abstract fun heartsEntryDao(): HeartsEntryDao
    abstract fun oneNoteSyncDao(): OneNoteSyncDao
    abstract fun moodEntryDao(): MoodEntryDao
    abstract fun livingTreeTagDao(): LivingTreeTagDao
    abstract fun livingTreePersonDao(): LivingTreePersonDao
    abstract fun livingTreePersonTagDao(): LivingTreePersonTagDao

    companion object {
        @Volatile
        private var instance: SereneDatabase? = null

        fun getInstance(context: Context): SereneDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SereneDatabase::class.java,
                    "serene_interval.db",
                )
                    .addMigrations(*SERENE_DATABASE_MIGRATIONS)
                    .build()
                    .also { instance = it }
            }
    }
}
