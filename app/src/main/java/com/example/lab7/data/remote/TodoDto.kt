package com.example.lab7.data.remote

data class TodoDto(
    val id: Int,
    val userId: Int,
    val title: String,
    val completed: Boolean
)

