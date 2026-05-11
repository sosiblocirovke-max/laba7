package com.example.lab7.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("users")
    suspend fun getUsers(): List<UserDto>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Int): UserDto

    @GET("posts")
    suspend fun getPostsByUser(@Query("userId") userId: Int): List<PostDto>

    @GET("todos")
    suspend fun getTodosByUser(@Query("userId") userId: Int): List<TodoDto>
}
