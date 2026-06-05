package com.sihiver.mqltv.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sihiver.mqltv.data.datastore.UserPreferences
import com.sihiver.mqltv.domain.repository.FavoriteRepository
import com.sihiver.mqltv.domain.repository.UserRepository
import com.sihiver.mqltv.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
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
    private val favoriteRepository: FavoriteRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = userPreferences.getLoginCredentialsOnce()
            val restored = userRepository.restoreSession()
            if (restored) {
                launch { runCatching { favoriteRepository.syncFromApi() } }
            }
            _state.update {
                it.copy(
                    isCheckingSession = false,
                    isLoggedIn = restored,
                    email = if (!restored) saved?.email.orEmpty() else it.email,
                    password = if (!restored) saved?.password.orEmpty() else it.password,
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

    fun markLoggedOut() {
        viewModelScope.launch {
            val saved = userPreferences.getLoginCredentialsOnce()
            _state.update {
                it.copy(
                    isLoggedIn = false,
                    isCheckingSession = false,
                    email = saved?.email.orEmpty(),
                    password = saved?.password.orEmpty(),
                    error = null,
                )
            }
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
                runCatching { userRepository.logout() }
                loginUseCase(email, password)
                userPreferences.saveLoginCredentials(email, password)
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        error = null,
                    )
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
