package com.featherframe.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureLikeDao {
    @Query("SELECT COUNT(*) FROM capture_likes WHERE captureId = :captureId")
    fun getLikeCount(captureId: String): Flow<Int>

    @Query("SELECT * FROM capture_likes WHERE captureId = :captureId AND photographerId = :photographerId")
    suspend fun getLike(captureId: String, photographerId: String): CaptureLikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: CaptureLikeEntity)

    @Delete
    suspend fun deleteLike(like: CaptureLikeEntity)

    @Query("DELETE FROM capture_likes WHERE captureId = :captureId AND photographerId = :photographerId")
    suspend fun removeLike(captureId: String, photographerId: String)
}
