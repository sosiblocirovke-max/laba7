package com.example.lab7.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmSuppressWildcards

@Dao
@JvmSuppressWildcards
interface TodoDao {

    @Query("SELECT * FROM todos WHERE userId = :userId ORDER BY id ASC")
    fun getByUserId(userId: Int): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<TodoEntity>): List<Long>

    @Query("DELETE FROM todos WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Int): Int
}

