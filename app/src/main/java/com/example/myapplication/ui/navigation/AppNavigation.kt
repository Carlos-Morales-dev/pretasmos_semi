package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.screens.HomeScreen
import com.example.myapplication.ui.screens.inventario.InventarioScreen
import com.example.myapplication.ui.screens.login.LoginScreen
import com.example.myapplication.ui.screens.login.RegistroScreen
import com.example.myapplication.ui.screens.prestamo.RegistrarDevolucionScreen
import com.example.myapplication.ui.screens.prestamo.RegistrarPrestamoScreen
import com.example.myapplication.utils.SesionActual

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginExitoso = { usuario ->
                    SesionActual.usuario = usuario
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onIrARegistro = { navController.navigate("registro") }
            )
        }

        composable("registro") {
            RegistroScreen(
                onVolverALogin = { navController.popBackStack() }
            )
        }

        composable("home") {
            HomeScreen(
                usuario = SesionActual.usuario,
                onRegistrarPrestamo = { navController.navigate("registrar_prestamo") },
                onRegistrarDevolucion = { navController.navigate("registrar_devolucion") },
                onMirarInventario = { navController.navigate("inventario") },
                onCerrarSesion = {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    SesionActual.usuario = null
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable("registrar_prestamo") {
            RegistrarPrestamoScreen(
                onVolver = { navController.popBackStack() }
            )
        }

        composable("registrar_devolucion") {
            RegistrarDevolucionScreen(
                onVolver = { navController.popBackStack() }
            )
        }

        composable("inventario") {
            InventarioScreen(
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
