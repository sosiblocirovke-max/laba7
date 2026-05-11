package com.example.lab7.data.mapper

import com.example.lab7.data.local.PostEntity
import com.example.lab7.data.remote.PostDto
import com.example.lab7.domain.model.Post

fun PostDto.toEntity(lastUpdated: Long = System.currentTimeMillis()): PostEntity =
    PostEntity(
        id = id,
        userId = userId,
        title = title,
        body = body,
        lastUpdated = lastUpdated
    )

fun PostEntity.toDomain(): Post =
    Post(
        id = id,
        userId = userId,
        title = title,
        body = body
    )

fun Post.toEntity(lastUpdated: Long = System.currentTimeMillis()): PostEntity =
    PostEntity(
        id = id,
        userId = userId,
        title = title,
        body = body,
        lastUpdated = lastUpdated
    )

fun PostDto.toDomain(): Post = toEntity().toDomain()

fun PostEntity.toDto(): PostDto =
    PostDto(
        userId = userId,
        id = id,
        title = title,
        body = body
    )
