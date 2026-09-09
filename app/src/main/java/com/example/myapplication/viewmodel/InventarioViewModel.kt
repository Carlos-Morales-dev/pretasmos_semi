package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.InventarioRepository
import com.example.myapplication.data.ItemDeportivo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InventarioUiState(
    val cargando: Boolean = true,
    val items: List<ItemDeportivo> = emptyList(),
    val itemsFiltrados: List<ItemDeportivo> = emptyList(),
    val busqueda: String = "",
    val categoriaFiltro: String = "Todas",
    val estadoFiltro: String = "Todos",
    val mensajeExito: String? = null,
    val mensajeError: String? = null,
    val totalArticulosDistintos: Int = 0,
    val totalUnidadesDisponibles: Int = 0,
    val totalUnidadesTotales: Int = 0,
    val totalUnidadesDanadas: Int = 0
)

class InventarioViewModel(
    private val repository: InventarioRepository = InventarioRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventarioUiState())
    val uiState: StateFlow<InventarioUiState> = _uiState.asStateFlow()

    init {
        inicializarYEscuchar()
    }

    private fun inicializarYEscuchar() {
        viewModelScope.launch {
            // Inicializa con artículos de prueba si la colección está vacía
            repository.inicializarDatosSiVacio()

            repository.obtenerInventarioFlow()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            mensajeError = "Error al cargar inventario: ${error.localizedMessage ?: "Desconocido"}"
                        )
                    }
                }
                .collect { items ->
                    _uiState.update { estadoActual ->
                        val filtrados = aplicarFiltros(
                            items = items,
                            query = estadoActual.busqueda,
                            cat = estadoActual.categoriaFiltro,
                            est = estadoActual.estadoFiltro
                        )

                        val totalDisponibles = items.sumOf { it.cantidadDisponible }
                        val totalTotales = items.sumOf { it.cantidadTotal }
                        val danados = items.filter { it.estado.equals("DAÑADO", ignoreCase = true) }
                            .sumOf { it.cantidadTotal - it.cantidadDisponible.coerceAtLeast(0) }

                        estadoActual.copy(
                            cargando = false,
                            items = items,
                            itemsFiltrados = filtrados,
                            totalArticulosDistintos = items.size,
                            totalUnidadesDisponibles = totalDisponibles,
                            totalUnidadesTotales = totalTotales,
                            totalUnidadesDanadas = if (danados > 0) danados else items.count { it.estado.equals("DAÑADO", ignoreCase = true) }
                        )
                    }
                }
        }
    }

    fun actualizarBusqueda(nuevaBusqueda: String) {
        _uiState.update { estado ->
            val filtrados = aplicarFiltros(
                items = estado.items,
                query = nuevaBusqueda,
                cat = estado.categoriaFiltro,
                est = estado.estadoFiltro
            )
            estado.copy(
                busqueda = nuevaBusqueda,
                itemsFiltrados = filtrados
            )
        }
    }

    fun seleccionarCategoriaFiltro(categoria: String) {
        _uiState.update { estado ->
            val filtrados = aplicarFiltros(
                items = estado.items,
                query = estado.busqueda,
                cat = categoria,
                est = estado.estadoFiltro
            )
            estado.copy(
                categoriaFiltro = categoria,
                itemsFiltrados = filtrados
            )
        }
    }

    fun seleccionarEstadoFiltro(estadoFiltro: String) {
        _uiState.update { estado ->
            val filtrados = aplicarFiltros(
                items = estado.items,
                query = estado.busqueda,
                cat = estado.categoriaFiltro,
                est = estadoFiltro
            )
            estado.copy(
                estadoFiltro = estadoFiltro,
                itemsFiltrados = filtrados
            )
        }
    }

    private fun aplicarFiltros(
        items: List<ItemDeportivo>,
        query: String,
        cat: String,
        est: String
    ): List<ItemDeportivo> {
        val q = query.trim().lowercase()
        return items.filter { item ->
            // Filtro por búsqueda de ID o Nombre o Categoría
            val coincideTexto = if (q.isBlank()) {
                true
            } else {
                item.id.lowercase().contains(q) ||
                item.nombre.lowercase().contains(q) ||
                item.categoria.lowercase().contains(q) ||
                item.descripcion.lowercase().contains(q)
            }

            // Filtro por categoría
            val coincideCategoria = if (cat == "Todas") {
                true
            } else {
                item.categoria.equals(cat, ignoreCase = true)
            }

            // Filtro por estado
            val coincideEstado = if (est == "Todos") {
                true
            } else {
                item.estado.equals(est, ignoreCase = true)
            }

            coincideTexto && coincideCategoria && coincideEstado
        }
    }

    fun sugerirSiguienteId(categoria: String, nombre: String): String {
        return InventarioRepository.generarSiguienteId(
            categoria = categoria,
            nombre = nombre,
            itemsExistentes = _uiState.value.items
        )
    }

    fun sugerirSecuenciaIds(categoria: String, nombre: String, cantidad: Int): List<String> {
        return InventarioRepository.generarSecuenciaIds(
            categoria = categoria,
            nombre = nombre,
            cantidad = cantidad,
            itemsExistentes = _uiState.value.items
        )
    }

    fun guardarItem(item: ItemDeportivo, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val resultado = repository.guardarItem(item)
            resultado.onSuccess { mensaje ->
                _uiState.update { it.copy(mensajeExito = mensaje) }
                onResultado(true)
            }.onFailure { e ->
                _uiState.update { it.copy(mensajeError = "No se pudo guardar: ${e.localizedMessage}") }
                onResultado(false)
            }
        }
    }

    fun guardarVariosItems(items: List<ItemDeportivo>, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val resultado = repository.guardarVariosItems(items)
            resultado.onSuccess { mensaje ->
                _uiState.update { it.copy(mensajeExito = mensaje) }
                onResultado(true)
            }.onFailure { e ->
                _uiState.update { it.copy(mensajeError = "No se pudo guardar el lote: ${e.localizedMessage}") }
                onResultado(false)
            }
        }
    }

    fun eliminarItem(id: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            val resultado = repository.eliminarItem(id)
            resultado.onSuccess { mensaje ->
                _uiState.update { it.copy(mensajeExito = mensaje) }
                onResultado(true)
            }.onFailure { e ->
                _uiState.update { it.copy(mensajeError = "Error al eliminar: ${e.localizedMessage}") }
                onResultado(false)
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(mensajeExito = null, mensajeError = null) }
    }
}
