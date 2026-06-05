package com.example.meditationparticles.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    version = 15,
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
