package com.ornitrack.raw.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureCommentDao {
    @Query("SELECT * FROM capture_comments WHERE captureId = :captureId ORDER BY createdAt DESC")
    fun getCommentsForCapture(captureId: String): Flow<List<CaptureCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CaptureCommentEntity)

    @Delete
    suspend fun deleteComment(comment: CaptureCommentEntity)
}