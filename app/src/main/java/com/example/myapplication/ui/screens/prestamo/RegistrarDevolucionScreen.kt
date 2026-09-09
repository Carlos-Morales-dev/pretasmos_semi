package com.example.myapplication.ui.screens.prestamo

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Prestamo
import com.example.myapplication.data.PrestamosRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarDevolucionScreen(
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prestamosRepository = remember { PrestamosRepository() }

    val todosLosPrestamos by prestamosRepository.obtenerPrestamosFlow().collectAsState(initial = emptyList())

    var filtroTexto by remember { mutableStateOf("") }
    var verSoloPendientes by remember { mutableStateOf(true) }
    var prestamoAConfirmar by remember { mutableStateOf<Prestamo?>(null) }
    var procesandoDevolucion by remember { mutableStateOf(false) }

    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale.getDefault()) }

    // Filtrar préstamos
    val prestamosFiltrados = remember(todosLosPrestamos, filtroTexto, verSoloPendientes) {
        todosLosPrestamos.filter { prestamo ->
            val coincideEstado = if (verSoloPendientes) prestamo.estadoDevolucion == "PENDIENTE" else true
            val coincideTexto = if (filtroTexto.isBlank()) true else {
                prestamo.codigoEstudiante.contains(filtroTexto.trim(), ignoreCase = true) ||
                prestamo.nombreEstudiante.contains(filtroTexto.trim(), ignoreCase = true) ||
                prestamo.id.contains(filtroTexto.trim(), ignoreCase = true) ||
                prestamo.itemsCodigos.any { it.contains(filtroTexto.trim(), ignoreCase = true) }
            }
            coincideEstado && coincideTexto
        }
    }

    val totalActivos = remember(todosLosPrestamos) {
        todosLosPrestamos.count { it.estadoDevolucion == "PENDIENTE" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Registrar Devolución",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$totalActivos préstamos activos con carnet en custodia",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA5D6A7)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver,
                        modifier = Modifier.testTag("volver_menu_devolucion_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al menú principal",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF142416))
            )
        },
        containerColor = Color(0xFF0F170F)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Buscador por estudiante / código de implemento
            OutlinedTextField(
                value = filtroTexto,
                onValueChange = { filtroTexto = it },
                label = { Text("Buscar por estudiante, código o carnet...") },
                placeholder = { Text("Ej: 221015042, Juan Pérez, BA-001") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (filtroTexto.isNotBlank()) {
                        IconButton(onClick = { filtroTexto = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF2E3D2F),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1B261D),
                    unfocusedContainerColor = Color(0xFF1B261D),
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("filtro_devolucion_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Selector de pestaña rápida: Pendientes vs Todos los préstamos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = verSoloPendientes,
                    onClick = { verSoloPendientes = true },
                    label = { Text("⚠️ Activos con Carnet Retenido ($totalActivos)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2E4030),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = Color(0xFF162118),
                        labelColor = Color.LightGray
                    )
                )
                FilterChip(
                    selected = !verSoloPendientes,
                    onClick = { verSoloPendientes = false },
                    label = { Text("📋 Historial Completo") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2E4030),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = Color(0xFF162118),
                        labelColor = Color.LightGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de préstamos
            if (prestamosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (verSoloPendientes) "No hay préstamos activos pendientes" else "No se encontraron préstamos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (verSoloPendientes) "Todos los implementos están devueltos y ningún carnet está retenido." else "Intenta con otro término de búsqueda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(prestamosFiltrados, key = { it.id }) { prestamo ->
                        PrestamoDevolucionCard(
                            prestamo = prestamo,
                            formatoFecha = formatoFecha,
                            onDevolverClick = { prestamoAConfirmar = prestamo }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de Confirmación de Devolución
    if (prestamoAConfirmar != null) {
        val prestamo = prestamoAConfirmar!!
        AlertDialog(
            onDismissRequest = { if (!procesandoDevolucion) prestamoAConfirmar = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Confirmar Devolución de Implementos", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Deseas registrar la devolución de los implementos de ${prestamo.nombreEstudiante}?",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A291C)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Estudiante: ${prestamo.nombreEstudiante} (${prestamo.codigoEstudiante})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA5D6A7)
                            )
                            Text(
                                text = "Artículos a reintegrar al inventario (${prestamo.itemsCodigos.size}):",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                            prestamo.itemsNombres.ifEmpty { listOf(prestamo.idItem) }.forEach { itemNombre ->
                                Text(
                                    text = " • $itemNombre",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                    Text(
                        text = "🪪 Recuerda entregar el carnet físico al estudiante al recibir todos los implementos en buen estado.",
                        fontSize = 12.sp,
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            procesandoDevolucion = true
                            try {
                                val resultado = prestamosRepository.registrarDevolucion(prestamo.id)
                                resultado.onSuccess { mensaje ->
                                    Toast.makeText(context, "$mensaje Carnet liberado.", Toast.LENGTH_SHORT).show()
                                    prestamoAConfirmar = null
                                }.onFailure { error ->
                                    Toast.makeText(context, "Error: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                procesandoDevolucion = false
                            }
                        }
                    },
                    enabled = !procesandoDevolucion,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (procesandoDevolucion) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00390A), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Devolver y Liberar Carnet", color = Color(0xFF00390A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { prestamoAConfirmar = null },
                    enabled = !procesandoDevolucion
                ) {
                    Text("Cancelar", color = Color.LightGray)
                }
            }
        )
    }
}

@Composable
fun PrestamoDevolucionCard(
    prestamo: Prestamo,
    formatoFecha: SimpleDateFormat,
    onDevolverClick: () -> Unit
) {
    val esActivo = prestamo.estadoDevolucion == "PENDIENTE"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162118)),
        border = BorderStroke(1.dp, if (esActivo) Color(0xFF2E4030) else Color(0xFF1E281F)),
        modifier = Modifier.fillMaxWidth().testTag("prestamo_card_${prestamo.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera: ID Préstamo y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "#${prestamo.id.takeLast(6)}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5D6A7),
                        fontSize = 13.sp
                    )
                    if (prestamo.carnetRetenido && esActivo) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF421C1C),
                            border = BorderStroke(1.dp, Color(0xFFFF5252))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🪪", fontSize = 11.sp)
                                Text(
                                    text = "Carnet Retenido",
                                    color = Color(0xFFFF8A80),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (esActivo) Color(0xFF332A15) else Color(0xFF1B331E)
                ) {
                    Text(
                        text = if (esActivo) "PENDIENTE" else "DEVUELTO",
                        color = if (esActivo) Color(0xFFFFD54F) else Color(0xFFA5D6A7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Datos del estudiante
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Column {
                    Text(
                        text = prestamo.nombreEstudiante,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Código: ${prestamo.codigoEstudiante} • Tel: ${prestamo.telefono.ifBlank { "N/A" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Divider(color = Color(0xFF223124), thickness = 1.dp)

            // Implementos prestados
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Implementos prestados (${prestamo.itemsCodigos.size}):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA5D6A7)
                )
                val listaNombres = if (prestamo.itemsNombres.isNotEmpty()) prestamo.itemsNombres else listOf(prestamo.idItem)
                listaNombres.forEach { nombre ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("•", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        Text(
                            text = nombre,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Fecha de retiro
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val fechaTexto = prestamo.fechaPrestamo?.toDate()?.let { formatoFecha.format(it) } ?: "Reciente"
                Text(
                    text = "Retiro: $fechaTexto",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            // Botón de devolución si está activo
            if (esActivo) {
                Button(
                    onClick = onDevolverClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("boton_devolver_${prestamo.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF00390A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Registrar Devolución y Liberar Carnet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF00390A)
                    )
                }
            }
        }
    }
}
