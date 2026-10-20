package com.elwsam.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUiState(
    val sessionLoaded: Boolean = false,
    val session: UnifiedSession? = null,
    val phone: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AppViewModel @Inject constructor(private val auth: UnifiedAuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            auth.session.collect { session ->
                _state.update { it.copy(sessionLoaded = true, session = session, loading = false) }
            }
        }
    }

    fun setPhone(value: String) = _state.update { it.copy(phone = value, error = null) }
    fun setPassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun togglePassword() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun login() {
        val current = _state.value
        if (current.loading) return
        if (current.phone.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(error = "أدخل رقم الهاتف وكلمة المرور.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            auth.login(current.phone, current.password)
                .onSuccess { _state.update { it.copy(loading = false, password = "") } }
                .onFailure {
                    _state.update { state ->
                        state.copy(
                            loading = false,
                            error = it.message?.takeIf { message -> message.isNotBlank() }
                                ?: "تعذر تسجيل الدخول. تحقق من البيانات والاتصال."
                        )
                    }
                }
        }
    }

    fun logout() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        auth.logout()
        _state.update { it.copy(loading = false, password = "") }
    }
}
