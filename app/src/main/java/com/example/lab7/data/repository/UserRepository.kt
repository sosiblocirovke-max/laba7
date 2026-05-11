package com.example.lab7.data.repository

import com.example.lab7.data.local.UserDao
import com.example.lab7.data.mapper.toDomain
import com.example.lab7.data.mapper.toEntity
import com.example.lab7.data.remote.ApiService
import com.example.lab7.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext

class UserRepository(
    private val api: ApiService,
    private val userDao: UserDao
) {

    fun observeUsers(): Flow<List<User>> =
        userDao.getAll()
            .map { entities -> entities.map { it.toDomain() } }
            .transform { users ->
                if (users.isEmpty()) {
                    refreshUsers()
                }
                emit(users)
            }
            .flowOn(Dispatchers.IO)

    suspend fun refreshUsers(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val entities = api.getUsers().map { it.toEntity(now) }
                userDao.insertAll(entities)
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    fun observeUserById(id: Int): Flow<User?> =
        userDao.getById(id)
            .map { entity -> entity?.toDomain() }
            .transform { user ->
                if (user == null) {
                    getUserById(id)
                }
                emit(user)
            }
            .flowOn(Dispatchers.IO)

    suspend fun getUserById(id: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val dto = api.getUserById(id)
                userDao.insertAll(listOf(dto.toEntity(now)))
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    suspend fun updateUser(user: User): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                userDao.update(user.toEntity(System.currentTimeMillis()))
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    suspend fun deleteUser(id: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                userDao.deleteById(id)
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }
}
