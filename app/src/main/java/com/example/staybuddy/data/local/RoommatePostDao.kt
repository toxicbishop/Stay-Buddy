package com.example.staybuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoommatePostDao {
    @Query("SELECT * FROM roommate_posts ORDER BY createdAt DESC")
    fun getAllRoommatePosts(): Flow<List<RoommatePostEntity>>

    @Query("SELECT * FROM roommate_posts WHERE postType = :postType ORDER BY createdAt DESC")
    fun getRoommatePostsByType(postType: String): Flow<List<RoommatePostEntity>>

    @Query("SELECT * FROM roommate_posts WHERE postId = :postId")
    suspend fun getRoommatePostById(postId: String): RoommatePostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoommatePosts(posts: List<RoommatePostEntity>)

    @Query("DELETE FROM roommate_posts")
    suspend fun clearAll()
}
