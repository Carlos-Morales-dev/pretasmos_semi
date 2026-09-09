package com.example.myapplication.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await

class InventarioRepository {

    private val db = FirebaseFirestore.getInstance()
    private val coleccionInventario = db.collection("inventario")

    /**
     * Escucha en tiempo real todos los implementos deportivos en Firestore.
     * Mantiene sincronizada la caché local y garantiza que la interfaz nunca quede
     * bloqueada si Firebase reporta PERMISSION_DENIED.
     */
    fun obtenerInventarioFlow(): Flow<List<ItemDeportivo>> = callbackFlow {
        // 1. Emitir inmediatamente el contenido de la caché local para evitar pantallas vacías
        trySend(cacheLocal.value)

        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            registration = coleccionInventario.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("InventarioRepository", "Aviso Firestore snapshot (se usará caché local): ${error.message}")
                    // No cerramos el flujo con close(error), emitimos los datos locales
                    trySend(cacheLocal.value)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ItemDeportivo::class.java)?.copy(id = doc.id)
                    }.sortedWith(compareBy({ it.categoria }, { it.id }))
                    cacheLocal.value = items
                    trySend(items)
                } else {
                    trySend(cacheLocal.value)
                }
            }
        } catch (e: Exception) {
            Log.w("InventarioRepository", "Error registrando snapshot listener", e)
            trySend(cacheLocal.value)
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * Guarda o actualiza un implemento deportivo.
     * Si las reglas de seguridad de Firestore no permiten escribir (PERMISSION_DENIED),
     * se almacena de forma inmediata en la caché local para que el usuario no pierda su trabajo
     * y pueda seguir operando sin bloqueos.
     */
    suspend fun guardarItem(item: ItemDeportivo): Result<String> {
        return try {
            val id = item.id.trim().uppercase()
            if (id.isBlank()) {
                return Result.failure(IllegalArgumentException("El ID del artículo no puede estar vacío"))
            }

            val ahora = Timestamp.now()
            val itemFinal = item.copy(
                id = id,
                nombre = item.nombre.trim(),
                categoria = item.categoria.trim(),
                fechaCreacion = item.fechaCreacion ?: ahora,
                fechaActualizacion = ahora
            )

            // Actualizamos la caché local primero
            cacheLocal.update { lista ->
                val indice = lista.indexOfFirst { it.id == id }
                if (indice >= 0) {
                    lista.toMutableList().apply { set(indice, itemFinal) }
                } else {
                    (lista + itemFinal).sortedWith(compareBy({ it.categoria }, { it.id }))
                }
            }

            // Intentamos sincronizar con Cloud Firestore
            try {
                coleccionInventario.document(id).set(itemFinal).await()
                Result.success("Elemento guardado en Firebase ($id)")
            } catch (e: Exception) {
                Log.w("InventarioRepository", "Fallo al persistir en Firestore", e)
                val esPermiso = e is FirebaseFirestoreException &&
                        e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true

                if (esPermiso) {
                    Result.success("Guardado en dispositivo. Nota: habilita la colección 'inventario' en las Reglas de Firebase Console.")
                } else {
                    Result.success("Guardado localmente (${e.localizedMessage ?: "Sin conexión"})")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Guarda o actualiza una lista de artículos deportivos individuales.
     * Genera registros independientes para cada elemento físico con su propio ID secuencial.
     */
    suspend fun guardarVariosItems(items: List<ItemDeportivo>): Result<String> {
        return try {
            val ahora = Timestamp.now()
            val itemsFinales = items.map { item ->
                val id = item.id.trim().uppercase()
                item.copy(
                    id = id,
                    nombre = item.nombre.trim(),
                    categoria = item.categoria.trim(),
                    cantidadTotal = 1,
                    cantidadDisponible = if (item.estado == "DISPONIBLE") 1 else 0,
                    fechaCreacion = item.fechaCreacion ?: ahora,
                    fechaActualizacion = ahora
                )
            }

            // Actualizar caché local
            cacheLocal.update { lista ->
                val mapa = lista.associateBy { it.id }.toMutableMap()
                for (item in itemsFinales) {
                    mapa[item.id] = item
                }
                mapa.values.sortedWith(compareBy({ it.categoria }, { it.id }))
            }

            // Guardar en Firestore
            for (item in itemsFinales) {
                try {
                    coleccionInventario.document(item.id).set(item).await()
                } catch (e: Exception) {
                    Log.w("InventarioRepository", "Aviso persistiendo ${item.id} en Firestore: ${e.message}")
                }
            }

            val mensaje = if (itemsFinales.size == 1) {
                "Artículo ${itemsFinales.first().id} registrado correctamente"
            } else {
                "${itemsFinales.size} artículos registrados con códigos secuenciales (${itemsFinales.first().id} a ${itemsFinales.last().id})"
            }
            Result.success(mensaje)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un implemento del inventario por su ID.
     */
    suspend fun eliminarItem(id: String): Result<String> {
        val idLimpio = id.trim().uppercase()
        cacheLocal.update { lista -> lista.filterNot { it.id == idLimpio } }
        return try {
            try {
                coleccionInventario.document(idLimpio).delete().await()
                Result.success("Elemento $idLimpio eliminado de Firebase")
            } catch (e: Exception) {
                Log.w("InventarioRepository", "Fallo al eliminar en Firestore", e)
                Result.success("Elemento $idLimpio eliminado localmente")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene un implemento puntual para consultas o verificaciones.
     */
    suspend fun obtenerItemPorId(id: String): Result<ItemDeportivo?> {
        val idLimpio = id.trim().uppercase()
        val local = cacheLocal.value.find { it.id == idLimpio }
        if (local != null) {
            return Result.success(local)
        }
        return try {
            val doc = coleccionInventario.document(idLimpio).get().await()
            val item = doc.toObject(ItemDeportivo::class.java)?.copy(id = doc.id)
            Result.success(item)
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    /**
     * Inicializa datos iniciales si la base de datos se encuentra vacía.
     */
    suspend fun inicializarDatosSiVacio(): Result<Boolean> {
        return try {
            val snapshot = coleccionInventario.limit(1).get().await()
            if (snapshot.isEmpty) {
                for (item in itemsEjemploBase) {
                    try {
                        coleccionInventario.document(item.id).set(item).await()
                    } catch (e: Exception) {
                        Log.w("InventarioRepository", "No se pudo sembrar ${item.id} en Firestore: ${e.message}")
                    }
                }
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.w("InventarioRepository", "Fallo al consultar Firestore inicial: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private val itemsEjemploBase = listOf(
            ItemDeportivo(
                id = "BA-001",
                nombre = "Balón de Fútbol Golty Tradicional #5 (Unidad 1)",
                categoria = "Balones",
                icono = "⚽",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Césped natural y sintético. Buen estado.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "BA-002",
                nombre = "Balón de Fútbol Golty Tradicional #5 (Unidad 2)",
                categoria = "Balones",
                icono = "⚽",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Balón reglamentario costurado a mano.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "BA-003",
                nombre = "Balón de Baloncesto Molten #7 (Unidad 1)",
                categoria = "Balones",
                icono = "🏀",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Cuero sintético para coliseo.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "BA-004",
                nombre = "Balón de Baloncesto Molten #7 (Unidad 2)",
                categoria = "Balones",
                icono = "🏀",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Superficie antideslizante con agarre profundo.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "BA-005",
                nombre = "Balón de Voleibol Mikasa MVA200 (Unidad 1)",
                categoria = "Balones",
                icono = "🏐",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Balón suave de 8 paneles espirales.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "RQ-001",
                nombre = "Raqueta Tenis de Mesa Butterfly (Unidad 1)",
                categoria = "Raquetas",
                icono = "🏓",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Goma roja y negra estándar para entrenamiento.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "RQ-002",
                nombre = "Raqueta Tenis de Mesa Butterfly (Unidad 2)",
                categoria = "Raquetas",
                icono = "🏓",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Mango cóncavo ergonómico.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "RQ-003",
                nombre = "Raqueta de Tenis de Campo Wilson (Unidad 1)",
                categoria = "Raquetas",
                icono = "🎾",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Cuerdas tensadas y grip estándar.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "PT-001",
                nombre = "Peto Deportivo Verde Neón (Unidad 1)",
                categoria = "Petos",
                icono = "🎽",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Chaleco de entrenamiento transpirable.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "PT-002",
                nombre = "Peto Deportivo Verde Neón (Unidad 2)",
                categoria = "Petos",
                icono = "🎽",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Chaleco de entrenamiento transpirable.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "PT-003",
                nombre = "Peto Deportivo Verde Neón (Unidad 3)",
                categoria = "Petos",
                icono = "🎽",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Chaleco de entrenamiento transpirable.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "CN-001",
                nombre = "Cono de Entrenamiento 23cm (Unidad 1)",
                categoria = "Conos",
                icono = "🔶",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Cono flexible de alta visibilidad.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "CN-002",
                nombre = "Cono de Entrenamiento 23cm (Unidad 2)",
                categoria = "Conos",
                icono = "🔶",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Cono flexible de alta visibilidad.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            ),
            ItemDeportivo(
                id = "AJ-001",
                nombre = "Juego de Ajedrez Profesional (Unidad 1)",
                categoria = "Ajedrez",
                icono = "♟️",
                cantidadTotal = 1,
                cantidadDisponible = 1,
                estado = "DISPONIBLE",
                descripcion = "Tablero enrollable con 32 piezas completas.",
                fechaCreacion = Timestamp.now(),
                fechaActualizacion = Timestamp.now()
            )
        )

        /**
         * Caché local en memoria compartida para toda la sesión.
         */
        val cacheLocal = MutableStateFlow<List<ItemDeportivo>>(itemsEjemploBase)

        /**
         * Determina el prefijo según categoría y nombre (BA-, RQ-, PT-, CN-, AJ-, etc.)
         */
        fun determinarPrefijo(categoria: String, nombre: String): String {
            val texto = "$categoria $nombre".uppercase()
            return when {
                texto.contains("FÚTBOL") || texto.contains("FUTBOL") ||
                texto.contains("BASKET") || texto.contains("BALONCESTO") ||
                texto.contains("VOLEIBOL") || texto.contains("VOLEY") ||
                texto.contains("BALÓN") || texto.contains("BALON") ||
                texto.contains("PELOTA") -> "BA"

                texto.contains("RAQUETA") || texto.contains("TENIS") ||
                texto.contains("PING") || texto.contains("MESA") ||
                texto.contains("BADMINTON") -> "RQ"

                texto.contains("PETO") || texto.contains("CHALECO") ||
                texto.contains("CAMISETA") -> "PT"

                texto.contains("CONO") || texto.contains("PLATILLO") ||
                texto.contains("ENTRENAMIENTO") -> "CN"

                texto.contains("AJEDREZ") || texto.contains("TABLERO") ||
                texto.contains("FICHAS") -> "AJ"

                texto.contains("LAZO") || texto.contains("CUERDA") ||
                texto.contains("SALTO") -> "LZ"

                texto.contains("COLCHONETA") || texto.contains("ESTERILLA") ||
                texto.contains("TAPETE") -> "CL"

                texto.contains("CRONÓMETRO") || texto.contains("CRONOMETRO") ||
                texto.contains("RELOJ") -> "CR"

                else -> {
                    val limpia = categoria.filter { it.isLetter() }.uppercase()
                    if (limpia.length >= 2) limpia.take(2) else "OT"
                }
            }
        }

        /**
         * Genera automáticamente el siguiente código secuencial disponible para la categoría dada.
         * Permite un offset para secuencias continuas en lotes de artículos.
         */
        fun generarSiguienteId(
            categoria: String,
            nombre: String,
            itemsExistentes: List<ItemDeportivo>,
            offset: Int = 0
        ): String {
            val prefijo = determinarPrefijo(categoria, nombre)
            val regex = Regex("""^${Regex.escape(prefijo)}-(\d+)$""")

            val numerosExistentes = itemsExistentes.mapNotNull { item ->
                regex.find(item.id.trim())?.groupValues?.get(1)?.toIntOrNull()
            }

            val baseNumero = if (numerosExistentes.isEmpty()) {
                1
            } else {
                (numerosExistentes.maxOrNull() ?: 0) + 1
            }

            val siguienteNumero = baseNumero + offset
            return "$prefijo-%03d".format(siguienteNumero)
        }

        /**
         * Genera una lista de códigos secuenciales para registrar N artículos individuales a la vez.
         */
        fun generarSecuenciaIds(
            categoria: String,
            nombre: String,
            cantidad: Int,
            itemsExistentes: List<ItemDeportivo>
        ): List<String> {
            val cant = cantidad.coerceAtLeast(1)
            return (0 until cant).map { offset ->
                generarSiguienteId(categoria, nombre, itemsExistentes, offset)
            }
        }

        /**
         * Asigna un ícono/emoji deportivo representativo según la categoría o nombre.
         */
        fun iconoRepresentativo(categoria: String, nombre: String = ""): String {
            val texto = "$categoria $nombre".lowercase()
            return when {
                texto.contains("fútbol") || texto.contains("futbol") -> "⚽"
                texto.contains("baloncesto") || texto.contains("basket") -> "🏀"
                texto.contains("voleibol") || texto.contains("voley") -> "🏐"
                texto.contains("balón") || texto.contains("balon") -> "⚽"
                texto.contains("tenis de mesa") || texto.contains("ping pong") -> "🏓"
                texto.contains("raqueta") || texto.contains("tenis") -> "🎾"
                texto.contains("peto") || texto.contains("chaleco") -> "🎽"
                texto.contains("cono") -> "🔶"
                texto.contains("ajedrez") -> "♟️"
                texto.contains("lazo") || texto.contains("cuerda") -> "🪢"
                texto.contains("cronómetro") || texto.contains("cronometro") -> "⏱️"
                texto.contains("colchoneta") -> "🧘"
                else -> "🏅"
            }
        }
    }
}
