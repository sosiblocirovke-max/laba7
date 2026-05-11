package com.example.lab7.data.remote

data class AddressDto(
    val city: String? = null
)

data class CompanyDto(
    val name: String? = null
)

data class UserDto(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val phone: String = "",
    val website: String = "",
    val address: AddressDto? = null,
    val company: CompanyDto? = null
)
