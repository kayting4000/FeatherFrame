package com.ornitrack.raw.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PhotographerEntity::class,
        BirdEntity::class,
        BirdCaptureEntity::class,
        CaptureLikeEntity::class,
        CaptureCommentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun photographerDao(): PhotographerDao
    abstract fun birdDao(): BirdDao
    abstract fun birdCaptureDao(): BirdCaptureDao
    abstract fun captureLikeDao(): CaptureLikeDao
    abstract fun captureCommentDao(): CaptureCommentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ornitrack_raw_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}