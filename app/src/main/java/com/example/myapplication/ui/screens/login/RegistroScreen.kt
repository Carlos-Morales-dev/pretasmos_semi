package com.example.myapplication.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.viewmodel.LoginUiState
import com.example.myapplication.viewmodel.LoginViewModel

@Composable
fun RegistroScreen(
    onVolverALogin: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repetirPassword by remember { mutableStateOf("") }
    val estado by viewModel.uiState.collectAsState()

    val pendienteVerificacion = estado as? LoginUiState.RegistroPendienteVerificacion

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. IMAGEN DE FONDO
        Image(
            painter = painterResource(id = R.drawable.fondo_login),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. DEGRADADO
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // 3. TARJETA DE REGISTRO
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (pendienteVerificacion != null) {
                        Text(
                            "Revisa tu correo",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Te enviamos un enlace de confirmación a ${pendienteVerificacion.correo}. " +
                                    "Ábrelo para verificar tu cuenta y luego inicia sesión.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.reiniciarEstado()
                                onVolverALogin()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("ir_a_login_button")
                        ) {
                            Text("Ir a iniciar sesión", fontWeight = FontWeight.Bold)
                        }
                        return@Column
                    }

                    Text(
                        "Crear cuenta",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                    Text(
                        "Regístrate como monitor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre completo") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("nombre_input")
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Correo institucional") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("correo_input")
                    )
                    Spacer(Modifier.height(8.dp))

                    val passwordTieneError = password.isNotEmpty() && password.length < 6
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordTieneError,
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            if (passwordTieneError) {
                                Text("Al menos 6 caracteres")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("password_input")
                    )
                    Spacer(Modifier.height(8.dp))

                    val passwordsNoCoinciden = repetirPassword.isNotEmpty() && password != repetirPassword
                    OutlinedTextField(
                        value = repetirPassword,
                        onValueChange = { repetirPassword = it },
                        label = { Text("Repetir contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordsNoCoinciden,
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            if (passwordsNoCoinciden) {
                                Text("No coinciden")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("repetir_password_input")
                    )
                    Spacer(Modifier.height(16.dp))

                    if (estado is LoginUiState.Error) {
                        Text(
                            (estado as LoginUiState.Error).mensaje,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { viewModel.registrar(nombre, correo, password, repetirPassword) },
                        enabled = estado !is LoginUiState.Cargando,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("registrar_button")
                    ) {
                        if (estado is LoginUiState.Cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Registrarme", fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(onClick = onVolverALogin) {
                        Text(
                            "Ya tengo cuenta, iniciar sesión",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
