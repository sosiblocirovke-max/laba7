package com.example.lab7.data.repository

import com.example.lab7.data.local.PostDao
import com.example.lab7.data.mapper.toDomain
import com.example.lab7.data.mapper.toEntity
import com.example.lab7.data.remote.ApiService
import com.example.lab7.domain.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext

class PostRepository(
    private val api: ApiService,
    private val postDao: PostDao
) {

    fun observePostsByUser(userId: Int): Flow<List<Post>> =
        postDao.getByUserId(userId)
            .map { entities -> entities.map { it.toDomain() } }
            .transform { posts ->
                if (posts.isEmpty()) {
                    getPostsByUser(userId)
                }
                emit(posts)
            }
            .flowOn(Dispatchers.IO)

    suspend fun getPostsByUser(userId: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val dtos = api.getPostsByUser(userId)
                postDao.deleteByUserId(userId)
                postDao.insertAll(dtos.map { it.toEntity(now) })
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    suspend fun updatePost(post: Post): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                postDao.update(post.toEntity(System.currentTimeMillis()))
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    suspend fun deletePost(postId: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                postDao.deleteById(postId)
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    suspend fun deleteAllPostsForUser(userId: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                postDao.deleteByUserId(userId)
                Result.success(Unit)
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }
}
