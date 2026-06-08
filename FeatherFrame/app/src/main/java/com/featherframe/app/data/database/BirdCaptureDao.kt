package com.featherframe.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BirdCaptureDao {
    @Query("SELECT * FROM bird_captures ORDER BY capturedAt DESC")
    fun getAllCaptures(): Flow<List<BirdCaptureEntity>>

    @Query("SELECT * FROM bird_captures WHERE captureId = :id")
    suspend fun getCaptureById(id: String): BirdCaptureEntity?

    @Query("SELECT * FROM bird_captures WHERE photographerId = :photographerId ORDER BY capturedAt DESC")
    fun getCapturesByPhotographer(photographerId: String): Flow<List<BirdCaptureEntity>>

    @Query("SELECT * FROM bird_captures WHERE isSynced = 0")
    suspend fun getUnsyncedCaptures(): List<BirdCaptureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: BirdCaptureEntity)

    @Update
    suspend fun updateCapture(capture: BirdCaptureEntity)

    @Query("UPDATE bird_captures SET isSynced = 1, gdriveFileId = :gdriveFileId WHERE captureId = :captureId")
    suspend fun markAsSynced(captureId: String, gdriveFileId: String)

    @Delete
    suspend fun deleteCapture(capture: BirdCaptureEntity)
}
