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
interface UserDao {

    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getById(id: Int): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>): List<Long>

    @Update
    suspend fun update(user: UserEntity): Int

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    @Query("DELETE FROM users")
    suspend fun deleteAll(): Int
}
