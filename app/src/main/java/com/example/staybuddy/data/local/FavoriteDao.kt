package com.example.staybuddy.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT listingId FROM favorites WHERE userId = :userId")
    fun getFavoriteIds(userId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE listingId = :listingId AND userId = :userId")
    suspend fun deleteFavorite(listingId: String, userId: String)

    @Query("DELETE FROM favorites WHERE userId = :userId AND listingId NOT IN (:currentIds)")
    suspend fun deleteRemovedFavorites(userId: String, currentIds: List<String>)

    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun clearFavorites(userId: String)

    @Transaction
    suspend fun syncFavorites(userId: String, favorites: List<FavoriteEntity>) {
        if (favorites.isEmpty()) {
            clearFavorites(userId)
        } else {
            val currentIds = favorites.map { it.listingId }
            deleteRemovedFavorites(userId, currentIds)
            insertFavorites(favorites)
        }
    }
}
