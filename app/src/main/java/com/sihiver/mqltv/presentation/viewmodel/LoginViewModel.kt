package com.sihiver.mqltv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv.domain.usecase.LoginUseCase
import com.sihiver.mqltv.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class LoginUiState(
    val email: String = "ahmad@email.com",
    val password: String = "",
    val isLoading: Boolean = false,
    val isCheckingSession: Boolean = true,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val restored = userRepository.restoreSession()
            _state.update {
                it.copy(
                    isCheckingSession = false,
                    isLoggedIn = restored,
                )
            }
        }
    }

    fun setEmail(value: String) {
        _state.update { it.copy(email = value, error = null) }
    }

    fun setPassword(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun useDemoAccount() {
        _state.update {
            it.copy(
                email = "ahmad@email.com",
                password = "password123",
                error = null,
            )
        }
    }

    fun login() {
        val email = _state.value.email.trim()
        val password = _state.value.password
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Email dan password wajib diisi") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                loginUseCase(email, password)
                _state.update {
                    it.copy(isLoading = false, isLoggedIn = true, error = null)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = parseError(e),
                    )
                }
            }
        }
    }

    private fun parseError(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            401 -> "Email atau password salah"
            403 -> "Akun diblokir atau tidak punya akses"
            else -> "Login gagal (${e.code()})"
        }
        else -> e.message ?: "Tidak dapat terhubung ke server"
    }
}
