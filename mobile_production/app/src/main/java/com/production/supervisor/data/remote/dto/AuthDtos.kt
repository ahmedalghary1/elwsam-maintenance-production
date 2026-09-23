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
data class NextSupervisorDto(
    val id: Int,
    val name: String,
    val phone: String = ""
)

@Serializable
data class UserDto(
    val id: Int,
    val phone: String,
    val name: String = "",
    val role: String = "",
    val shift: String? = null,
    @SerialName("shift_display") val shiftDisplay: String? = null,
    @SerialName("next_shift") val nextShift: String? = null,
    @SerialName("next_shift_display") val nextShiftDisplay: String? = null,
    @SerialName("next_shift_supervisor") val nextShiftSupervisor: NextSupervisorDto? = null
)
