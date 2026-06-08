package com.featherframe.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BirdDao {
    @Query("SELECT * FROM birds WHERE birdId = :id")
    suspend fun getBirdById(id: String): BirdEntity?

    @Query("SELECT * FROM birds")
    fun getAllBirds(): Flow<List<BirdEntity>>

    @Query("SELECT * FROM birds WHERE speciesName LIKE '%' || :query || '%' OR scientificName LIKE '%' || :query || '%'")
    fun searchBirds(query: String): Flow<List<BirdEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBird(bird: BirdEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBirds(birds: List<BirdEntity>)
}
