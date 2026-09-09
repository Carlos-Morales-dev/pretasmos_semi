package com.example.myapplication.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AuthRepository
import com.example.myapplication.data.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Inactivo : LoginUiState()
    object Cargando : LoginUiState()
    data class Exito(val usuario: Usuario) : LoginUiState()
    data class RegistroPendienteVerificacion(val correo: String) : LoginUiState()
    data class Error(val mensaje: String) : LoginUiState()
}

/** Valida que el correo tenga un formato real (usuario@dominio.ext). */
private fun esCorreoValido(correo: String): Boolean =
    Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()

private fun traducirErrorFirebase(e: Throwable): String {
    val msg = e.message ?: ""
    return when {
        msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
            "Permisos insuficientes en Firebase Firestore. Recuerda actualizar las reglas en tu consola."
        msg.contains("password is invalid", ignoreCase = true) ||
        msg.contains("wrong-password", ignoreCase = true) ||
        msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
            "Correo o contraseña incorrectos. Verifica tus credenciales."
        msg.contains("user-not-found", ignoreCase = true) ||
        msg.contains("no user record", ignoreCase = true) ->
            "No se encontró una cuenta con este correo. ¿Deseas registrarte?"
        msg.contains("email-already-in-use", ignoreCase = true) ->
            "Ya existe una cuenta registrada con este correo electrónico."
        msg.contains("network error", ignoreCase = true) ||
        msg.contains("UNAVAILABLE", ignoreCase = true) ->
            "Error de red. Verifica tu conexión a internet."
        msg.contains("too-many-requests", ignoreCase = true) ->
            "Demasiados intentos fallidos. Espera un momento antes de volver a intentar."
        msg.isNotBlank() -> msg
        else -> "Error al iniciar sesión"
    }
}

class LoginViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Inactivo)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(correo: String, password: String) {
        if (correo.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Completa correo y contraseña")
            return
        }
        if (!esCorreoValido(correo)) {
            _uiState.value = LoginUiState.Error("Ingresa un correo electrónico válido")
            return
        }
        _uiState.value = LoginUiState.Cargando
        viewModelScope.launch {
            val resultado = repository.login(correo, password)
            _uiState.value = resultado.fold(
                onSuccess = { LoginUiState.Exito(it) },
                onFailure = { LoginUiState.Error(traducirErrorFirebase(it)) }
            )
        }
    }

    /**
     * Registro de un nuevo monitor del área de deportes.
     * Todo el que se registre aquí queda con rol ADMIN, porque esta pantalla
     * es exclusivamente para monitores, no para estudiantes.
     */
    fun registrar(nombre: String, correo: String, password: String, repetirPassword: String = password) {
        if (nombre.isBlank() || correo.isBlank() || password.isBlank() || repetirPassword.isBlank()) {
            _uiState.value = LoginUiState.Error("Completa todos los campos obligatorios")
            return
        }
        if (!esCorreoValido(correo)) {
            _uiState.value = LoginUiState.Error("Ingresa un correo electrónico válido")
            return
        }
        if (password != repetirPassword) {
            _uiState.value = LoginUiState.Error("Las contraseñas no coinciden")
            return
        }
        if (password.length < 6) {
            _uiState.value = LoginUiState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }
        _uiState.value = LoginUiState.Cargando
        viewModelScope.launch {
            val nuevoUsuario = Usuario(nombre = nombre, codigo = "", correo = correo, rol = "ADMIN")
            val resultado = repository.registrar(nuevoUsuario, password)
            _uiState.value = resultado.fold(
                onSuccess = { LoginUiState.RegistroPendienteVerificacion(correo) },
                onFailure = { LoginUiState.Error(traducirErrorFirebase(it)) }
            )
        }
    }

    /** Reinicia el estado, útil al volver a la pantalla de login tras el registro. */
    fun reiniciarEstado() {
        _uiState.value = LoginUiState.Inactivo
    }
}
