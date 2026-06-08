package com.featherframe.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotographerDao {
    @Query("SELECT * FROM photographers WHERE photographerId = :id")
    suspend fun getPhotographerById(id: String): PhotographerEntity?

    @Query("SELECT * FROM photographers")
    fun getAllPhotographers(): Flow<List<PhotographerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotographer(photographer: PhotographerEntity)

    @Update
    suspend fun updatePhotographer(photographer: PhotographerEntity)

    @Query("UPDATE photographers SET bio = :bio, favoriteGear = :favoriteGear WHERE photographerId = :id")
    suspend fun updateProfile(id: String, bio: String?, favoriteGear: String?)

    @Query("DELETE FROM photographers WHERE photographerId = :id")
    suspend fun deletePhotographer(id: String)
}
