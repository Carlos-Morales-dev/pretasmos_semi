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

class PrestamosRepository {

    private val db = FirebaseFirestore.getInstance()
    private val coleccionPrestamos = db.collection("prestamos")
    private val coleccionInventario = db.collection("inventario")

    /**
     * Escucha en tiempo real la lista de préstamos.
     * Mantiene sincronizada la caché local para funcionamiento sin conexión o
     * ante restricciones temporales de Firestore.
     */
    fun obtenerPrestamosFlow(): Flow<List<Prestamo>> = callbackFlow {
        trySend(cacheLocalPrestamos.value)

        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            registration = coleccionPrestamos.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PrestamosRepository", "Firestore listener aviso: ${error.message}")
                    trySend(cacheLocalPrestamos.value)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val prestamos = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Prestamo::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.fechaPrestamo?.seconds ?: 0L }
                    cacheLocalPrestamos.value = prestamos
                    trySend(prestamos)
                } else {
                    trySend(cacheLocalPrestamos.value)
                }
            }
        } catch (e: Exception) {
            Log.w("PrestamosRepository", "Error registrando snapshot listener", e)
            trySend(cacheLocalPrestamos.value)
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * Verifica si un estudiante actualmente tiene un préstamo ACTIVO pendiente de devolución.
     * Un estudiante con préstamo pendiente TIENE SU CARNET RETENIDO, por lo que NO se le
     * permite registrar un nuevo préstamo.
     */
    fun buscarPrestamoActivoPorEstudiante(codigo: String, cedula: String = ""): Prestamo? {
        val codigoLimpio = codigo.trim()
        val cedulaLimpia = cedula.trim()
        if (codigoLimpio.isBlank() && cedulaLimpia.isBlank()) return null

        return cacheLocalPrestamos.value.find { prestamo ->
            prestamo.estadoDevolucion.equals("PENDIENTE", ignoreCase = true) &&
            prestamo.carnetRetenido &&
            ((codigoLimpio.isNotBlank() && prestamo.codigoEstudiante.equals(codigoLimpio, ignoreCase = true)) ||
             (cedulaLimpia.isNotBlank() && prestamo.cedula.equals(cedulaLimpia, ignoreCase = true)))
        }
    }

    /**
     * Registra un nuevo préstamo con uno o VARIOS artículos para un estudiante.
     * Si el estudiante ya tiene un préstamo activo (carnet retenido), la operación es rechazada.
     * Además, actualiza el estado de cada artículo individual a "PRESTADO".
     */
    suspend fun registrarPrestamo(
        prestamo: Prestamo,
        itemsPrestados: List<ItemDeportivo>
    ): Result<String> {
        val codigo = prestamo.codigoEstudiante.trim()
        val cedula = prestamo.cedula.trim()

        // 1. Regla de negocio: Verificar si ya tiene carnet retenido
        val prestamoActivo = buscarPrestamoActivoPorEstudiante(codigo, cedula)
        if (prestamoActivo != null) {
            return Result.failure(
                IllegalStateException("El estudiante ya tiene un préstamo activo (#${prestamoActivo.id}) y su carnet está retenido. No puede solicitar otro préstamo sin carnet.")
            )
        }

        val prestamoId = if (prestamo.id.isNotBlank()) prestamo.id else "PR-${System.currentTimeMillis() % 100000}"
        val ahora = Timestamp.now()

        val prestamoFinal = prestamo.copy(
            id = prestamoId,
            codigoEstudiante = codigo,
            cedula = cedula,
            nombreEstudiante = prestamo.nombreEstudiante.trim(),
            itemsCodigos = itemsPrestados.map { it.id },
            itemsNombres = itemsPrestados.map { "${it.id} - ${it.nombre}" },
            cantidadTotalItems = itemsPrestados.size,
            idItem = itemsPrestados.firstOrNull()?.id ?: prestamo.idItem,
            fechaPrestamo = prestamo.fechaPrestamo ?: ahora,
            estadoDevolucion = "PENDIENTE",
            carnetRetenido = true,
            documentoRetenido = true
        )

        // 2. Actualizar caché local de préstamos inmediatamente
        cacheLocalPrestamos.update { lista ->
            listOf(prestamoFinal) + lista.filterNot { it.id == prestamoId }
        }

        // 3. Actualizar estado de los artículos a "PRESTADO" en la caché del inventario
        InventarioRepository.cacheLocal.update { lista ->
            lista.map { item ->
                if (itemsPrestados.any { it.id.equals(item.id, ignoreCase = true) }) {
                    item.copy(
                        estado = "PRESTADO",
                        cantidadDisponible = 0,
                        estudiantePrestamoId = codigo,
                        estudiantePrestamoNombre = prestamoFinal.nombreEstudiante,
                        fechaActualizacion = ahora
                    )
                } else {
                    item
                }
            }
        }

        // 4. Persistir en Firestore de manera segura (con tolerancia si falta permiso en consola)
        return try {
            try {
                coleccionPrestamos.document(prestamoId).set(prestamoFinal).await()

                // Actualizar cada documento en Firestore
                for (item in itemsPrestados) {
                    try {
                        coleccionInventario.document(item.id).update(
                            mapOf(
                                "estado" to "PRESTADO",
                                "cantidadDisponible" to 0,
                                "estudiantePrestamoId" to codigo,
                                "estudiantePrestamoNombre" to prestamoFinal.nombreEstudiante,
                                "fechaActualizacion" to ahora
                            )
                        ).await()
                    } catch (e: Exception) {
                        Log.w("PrestamosRepository", "Aviso actualizando item ${item.id} en Firestore: ${e.message}")
                    }
                }

                Result.success("Préstamo registrado exitosamente en Firebase (#$prestamoId)")
            } catch (e: Exception) {
                Log.w("PrestamosRepository", "Firestore write notice (guardado localmente): ${e.message}")
                val esPermiso = e is FirebaseFirestoreException &&
                        e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true

                if (esPermiso) {
                    Result.success("Préstamo registrado localmente (#$prestamoId). (Configura las reglas de la colección 'prestamos' en Firebase Console)")
                } else {
                    Result.success("Préstamo registrado (#$prestamoId)")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registra la devolución de los elementos de un préstamo:
     * - Libera el carnet del estudiante (carnetRetenido = false).
     * - Devuelve los implementos a estado "DISPONIBLE" en el inventario.
     */
    suspend fun registrarDevolucion(prestamoId: String): Result<String> {
        val prestamo = cacheLocalPrestamos.value.find { it.id == prestamoId }
            ?: return Result.failure(IllegalArgumentException("No se encontró el préstamo $prestamoId"))

        val ahora = Timestamp.now()
        val prestamoActualizado = prestamo.copy(
            estadoDevolucion = "DEVUELTO",
            carnetRetenido = false,
            documentoRetenido = false,
            fechaEntregaReal = ahora
        )

        // 1. Actualizar caché de préstamos
        cacheLocalPrestamos.update { lista ->
            lista.map { if (it.id == prestamoId) prestamoActualizado else it }
        }

        // 2. Liberar artículos en caché de inventario
        InventarioRepository.cacheLocal.update { lista ->
            lista.map { item ->
                if (prestamo.itemsCodigos.any { it.equals(item.id, ignoreCase = true) }) {
                    item.copy(
                        estado = "DISPONIBLE",
                        cantidadDisponible = 1,
                        estudiantePrestamoId = "",
                        estudiantePrestamoNombre = "",
                        fechaActualizacion = ahora
                    )
                } else {
                    item
                }
            }
        }

        // 3. Persistir en Firestore
        return try {
            try {
                coleccionPrestamos.document(prestamoId).set(prestamoActualizado).await()

                for (codigoItem in prestamo.itemsCodigos) {
                    try {
                        coleccionInventario.document(codigoItem).update(
                            mapOf(
                                "estado" to "DISPONIBLE",
                                "cantidadDisponible" to 1,
                                "estudiantePrestamoId" to "",
                                "estudiantePrestamoNombre" to "",
                                "fechaActualizacion" to ahora
                            )
                        ).await()
                    } catch (e: Exception) {
                        Log.w("PrestamosRepository", "Aviso liberando item $codigoItem en Firestore: ${e.message}")
                    }
                }
                Result.success("Devolución registrada. Carnet devuelto al estudiante.")
            } catch (e: Exception) {
                Log.w("PrestamosRepository", "Fallo al sincronizar devolución en Firestore: ${e.message}")
                Result.success("Devolución registrada localmente. Carnet devuelto al estudiante.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        val cacheLocalPrestamos = MutableStateFlow<List<Prestamo>>(emptyList())
    }
}
