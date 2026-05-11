package com.example.lab7.ui.users

import com.example.lab7.domain.model.Post
import com.example.lab7.domain.model.User

data class UsersListContent(
    val users: List<User>,
    val isRefreshing: Boolean = false
)

data class UserDetailContent(
    val user: User,
    val posts: List<Post>,
    val isRefreshing: Boolean = false
)
