package com.production.supervisor.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.production.supervisor.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val phone: String = "",
    val pass: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onPhoneChange(phone: String) {
        _uiState.value = _uiState.value.copy(phone = phone, errorMessage = null)
    }

    fun onPassChange(pass: String) {
        _uiState.value = _uiState.value.copy(pass = pass, errorMessage = null)
    }

    fun login() {
        val state = _uiState.value
        if (state.phone.isBlank() || state.pass.isBlank()) {
            _uiState.value = state.copy(errorMessage = "يرجى كتابة رقم الهاتف وكلمة المرور")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val result = authRepository.login(state.phone, state.pass)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = ex.message ?: "فشل تسجيل الدخول، تأكد من صحة البيانات"
                )
            }
        }
    }
}
