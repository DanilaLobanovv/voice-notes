package com.voicenotes.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenotes.app.data.ApiException
import com.voicenotes.app.data.NotesRepository
import kotlinx.coroutines.launch

/** Состояние и действия экрана входа и регистрации. */
class AuthViewModel(private val repo: NotesRepository) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isRegister by mutableStateOf(false)
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    fun toggleMode() {
        isRegister = !isRegister
        error = null
    }

    fun submit() {
        if (email.isBlank() || password.isEmpty()) {
            error = "Введите email и пароль"
            return
        }
        if (isRegister && password.length < 8) {
            error = "Пароль должен быть не короче 8 символов"
            return
        }
        viewModelScope.launch {
            loading = true
            error = null
            try {
                if (isRegister) repo.register(email, password) else repo.login(email, password)
                password = ""
            } catch (e: ApiException) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }
}
