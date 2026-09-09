package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Usuario
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    usuario: Usuario?,
    onRegistrarPrestamo: () -> Unit = {},
    onRegistrarDevolucion: () -> Unit = {},
    onMirarInventario: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    val nombre = usuario?.nombre?.ifBlank { "Monitor" } ?: "Monitor"
    val correo = usuario?.correo?.ifBlank { "monitor@udenar.edu.co" } ?: "monitor@udenar.edu.co"
    val rol = usuario?.rol?.ifBlank { "ADMIN" } ?: "ADMIN"

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Estados para los diálogos de las opciones de la barra lateral
    var mostrarDialogoPerfil by remember { mutableStateOf(false) }
    var mostrarDialogoReglamento by remember { mutableStateOf(false) }
    var mostrarDialogoAcercaDe by remember { mutableStateOf(false) }
    var mostrarConfirmacionCerrarSesion by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF131D14),
                drawerContentColor = Color.White,
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Cabecera del Drawer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil del usuario",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = correo,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA5D6A7)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Monitor • $rol",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF2A3D2C), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // =====================================
                    // SECCIÓN 1: OPCIONES DE USUARIO
                    // =====================================
                    Text(
                        text = "OPCIONES DE USUARIO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )

                    DrawerOpcionItem(
                        titulo = "Datos del Monitor",
                        subtitulo = "Ver información personal y credenciales",
                        icono = Icons.Default.Person,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            mostrarDialogoPerfil = true
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF2A3D2C), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // =====================================
                    // SECCIÓN 2: OPCIONES DE LA APLICACIÓN
                    // =====================================
                    Text(
                        text = "OPCIONES DE LA APLICACIÓN",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )

                    DrawerOpcionItem(
                        titulo = "Reglamento y Políticas",
                        subtitulo = "Normas de entrega y custodia de carnet",
                        icono = Icons.Default.Check,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            mostrarDialogoReglamento = true
                        }
                    )

                    DrawerOpcionItem(
                        titulo = "Acerca de la Aplicación",
                        subtitulo = "Versión del sistema y soporte",
                        icono = Icons.Default.Info,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            mostrarDialogoAcercaDe = true
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF2A3D2C), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // =====================================
                    // SECCIÓN 3: CERRAR SESIÓN
                    // =====================================
                    ElevatedButton(
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            mostrarConfirmacionCerrarSesion = true
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF3B1E1E),
                            contentColor = Color(0xFFFF8A80)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("cerrar_sesion_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cerrar sesión",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Cerrar Sesión",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFFF8A80)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Préstamos Deportivos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            modifier = Modifier.testTag("abrir_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir menú lateral",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF142416)
                    )
                )
            },
            containerColor = Color(0xFF0F170F)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // =========================================================================
                // ESPACIO DE PERFIL DE USUARIO DEL MONITOR (SOBRE EL TEXTO "BIENVENIDO")
                // Formato vertical: Ícono de usuario arriba, debajo Nombre, debajo Correo
                // =========================================================================
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFF162417)
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("perfil_monitor_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 22.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Ícono de usuario arriba
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .testTag("icono_usuario_perfil")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Foto de perfil del monitor",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Debajo: Nombre
                        Text(
                            text = nombre,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("nombre_monitor_text")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 3. Debajo de nombre: Correo
                        Text(
                            text = correo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA5D6A7),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("correo_monitor_text")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF243826)
                        ) {
                            Text(
                                text = "MONITOR EN TURNO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Texto Bienvenido debajo del Perfil
                Text(
                    text = "Bienvenido, $nombre",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Monitor del área de deportes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Menú Principal de Acciones
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HomeMenuButton(
                        text = "Registrar Préstamo",
                        icon = Icons.Default.Add,
                        onClick = onRegistrarPrestamo
                    )
                    HomeMenuButton(
                        text = "Registrar Devolución",
                        icon = Icons.Default.Refresh,
                        onClick = onRegistrarDevolucion
                    )
                    HomeMenuButton(
                        text = "Mirar Inventario",
                        icon = Icons.AutoMirrored.Filled.List,
                        onClick = onMirarInventario
                    )
                    HomeMenuButton(
                        text = "Mirar Reporte",
                        icon = Icons.Default.Info,
                        onClick = { /* Próxima funcionalidad */ }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // =========================================================================
    // DIÁLOGOS DE LAS OPCIONES DE LA BARRA LATERAL (IDEA DE LOS APARTADOS)
    // =========================================================================

    // Diálogo 1: Datos del Monitor
    if (mostrarDialogoPerfil) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPerfil = false },
            icon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(text = "Datos del Monitor", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DatoItem(etiqueta = "Nombre:", valor = nombre)
                    DatoItem(etiqueta = "Correo institucional:", valor = correo)
                    DatoItem(etiqueta = "Rol asignado:", valor = rol)
                    DatoItem(etiqueta = "ID / Código:", valor = usuario?.codigo?.ifBlank { usuario.id.take(8) } ?: "MON-2026")
                    DatoItem(etiqueta = "Estado:", valor = "Activo en turno deportivo")
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogoPerfil = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cerrar", color = Color(0xFF00390A), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Diálogo 2: Reglamento y Políticas
    if (mostrarDialogoReglamento) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoReglamento = false },
            icon = {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(text = "Reglamento de Préstamos", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Es requisito indispensable presentar carnet estudiantil o cédula vigente.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    Text("2. El tiempo máximo de préstamo por sesión es de 2 horas continuas.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    Text("3. El solicitante se compromete a devolver los implementos en el mismo estado en que los recibió.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    Text("4. El monitor revisará el estado del implemento antes de devolver el carnet retenido.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogoReglamento = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Entendido", color = Color(0xFF00390A), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Diálogo 5: Acerca de la Aplicación
    if (mostrarDialogoAcercaDe) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAcercaDe = false },
            icon = {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(text = "Acerca de la App", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Préstamos Deportivos Udenar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Versión 1.0.0 (Build 2026)", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("Sistema digital para la gestión ágil y control de inventario de implementos deportivos.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Diseñado para optimizar el registro de solicitantes, sustituir el formato de papel y agilizar las devoluciones.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogoAcercaDe = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cerrar", color = Color(0xFF00390A), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Diálogo 6: Confirmación de Cerrar Sesión con Funcionalidad Real
    if (mostrarConfirmacionCerrarSesion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionCerrarSesion = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFFFF8A80),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(text = "¿Cerrar Sesión?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    text = "Se cerrará la sesión actual de $nombre. ¿Deseas salir y volver a la pantalla de ingreso?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { mostrarConfirmacionCerrarSesion = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Cancelar")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacionCerrarSesion = false
                        onCerrarSesion()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.testTag("confirmar_cerrar_sesion_button")
                ) {
                    Text("Sí, Cerrar Sesión", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun DrawerOpcionItem(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1B281C),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DatoItem(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = etiqueta, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(text = valor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
fun HomeMenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF182619),
            contentColor = Color.White
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MyApplicationTheme(darkTheme = true) {
        HomeScreen(usuario = Usuario(nombre = "Carlos Morales", correo = "carlos@udenar.edu.co"))
    }
}
