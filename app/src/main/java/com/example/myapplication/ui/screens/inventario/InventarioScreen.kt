package com.example.myapplication.ui.screens.inventario

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.InventarioRepository
import com.example.myapplication.data.ItemDeportivo
import com.example.myapplication.viewmodel.InventarioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    onVolver: () -> Unit,
    viewModel: InventarioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Estados para diálogos
    var itemAEditar by remember { mutableStateOf<ItemDeportivo?>(null) }
    var itemAEliminar by remember { mutableStateOf<ItemDeportivo?>(null) }
    var mostrarDialogoNuevo by remember { mutableStateOf(false) }
    var mostrarDialogoReglas by remember { mutableStateOf(false) }

    // Mostrar mensajes en Snackbar
    LaunchedEffect(uiState.mensajeExito, uiState.mensajeError) {
        uiState.mensajeExito?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
        uiState.mensajeError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
    }

    val categoriasFiltro = remember {
        listOf("Todas", "Balones", "Raquetas", "Petos", "Conos", "Ajedrez", "Otros")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Inventario Deportivo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Control de existencias y estado de implementos",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA5D6A7)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver,
                        modifier = Modifier.testTag("volver_inventario_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al inicio",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { mostrarDialogoReglas = true },
                        modifier = Modifier.testTag("boton_info_reglas_firebase")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Configuración Reglas Firestore",
                            tint = Color(0xFFA5D6A7)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF142416)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarDialogoNuevo = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00390A)) },
                text = { Text("Nuevo Elemento", fontWeight = FontWeight.Bold, color = Color(0xFF00390A)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color(0xFF00390A),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .testTag("agregar_item_fab")
                    .padding(bottom = 8.dp)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F170F)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ==========================================
            // RESUMEN RÁPIDO DE INVENTARIO
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResumenMiniCard(
                    titulo = "Tipos",
                    valor = "${uiState.totalArticulosDistintos}",
                    subtitulo = "Artículos",
                    colorTexto = Color.White,
                    modifier = Modifier.weight(1f)
                )
                ResumenMiniCard(
                    titulo = "Disponibles",
                    valor = "${uiState.totalUnidadesDisponibles}",
                    subtitulo = "En bodega",
                    colorTexto = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ResumenMiniCard(
                    titulo = "Dañados",
                    valor = "${uiState.totalUnidadesDanadas}",
                    subtitulo = "En revisión",
                    colorTexto = if (uiState.totalUnidadesDanadas > 0) Color(0xFFFF8A80) else Color.LightGray,
                    modifier = Modifier.weight(1f)
                )
            }

            // ==========================================
            // BARRA DE BÚSQUEDA (ID / NOMBRE)
            // ==========================================
            OutlinedTextField(
                value = uiState.busqueda,
                onValueChange = { viewModel.actualizarBusqueda(it) },
                placeholder = { Text("Buscar por código (ej: BA-001) o nombre...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (uiState.busqueda.isNotBlank()) {
                        IconButton(onClick = { viewModel.actualizarBusqueda("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda", tint = Color.LightGray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF2E3D2F),
                    focusedContainerColor = Color(0xFF162118),
                    unfocusedContainerColor = Color(0xFF162118),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("busqueda_inventario_input")
            )

            // ==========================================
            // FILTRO DE CATEGORÍAS (CHIPS SCROLLABLES)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoriasFiltro.forEach { cat ->
                    val esSeleccionado = uiState.categoriaFiltro.equals(cat, ignoreCase = true)
                    val iconoEmoji = when (cat) {
                        "Balones" -> "⚽ "
                        "Raquetas" -> "🎾 "
                        "Petos" -> "🎽 "
                        "Conos" -> "🔶 "
                        "Ajedrez" -> "♟️ "
                        "Otros" -> "🏅 "
                        else -> ""
                    }
                    FilterChip(
                        selected = esSeleccionado,
                        onClick = { viewModel.seleccionarCategoriaFiltro(cat) },
                        label = {
                            Text(
                                text = "$iconoEmoji$cat",
                                fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color(0xFF00390A),
                            containerColor = Color(0xFF1A261B),
                            labelColor = Color(0xFFC4D5C5)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = esSeleccionado,
                            borderColor = Color(0xFF2E3D2F),
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // ==========================================
            // LISTADO DE ARTÍCULOS O ESTADO VACÍO
            // ==========================================
            if (uiState.cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.itemsFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📦",
                            fontSize = 54.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No se encontraron elementos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.busqueda.isNotBlank() || uiState.categoriaFiltro != "Todas") {
                                "Prueba ajustando el término de búsqueda o seleccionando otra categoría."
                            } else {
                                "El inventario está vacío. Usa el botón inferior para registrar el primer implemento."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        if (uiState.busqueda.isNotBlank() || uiState.categoriaFiltro != "Todas") {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.actualizarBusqueda("")
                                    viewModel.seleccionarCategoriaFiltro("Todas")
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Restablecer filtros")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(
                        items = uiState.itemsFiltrados,
                        key = { it.id }
                    ) { item ->
                        ItemInventarioCard(
                            item = item,
                            onEditar = { itemAEditar = item },
                            onEliminar = { itemAEliminar = item }
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // DIÁLOGO PARA CREAR NUEVO ARTÍCULO (O LOTE)
    // ==========================================
    if (mostrarDialogoNuevo) {
        FormularioItemDialog(
            itemInicial = null,
            titulo = "Registrar Nuevo Elemento",
            onSugerirId = { cat, nom -> viewModel.sugerirSiguienteId(cat, nom) },
            onSugerirSecuencia = { cat, nom, cant -> viewModel.sugerirSecuenciaIds(cat, nom, cant) },
            onDismiss = { mostrarDialogoNuevo = false },
            onGuardarItem = { nuevoItem ->
                viewModel.guardarItem(nuevoItem) { exitoso ->
                    if (exitoso) {
                        mostrarDialogoNuevo = false
                    }
                }
            },
            onGuardarLote = { listaItems ->
                viewModel.guardarVariosItems(listaItems) { exitoso ->
                    if (exitoso) {
                        mostrarDialogoNuevo = false
                    }
                }
            }
        )
    }

    // ==========================================
    // DIÁLOGO PARA EDITAR ARTÍCULO INDIVIDUAL
    // ==========================================
    itemAEditar?.let { item ->
        FormularioItemDialog(
            itemInicial = item,
            titulo = "Editar Elemento Individual (${item.id})",
            onSugerirId = { cat, nom -> viewModel.sugerirSiguienteId(cat, nom) },
            onSugerirSecuencia = { cat, nom, cant -> viewModel.sugerirSecuenciaIds(cat, nom, cant) },
            onDismiss = { itemAEditar = null },
            onGuardarItem = { itemActualizado ->
                viewModel.guardarItem(itemActualizado) { exitoso ->
                    if (exitoso) {
                        itemAEditar = null
                    }
                }
            },
            onGuardarLote = { /* Solo edición individual */ }
        )
    }

    // ==========================================
    // DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN
    // ==========================================
    itemAEliminar?.let { item ->
        AlertDialog(
            onDismissRequest = { itemAEliminar = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFFF8A80),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "¿Eliminar elemento?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Estás a punto de eliminar el implemento del inventario:",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${item.icono} ${item.nombre} [${item.id}]",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Esta acción eliminará el registro de la base de datos de Firebase.",
                        color = Color(0xFFFFB4AB),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idBorrar = item.id
                        itemAEliminar = null
                        viewModel.eliminarItem(idBorrar) { }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemAEliminar = null }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E261F),
            shape = RoundedCornerShape(18.dp)
        )
    }

    // ==========================================
    // DIÁLOGO DE CONFIGURACIÓN REGLAS FIREBASE
    // ==========================================
    if (mostrarDialogoReglas) {
        val snippetReglas = """rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Colección de Usuarios
    match /usuarios/{userId} {
      allow read, write: if request.auth != null;
    }
    // Colección de Inventario Deportivo
    match /inventario/{document=**} {
      allow read, write: if request.auth != null;
    }
    // Colección de Préstamos
    match /prestamos/{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}"""

        AlertDialog(
            onDismissRequest = { mostrarDialogoReglas = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Reglas de Firebase Firestore",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "El error PERMISSION_DENIED ocurre cuando en Firebase Console la colección 'inventario' aún no tiene reglas de acceso permitidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Text(
                        text = "1. Abre Firebase Console > Firestore Database > pestaña 'Reglas'.\n2. Reemplaza o agrega estas reglas:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA5D6A7),
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0C140D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3D2F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = snippetReglas,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF81C784),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        text = "Nota: La aplicación ya cuenta con caché local inteligente para que puedas registrar, editar o eliminar elementos deportivos de inmediato sin que la app se bloquee.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(snippetReglas))
                        Toast.makeText(context, "¡Reglas copiadas al portapapeles!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color(0xFF00390A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar Reglas", color = Color(0xFF00390A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoReglas = false }) {
                    Text("Cerrar", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E261F),
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
fun ResumenMiniCard(
    titulo: String,
    valor: String,
    subtitulo: String,
    colorTexto: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF142216)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorTexto
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF819782)
            )
        }
    }
}

@Composable
fun ItemInventarioCard(
    item: ItemDeportivo,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val colorEstado = when (item.estado.uppercase()) {
        "DISPONIBLE" -> Color(0xFF4CAF50)
        "PRESTADO" -> Color(0xFF42A5F5)
        "DAÑADO" -> Color(0xFFFF5252)
        "EN REPARACIÓN", "REPARACION" -> Color(0xFFFFB74D)
        "AGOTADO" -> Color(0xFFB0BEC5)
        else -> MaterialTheme.colorScheme.primary
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF152216)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. ÍCONO REPRESENTATIVO
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFF1E2E20), CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.icono.ifBlank { InventarioRepository.iconoRepresentativo(item.categoria, item.nombre) },
                        fontSize = 26.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 2. INFORMACIÓN PRINCIPAL (ID INDIVIDUAL, NOMBRE, CATEGORÍA)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // ID_articulo ÚNICO
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = item.id,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }

                        // Estado actual
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorEstado.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colorEstado.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = item.estado.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorEstado,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Nombre del Producto
                    Text(
                        text = item.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Categoría
                    Text(
                        text = "Categoría: ${item.categoria}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA5D6A7)
                    )
                }

                // 3. ACCIONES ESPECÍFICAS DE ESTE ARTÍCULO (EDITAR / ELIMINAR ESTA UNIDAD)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEditar,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF223424), CircleShape)
                            .testTag("editar_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar implemento ${item.id}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onEliminar,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF331E1E), CircleShape)
                            .testTag("eliminar_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar implemento ${item.id}",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Banner si el artículo está prestado actualmente a un estudiante
            if (item.estado.uppercase() == "PRESTADO" && item.estudiantePrestamoNombre.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF102636),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF42A5F5).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("👤", fontSize = 13.sp)
                        Text(
                            text = "En préstamo a: ${item.estudiantePrestamoNombre} (${item.estudiantePrestamoId})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF90CAF9),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF233525), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // 4. DETALLES DE UNIDAD FÍSICA Y OBSERVACIONES
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E2D20)
                    ) {
                        Text(
                            text = "Unidad Física Individual",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA5D6A7),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = if (item.estado == "DISPONIBLE") "• Disponible para préstamo" else "• No disponible",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.estado == "DISPONIBLE") Color(0xFF4CAF50) else Color(0xFFFFB74D)
                    )
                }

                if (item.descripcion.isNotBlank()) {
                    Text(
                        text = item.descripcion,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioItemDialog(
    itemInicial: ItemDeportivo?,
    titulo: String,
    onSugerirId: (categoria: String, nombre: String) -> String,
    onSugerirSecuencia: (categoria: String, nombre: String, cantidad: Int) -> List<String>,
    onDismiss: () -> Unit,
    onGuardarItem: (ItemDeportivo) -> Unit,
    onGuardarLote: (List<ItemDeportivo>) -> Unit
) {
    val categoriasDisponibles = remember {
        listOf(
            "Balones" to "⚽",
            "Raquetas" to "🎾",
            "Petos" to "🎽",
            "Conos" to "🔶",
            "Ajedrez" to "♟️",
            "Otros" to "🏅"
        )
    }

    var categoriaSeleccionada by remember {
        mutableStateOf(itemInicial?.categoria ?: "Balones")
    }

    var nombre by remember {
        mutableStateOf(itemInicial?.nombre ?: "")
    }

    // Cantidad de unidades físicas a crear (solo al agregar nuevos)
    var cantidadUnidades by remember {
        mutableIntStateOf(1)
    }

    // Si es nuevo elemento, se autocalcula según categoría y nombre; si es edición, se mantiene el ID
    var idArticulo by remember {
        mutableStateOf(itemInicial?.id ?: onSugerirId(categoriaSeleccionada, nombre))
    }

    var icono by remember {
        mutableStateOf(itemInicial?.icono ?: InventarioRepository.iconoRepresentativo(categoriaSeleccionada, nombre))
    }

    val estados = listOf("DISPONIBLE", "PRESTADO", "DAÑADO", "EN REPARACIÓN")
    var estadoSeleccionado by remember {
        mutableStateOf(itemInicial?.estado ?: "DISPONIBLE")
    }

    var descripcion by remember {
        mutableStateOf(itemInicial?.descripcion ?: "")
    }

    var errorValidacion by remember { mutableStateOf<String?>(null) }

    val nombreValido = nombre.trim().length >= 3
    val idValido = idArticulo.trim().length >= 2

    // Previsualización de secuencia de códigos si se crean múltiples unidades
    val codigosSecuencia = remember(categoriaSeleccionada, nombre, cantidadUnidades, itemInicial) {
        if (itemInicial == null && cantidadUnidades > 1) {
            onSugerirSecuencia(categoriaSeleccionada, nombre, cantidadUnidades)
        } else {
            emptyList()
        }
    }

    // Actualiza sugerencia de ID e ícono automáticamente cuando cambia la categoría (solo si es nuevo)
    LaunchedEffect(categoriaSeleccionada) {
        if (itemInicial == null) {
            idArticulo = onSugerirId(categoriaSeleccionada, nombre)
            icono = InventarioRepository.iconoRepresentativo(categoriaSeleccionada, nombre)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = icono,
                    fontSize = 24.sp
                )
                Text(
                    text = titulo,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector de Categoría con Emojis
                Text(
                    text = "Categoría del implemento:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA5D6A7)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoriasDisponibles.forEach { (cat, emoji) ->
                        val esSeleccionada = cat.equals(categoriaSeleccionada, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (esSeleccionada) MaterialTheme.colorScheme.primary else Color(0xFF1E2D20),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    categoriaSeleccionada = cat
                                    icono = emoji
                                    if (itemInicial == null) {
                                        idArticulo = onSugerirId(cat, nombre)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(emoji, fontSize = 14.sp)
                                Text(
                                    text = cat,
                                    color = if (esSeleccionada) Color(0xFF00390A) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (esSeleccionada) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Nombre del Producto
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { input ->
                        if (input.length <= 60) {
                            nombre = input
                            if (itemInicial == null && idArticulo.isBlank()) {
                                idArticulo = onSugerirId(categoriaSeleccionada, input)
                            }
                        }
                    },
                    label = { Text("Nombre del Producto *") },
                    placeholder = { Text("Ej: Balón Golty Tradicional #5") },
                    singleLine = true,
                    isError = nombre.isNotBlank() && !nombreValido,
                    supportingText = {
                        if (nombre.isNotBlank() && !nombreValido) {
                            Text("Mínimo 3 caracteres", color = Color(0xFFFF8A80), fontSize = 11.sp)
                        } else {
                            Text("Máximo 60 caracteres", color = Color.Gray, fontSize = 11.sp)
                        }
                    },
                    colors = dialogTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_nombre_producto")
                )

                if (itemInicial == null) {
                    // Selector de Cantidad de unidades físicas individuales a ingresar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF142015), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Cantidad de artículos físicos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Cada uno tendrá su propio código independiente",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA5D6A7)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (cantidadUnidades > 1) cantidadUnidades-- },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF223524), CircleShape)
                                ) {
                                    Text("—", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "$cantidadUnidades",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.widthIn(min = 20.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                IconButton(
                                    onClick = { if (cantidadUnidades < 50) cantidadUnidades++ },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF223524), CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (cantidadUnidades > 1 && codigosSecuencia.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Códigos secuenciales que se generarán:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                codigosSecuencia.forEach { cod ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = cod,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ID del Artículo (Para unidad única o edición)
                if (itemInicial != null || cantidadUnidades == 1) {
                    OutlinedTextField(
                        value = idArticulo,
                        onValueChange = { input ->
                            val filtrado = input.uppercase().filter { it.isLetterOrDigit() || it == '-' || it == '_' }
                            if (filtrado.length <= 15) {
                                idArticulo = filtrado
                            }
                        },
                        label = { Text("Código de Inventario (ID único) *") },
                        placeholder = { Text("Ej: BA-001, RQ-002, PT-003") },
                        singleLine = true,
                        isError = idArticulo.isNotBlank() && !idValido,
                        supportingText = {
                            if (idArticulo.isNotBlank() && !idValido) {
                                Text("Mínimo 2 caracteres (letras, números y guiones)", color = Color(0xFFFF8A80), fontSize = 11.sp)
                            } else {
                                Text("Secuencia de categoría sugerida (ej. BA-XXX)", color = Color.Gray, fontSize = 11.sp)
                            }
                        },
                        trailingIcon = {
                            if (itemInicial == null) {
                                TextButton(
                                    onClick = {
                                        idArticulo = onSugerirId(categoriaSeleccionada, nombre)
                                    }
                                ) {
                                    Text("Auto", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                            }
                        },
                        colors = dialogTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_id_articulo")
                    )
                }

                // Selector de Estado (DISPONIBLE, PRESTADO, DAÑADO, EN REPARACIÓN)
                Text(
                    text = "Estado del artículo:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA5D6A7)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    estados.forEach { est ->
                        val esSeleccionado = est.equals(estadoSeleccionado, ignoreCase = true)
                        val colorEst = when (est) {
                            "DISPONIBLE" -> Color(0xFF4CAF50)
                            "PRESTADO" -> Color(0xFF42A5F5)
                            "DAÑADO" -> Color(0xFFFF5252)
                            "EN REPARACIÓN" -> Color(0xFFFFB74D)
                            else -> Color(0xFF9E9E9E)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (esSeleccionado) colorEst.copy(alpha = 0.25f) else Color(0xFF19251A),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (esSeleccionado) colorEst else Color(0xFF283A2A)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    estadoSeleccionado = est
                                }
                        ) {
                            Text(
                                text = est,
                                color = if (esSeleccionado) colorEst else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Descripción / Observaciones
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción / Observaciones (Opcional)") },
                    placeholder = { Text("Marca, estado físico o detalles de esta unidad") },
                    colors = dialogTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Mensaje de error de validación si existe
                errorValidacion?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFFF8A80),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            val formularioValido = nombreValido && (itemInicial != null || cantidadUnidades > 1 || idValido)
            Button(
                onClick = {
                    if (!nombreValido) {
                        errorValidacion = "El nombre del producto debe tener al menos 3 caracteres."
                        return@Button
                    }

                    if ((itemInicial != null || cantidadUnidades == 1) && !idValido) {
                        errorValidacion = "El código de inventario debe tener al menos 2 caracteres."
                        return@Button
                    }

                    errorValidacion = null
                    val emojiIcono = icono.ifBlank {
                        InventarioRepository.iconoRepresentativo(categoriaSeleccionada, nombre)
                    }

                    if (itemInicial != null) {
                        // Edición de un artículo específico existente
                        val itemActualizado = itemInicial.copy(
                            id = idArticulo.trim().uppercase(),
                            nombre = nombre.trim(),
                            categoria = categoriaSeleccionada,
                            icono = emojiIcono,
                            estado = estadoSeleccionado,
                            cantidadTotal = 1,
                            cantidadDisponible = if (estadoSeleccionado == "DISPONIBLE") 1 else 0,
                            descripcion = descripcion.trim()
                        )
                        onGuardarItem(itemActualizado)
                    } else {
                        // Creación de artículo nuevo (individual o en lote secuencial)
                        if (cantidadUnidades == 1) {
                            if (idArticulo.isBlank()) {
                                errorValidacion = "El ID del artículo es obligatorio."
                                return@Button
                            }
                            val nuevo = ItemDeportivo(
                                id = idArticulo.trim().uppercase(),
                                nombre = nombre.trim(),
                                categoria = categoriaSeleccionada,
                                icono = emojiIcono,
                                cantidadTotal = 1,
                                cantidadDisponible = if (estadoSeleccionado == "DISPONIBLE") 1 else 0,
                                estado = estadoSeleccionado,
                                descripcion = descripcion.trim()
                            )
                            onGuardarItem(nuevo)
                        } else {
                            val codigos = onSugerirSecuencia(categoriaSeleccionada, nombre, cantidadUnidades)
                            val lote = codigos.mapIndexed { index, cod ->
                                ItemDeportivo(
                                    id = cod,
                                    nombre = "${nombre.trim()} (Unidad ${index + 1})",
                                    categoria = categoriaSeleccionada,
                                    icono = emojiIcono,
                                    cantidadTotal = 1,
                                    cantidadDisponible = if (estadoSeleccionado == "DISPONIBLE") 1 else 0,
                                    estado = estadoSeleccionado,
                                    descripcion = descripcion.trim()
                                )
                            }
                            onGuardarLote(lote)
                        }
                    }
                },
                enabled = formularioValido,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color(0xFF253126),
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier.testTag("guardar_item_dialog_button")
            ) {
                val textoBoton = if (itemInicial != null) {
                    "Guardar Cambios"
                } else if (cantidadUnidades > 1) {
                    "Crear $cantidadUnidades Artículos"
                } else {
                    "Guardar Artículo"
                }
                Text(textoBoton, color = Color(0xFF00390A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.LightGray)
            }
        },
        containerColor = Color(0xFF162317),
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color(0xFF2E3E30),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = Color.LightGray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF131D14),
    unfocusedContainerColor = Color(0xFF131D14),
    cursorColor = MaterialTheme.colorScheme.primary
)
