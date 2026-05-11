package com.example.lab7.data.mapper

import com.example.lab7.data.local.UserEntity
import com.example.lab7.data.remote.AddressDto
import com.example.lab7.data.remote.CompanyDto
import com.example.lab7.data.remote.UserDto
import com.example.lab7.domain.model.User

fun UserDto.toEntity(lastUpdated: Long = System.currentTimeMillis()): UserEntity =
    UserEntity(
        id = id,
        name = name,
        username = username,
        email = email,
        phone = phone,
        website = website,
        companyName = company?.name.orEmpty(),
        city = address?.city.orEmpty(),
        lastUpdated = lastUpdated
    )

fun UserEntity.toDomain(): User =
    User(
        id = id,
        name = name,
        username = username,
        email = email,
        phone = phone,
        website = website,
        companyName = companyName,
        city = city
    )

fun User.toEntity(lastUpdated: Long = System.currentTimeMillis()): UserEntity =
    UserEntity(
        id = id,
        name = name,
        username = username,
        email = email,
        phone = phone,
        website = website,
        companyName = companyName,
        city = city,
        lastUpdated = lastUpdated
    )

fun UserDto.toDomain(): User = toEntity().toDomain()

fun UserEntity.toDto(): UserDto =
    UserDto(
        id = id,
        name = name,
        username = username,
        email = email,
        phone = phone,
        website = website,
        address = AddressDto(city = city.takeIf { it.isNotEmpty() }),
        company = CompanyDto(name = companyName.takeIf { it.isNotEmpty() })
    )
