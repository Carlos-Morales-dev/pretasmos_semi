package com.example.myapplication.data

import com.google.firebase.Timestamp

data class Prestamo(
    val id: String = "",
    val codigoEstudiante: String = "",       // Código estudiantil
    val nombreEstudiante: String = "",       // Nombre del estudiante
    val cedula: String = "",                 // Cédula de ciudadanía
    val telefono: String = "",               // Contacto
    val idItem: String = "",                 // Código del primer ítem o resumen
    val itemsCodigos: List<String> = emptyList(), // Lista de IDs físicos prestados: ["BA-001", "RQ-002"]
    val itemsNombres: List<String> = emptyList(), // Lista de nombres descriptivos
    val cantidadTotalItems: Int = 1,         // Total de implementos prestados en esta solicitud
    val fechaPrestamo: Timestamp? = null,
    val fechaEntregaEsperada: Timestamp? = null,
    val fechaEntregaReal: Timestamp? = null,
    val estadoDevolucion: String = "PENDIENTE", // "PENDIENTE" o "DEVUELTO"
    val observaciones: String = "",
    val documentoRetenido: Boolean = true,   // true mientras el monitor tiene el carnet/cédula físico
    val carnetRetenido: Boolean = true,      // true si el carnet está en custodia
    val tipoDocumento: String = "CARNET"     // "CARNET" o "CEDULA"
) {
    /**
     * Identificador para mostrar en pantallas y reportes:
     * usa el código de estudiante si existe; si no, cae en la cédula.
     */
    val identificadorPrestatario: String
        get() = codigoEstudiante.ifBlank { cedula }
}

