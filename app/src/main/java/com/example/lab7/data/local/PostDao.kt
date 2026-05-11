package com.example.lab7.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmSuppressWildcards

@Dao
@JvmSuppressWildcards
interface PostDao {

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY id ASC")
    fun getByUserId(userId: Int): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>): List<Long>

    @Update
    suspend fun update(post: PostEntity): Int

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    @Query("DELETE FROM posts WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Int): Int
}
