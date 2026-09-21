package com.production.supervisor.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val phone: String,
    val password: String
)

@Serializable
data class LoginResponseDto(
    val access: String,
    val refresh: String,
    val user: UserDto? = null
)

@Serializable
data class UserDto(
    val id: Int,
    val phone: String,
    val name: String = "",
    val role: String = ""
)
