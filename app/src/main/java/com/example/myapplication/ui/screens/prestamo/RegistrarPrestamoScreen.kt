package com.example.myapplication.ui.screens.prestamo

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.InventarioRepository
import com.example.myapplication.data.ItemDeportivo
import com.example.myapplication.data.Prestamo
import com.example.myapplication.data.PrestamosRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarPrestamoScreen(
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val inventarioRepository = remember { InventarioRepository() }
    val prestamosRepository = remember { PrestamosRepository() }

    val itemsInventario by inventarioRepository.obtenerInventarioFlow().collectAsState(initial = emptyList())
    val prestamosHistoricos by prestamosRepository.obtenerPrestamosFlow().collectAsState(initial = emptyList())

    var guardandoPrestamo by remember { mutableStateOf(false) }

    // 1. Estados de Datos del Solicitante con validaciones
    var codigoEstudiante by remember { mutableStateOf("") }
    var nombreCompleto by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    val codigoValido = codigoEstudiante.trim().length >= 3
    val nombreValido = nombreCompleto.trim().length >= 3
    val telefonoValido = telefono.isBlank() || (telefono.length in 7..10)

    // Validación de préstamo activo con carnet retenido
    val prestamoActivo = remember(codigoEstudiante, prestamosHistoricos) {
        if (codigoEstudiante.isBlank()) null
        else {
            prestamosRepository.buscarPrestamoActivoPorEstudiante(codigo = codigoEstudiante)
        }
    }
    val tieneCarnetRetenido = prestamoActivo != null

    // 2. Estados de Selección de Artículos (Múltiples elementos posibles)
    val articulosSeleccionados = remember { mutableStateListOf<ItemDeportivo>() }
    var categoriaFiltro by remember { mutableStateOf("Todos") }
    var busquedaItem by remember { mutableStateOf("") }

    val categoriasDisponibles = remember {
        listOf("Todos", "Balones", "Raquetas", "Petos", "Conos", "Ajedrez", "Otros")
    }

    // Filtrar implementos DISPONIBLES para prestar (que no estén ya seleccionados ni dañados/prestados)
    val itemsDisponibles = remember(itemsInventario, categoriaFiltro, busquedaItem, articulosSeleccionados.size) {
        itemsInventario.filter { item ->
            val estaDisponible = item.estado.equals("DISPONIBLE", ignoreCase = true) && item.cantidadDisponible > 0
            val noEstaEnCanasta = articulosSeleccionados.none { it.id.equals(item.id, ignoreCase = true) }
            val cumpleCategoria = if (categoriaFiltro == "Todos") true else item.categoria.equals(categoriaFiltro, ignoreCase = true)
            val cumpleBusqueda = if (busquedaItem.isBlank()) true else {
                item.id.contains(busquedaItem.trim(), ignoreCase = true) ||
                item.nombre.contains(busquedaItem.trim(), ignoreCase = true)
            }
            estaDisponible && noEstaEnCanasta && cumpleCategoria && cumpleBusqueda
        }
    }

    // 3. Estados de Detalles del Préstamo
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale.getDefault()) }
    var fechaHoraRetiro by remember { mutableStateOf(formatoFecha.format(Date())) }
    var mostrarDialogoFecha by remember { mutableStateOf(false) }
    var carnetRetenido by remember { mutableStateOf(true) }

    // 4. Estados de Validación y Firma
    var compromisoAceptado by remember { mutableStateOf(true) }
    val trazosFirma = remember { mutableStateListOf<List<Offset>>() }
    var trazoActual by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var mostrarConfirmacionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Registrar Préstamo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Gestión con retención de carnet estudiantil",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA5D6A7)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver,
                        modifier = Modifier.testTag("volver_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al menú principal",
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==========================================
            // SECCIÓN 1: DATOS DEL SOLICITANTE
            // ==========================================
            SeccionCard(
                titulo = "Datos del Solicitante / Responsable",
                subtitulo = "Información del estudiante que recibe los implementos",
                icono = Icons.Default.Person
            ) {
                OutlinedTextField(
                    value = codigoEstudiante,
                    onValueChange = { input ->
                        val filtrado = input.filter { it.isLetterOrDigit() || it == '-' }
                        if (filtrado.length <= 15) {
                            codigoEstudiante = filtrado
                        }
                    },
                    label = { Text("Código de estudiante / Identificación *") },
                    placeholder = { Text("Ej: 221015042 o Cédula") },
                    singleLine = true,
                    isError = codigoEstudiante.isNotBlank() && !codigoValido,
                    supportingText = {
                        if (codigoEstudiante.isNotBlank() && !codigoValido) {
                            Text("Mínimo 3 caracteres alfanuméricos", color = Color(0xFFFF8A80), fontSize = 11.sp)
                        } else {
                            Text("Identificación única del estudiante", color = Color.Gray, fontSize = 11.sp)
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("codigo_estudiante_input")
                )

                // ALERTA VISUAL DE CARNET RETENIDO
                if (tieneCarnetRetenido && prestamoActivo != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF381515)),
                        border = BorderStroke(1.5.dp, Color(0xFFFF5252)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("alerta_carnet_retenido")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alerta carnet retenido",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(32.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "PRÉSTAMO DENEGADO: CARNET RETENIDO",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFF8A80),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "El estudiante $codigoEstudiante ya tiene un préstamo activo en curso (#${prestamoActivo.id}) y su carnet está en custodia.",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Implementos pendientes por devolver: ${prestamoActivo.itemsNombres.joinToString().ifBlank { prestamoActivo.idItem }}",
                                    color = Color(0xFFFFCDD2),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Regla de seguridad: No se puede prestar ningún otro elemento sin presentar carnet. Primero debe registrarse la devolución del préstamo anterior.",
                                    color = Color(0xFFFFAB91),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nombreCompleto,
                    onValueChange = { input ->
                        val filtrado = input.filter { it.isLetter() || it.isWhitespace() || it == '.' }
                        if (filtrado.length <= 50) {
                            nombreCompleto = filtrado
                        }
                    },
                    label = { Text("Nombre completo del estudiante *") },
                    placeholder = { Text("Ej: Juan Pérez Martínez") },
                    singleLine = true,
                    isError = nombreCompleto.isNotBlank() && !nombreValido,
                    supportingText = {
                        if (nombreCompleto.isNotBlank() && !nombreValido) {
                            Text("Ingresa al menos 3 letras (solo texto)", color = Color(0xFFFF8A80), fontSize = 11.sp)
                        } else {
                            Text("Solo letras y espacios", color = Color.Gray, fontSize = 11.sp)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nombre_estudiante_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { input ->
                        val soloDigitos = input.filter { it.isDigit() }
                        if (soloDigitos.length <= 10) {
                            telefono = soloDigitos
                        }
                    },
                    label = { Text("Teléfono / Celular de contacto (Opcional)") },
                    placeholder = { Text("Ej: 3123456789") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    isError = telefono.isNotBlank() && !telefonoValido,
                    supportingText = {
                        if (telefono.isNotBlank() && !telefonoValido) {
                            Text("Debe contener entre 7 y 10 dígitos", color = Color(0xFFFF8A80), fontSize = 11.sp)
                        } else {
                            Text("7 a 10 dígitos numéricos", color = Color.Gray, fontSize = 11.sp)
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("telefono_estudiante_input")
                )
            }

            // ==========================================
            // SECCIÓN 2: SELECCIÓN DE ARTÍCULOS (MÚLTIPLES)
            // ==========================================
            SeccionCard(
                titulo = "Artículos a Prestar (${articulosSeleccionados.size} seleccionados)",
                subtitulo = "Un estudiante puede solicitar uno o varios implementos individuales",
                icono = Icons.Default.CheckCircle
            ) {
                // 1. Canasta de artículos seleccionados
                if (articulosSeleccionados.isNotEmpty()) {
                    Text(
                        text = "Implementos asignados a este préstamo:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        articulosSeleccionados.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E2F20),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(item.icono, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                ) {
                                                    Text(
                                                        text = item.id,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = item.nombre,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "Categoría: ${item.categoria}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFA5D6A7)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { articulosSeleccionados.remove(item) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Quitar implemento",
                                            tint = Color(0xFFFF8A80),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF19251B),
                        border = BorderStroke(1.dp, Color(0xFF2C3E2E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📦", fontSize = 20.sp)
                            Text(
                                text = "Aún no has agregado artículos. Elige los implementos disponibles abajo para agregarlos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 2. Buscador y Filtro de Categoría para agregar implementos
                Text(
                    text = "Seleccionar implementos disponibles del inventario:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = busquedaItem,
                    onValueChange = { busquedaItem = it },
                    placeholder = { Text("Buscar por código (ej. BA-001) o nombre...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (busquedaItem.isNotBlank()) {
                            IconButton(onClick = { busquedaItem = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = outlinedFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Selector de categoría horizontal
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoriasDisponibles) { cat ->
                        val esSeleccionada = cat.equals(categoriaFiltro, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (esSeleccionada) MaterialTheme.colorScheme.primary else Color(0xFF1F2B20),
                            border = BorderStroke(1.dp, if (esSeleccionada) MaterialTheme.colorScheme.primary else Color(0xFF314332)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { categoriaFiltro = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (esSeleccionada) Color(0xFF00390A) else Color.White,
                                fontWeight = if (esSeleccionada) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Lista horizontal/vertical compacta de implementos disponibles
                if (itemsDisponibles.isEmpty()) {
                    Text(
                        text = "No hay más implementos disponibles en este filtro.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(itemsDisponibles) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF18261A),
                                border = BorderStroke(1.dp, Color(0xFF2C3E2E)),
                                modifier = Modifier
                                    .widthIn(min = 160.dp, max = 200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        articulosSeleccionados.add(item)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = item.id,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(item.icono, fontSize = 18.sp)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = item.nombre,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = item.categoria,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = { articulosSeleccionados.add(item) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.fillMaxWidth().height(30.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253B28))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Agregar", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECCIÓN 3: DETALLES DEL PRÉSTAMO
            // ==========================================
            SeccionCard(
                titulo = "Detalles del Préstamo",
                subtitulo = "Horario y custodia de documentos",
                icono = Icons.Default.DateRange
            ) {
                // Fecha y Hora de Retiro
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B261D), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Fecha y Hora de Retiro",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = fechaHoraRetiro,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { fechaHoraRetiro = formatoFecha.format(Date()) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualizar a la hora actual",
                                tint = Color(0xFFA5D6A7)
                            )
                        }
                        Button(
                            onClick = { mostrarDialogoFecha = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E4030))
                        ) {
                            Text("Ajustar", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Retención de Carnet / Documento Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B261D), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Retención de Carnet Físico",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = if (carnetRetenido) "Carnet retenido en custodia durante el préstamo (Sí)" else "Sin carnet (No recomendado)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (carnetRetenido) MaterialTheme.colorScheme.primary else Color(0xFFFF8A80)
                        )
                    }

                    Switch(
                        checked = carnetRetenido,
                        onCheckedChange = { carnetRetenido = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color(0xFF2C392D)
                        ),
                        modifier = Modifier.testTag("carnet_retenido_switch")
                    )
                }
            }

            // ==========================================
            // SECCIÓN 4: VALIDACIÓN Y CONFIRMACIÓN
            // ==========================================
            SeccionCard(
                titulo = "Validación y Firma",
                subtitulo = "Compromiso de cuidado y firma digital",
                icono = Icons.Default.Check
            ) {
                // Nota legal con Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { compromisoAceptado = !compromisoAceptado }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = compromisoAceptado,
                        onCheckedChange = { compromisoAceptado = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = Color.Black,
                            uncheckedColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("compromiso_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Acepto el compromiso de devolución y cuidado de los implementos deportivos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Recuadro interactivo para dibujo de Firma Digital
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Firma Digital (Trazar con el dedo):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFA5D6A7)
                    )

                    if (trazosFirma.isNotEmpty() || trazoActual.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                trazosFirma.clear()
                                trazoActual = emptyList()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Limpiar firma", color = Color(0xFFFF8A80), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Color(0xFF141D15), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF2E4030), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    trazoActual = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    trazoActual = trazoActual + change.position
                                },
                                onDragEnd = {
                                    if (trazoActual.isNotEmpty()) {
                                        trazosFirma.add(trazoActual)
                                        trazoActual = emptyList()
                                    }
                                },
                                onDragCancel = {
                                    trazoActual = emptyList()
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (trazo in trazosFirma) {
                            if (trazo.size > 1) {
                                val path = Path().apply {
                                    moveTo(trazo.first().x, trazo.first().y)
                                    for (i in 1 until trazo.size) {
                                        lineTo(trazo[i].x, trazo[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFF8BC34A),
                                    style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                        if (trazoActual.size > 1) {
                            val path = Path().apply {
                                moveTo(trazoActual.first().x, trazoActual.first().y)
                                for (i in 1 until trazoActual.size) {
                                    lineTo(trazoActual[i].x, trazoActual[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF8BC34A),
                                style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }

                    if (trazosFirma.isEmpty() && trazoActual.isEmpty()) {
                        Text(
                            text = "✍️ Dibuja la firma del estudiante aquí",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5A725D),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // BOTÓN DE ACCIÓN PRINCIPAL
            // ==========================================
            val puedeRegistrar = !tieneCarnetRetenido && 
                articulosSeleccionados.isNotEmpty() && 
                codigoValido && 
                nombreValido && 
                telefonoValido && 
                compromisoAceptado

            Button(
                onClick = {
                    if (tieneCarnetRetenido) {
                        Toast.makeText(context, "Préstamo denegado: El estudiante ya tiene un préstamo activo con carnet retenido.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (articulosSeleccionados.isEmpty()) {
                        Toast.makeText(context, "Por favor agrega al menos un artículo deportivo al préstamo.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!codigoValido || !nombreValido) {
                        Toast.makeText(context, "Verifica los datos del estudiante antes de continuar.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!telefonoValido) {
                        Toast.makeText(context, "El número de teléfono debe tener entre 7 y 10 dígitos.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    mostrarConfirmacionDialog = true
                },
                enabled = puedeRegistrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("boton_registrar_prestamo"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (puedeRegistrar) MaterialTheme.colorScheme.primary else Color(0xFF2C392D),
                    disabledContainerColor = Color(0xFF253126),
                    disabledContentColor = Color.Gray
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = if (tieneCarnetRetenido) Icons.Default.Warning else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (puedeRegistrar) Color(0xFF00390A) else Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        tieneCarnetRetenido -> "Bloqueado: Carnet Retenido"
                        !codigoValido -> "Ingresa código de estudiante"
                        !nombreValido -> "Ingresa nombre del estudiante"
                        !telefonoValido -> "Teléfono inválido (7-10 dígitos)"
                        articulosSeleccionados.isEmpty() -> "Selecciona al menos 1 artículo"
                        !compromisoAceptado -> "Acepta el compromiso de devolución"
                        else -> "Registrar Préstamo (${articulosSeleccionados.size} implementos)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (puedeRegistrar) Color(0xFF00390A) else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Cuadro de diálogo para ajustar horario
    if (mostrarDialogoFecha) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoFecha = false },
            title = {
                Text(text = "Ajustar Fecha y Hora", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Puedes seleccionar una opción rápida de horario para el préstamo:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = {
                            fechaHoraRetiro = formatoFecha.format(Date())
                            mostrarDialogoFecha = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hora actual del sistema")
                    }
                    OutlinedButton(
                        onClick = {
                            fechaHoraRetiro = "Hoy a las 08:00 AM"
                            mostrarDialogoFecha = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Jornada Mañana (08:00 AM)")
                    }
                    OutlinedButton(
                        onClick = {
                            fechaHoraRetiro = "Hoy a las 02:00 PM"
                            mostrarDialogoFecha = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Jornada Tarde (02:00 PM)")
                    }
                    OutlinedButton(
                        onClick = {
                            fechaHoraRetiro = "Hoy a las 06:00 PM"
                            mostrarDialogoFecha = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Jornada Noche (06:00 PM)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoFecha = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo de confirmación del Préstamo
    if (mostrarConfirmacionDialog) {
        AlertDialog(
            onDismissRequest = { if (!guardandoPrestamo) mostrarConfirmacionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Confirmar Préstamo Deportivo",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Resumen del registro:",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA5D6A7)
                    )
                    Text(
                        text = "• Solicitante: $nombreCompleto ($codigoEstudiante)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    if (telefono.isNotBlank()) {
                        Text(
                            text = "• Teléfono: $telefono",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                    Text(
                        text = "• Artículos a entregar (${articulosSeleccionados.size}):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    articulosSeleccionados.forEach { item ->
                        Text(
                            text = "   - [${item.id}] ${item.nombre}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA5D6A7)
                        )
                    }
                    Text(
                        text = "• Carnet en custodia: ${if (carnetRetenido) "Sí (Retenido en monitoreo)" else "No"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Text(
                        text = "• Fecha/Hora: $fechaHoraRetiro",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Se actualizará el estado de cada implemento individual a 'PRESTADO' y se registrará la retención del carnet.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA5D6A7)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            guardandoPrestamo = true
                            try {
                                val ahora = Timestamp.now()
                                val nuevoPrestamo = Prestamo(
                                    codigoEstudiante = codigoEstudiante.trim(),
                                    nombreEstudiante = nombreCompleto.trim(),
                                    telefono = telefono.trim(),
                                    itemsCodigos = articulosSeleccionados.map { it.id },
                                    itemsNombres = articulosSeleccionados.map { "${it.id} - ${it.nombre}" },
                                    cantidadTotalItems = articulosSeleccionados.size,
                                    carnetRetenido = carnetRetenido,
                                    documentoRetenido = carnetRetenido,
                                    fechaPrestamo = ahora,
                                    estadoDevolucion = "PENDIENTE",
                                    observaciones = "Retiro: $fechaHoraRetiro"
                                )

                                val resultado = prestamosRepository.registrarPrestamo(
                                    prestamo = nuevoPrestamo,
                                    itemsPrestados = articulosSeleccionados.toList()
                                )

                                resultado.onSuccess { mensaje ->
                                    Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                                    mostrarConfirmacionDialog = false
                                    onVolver()
                                }.onFailure { error ->
                                    Toast.makeText(context, error.localizedMessage ?: "Error al registrar préstamo", Toast.LENGTH_LONG).show()
                                    mostrarConfirmacionDialog = false
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                mostrarConfirmacionDialog = false
                            } finally {
                                guardandoPrestamo = false
                            }
                        }
                    },
                    enabled = !guardandoPrestamo,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (guardandoPrestamo) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00390A), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardando...", color = Color(0xFF00390A), fontWeight = FontWeight.Bold)
                    } else {
                        Text("Confirmar y Guardar", color = Color(0xFF00390A), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { mostrarConfirmacionDialog = false },
                    enabled = !guardandoPrestamo
                ) {
                    Text("Cancelar", color = Color.LightGray)
                }
            }
        )
    }
}

@Composable
fun SeccionCard(
    titulo: String,
    subtitulo: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF162118)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
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
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0B3A2)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color(0xFF2E3D2F),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = Color.LightGray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF1B261D),
    unfocusedContainerColor = Color(0xFF1B261D),
    cursorColor = MaterialTheme.colorScheme.primary
)
