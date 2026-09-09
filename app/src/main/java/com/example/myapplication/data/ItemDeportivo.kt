package com.example.myapplication.data

import com.google.firebase.Timestamp

data class ItemDeportivo(
    val id: String = "",                         // Código único individual (ej: BA-001, BA-002, RQ-001)
    val nombre: String = "",                     // Nombre o referencia del elemento
    val categoria: String = "Balones",           // Categoría deportiva
    val icono: String = "⚽",                    // Ícono representativo
    val estado: String = "DISPONIBLE",           // DISPONIBLE, PRESTADO, DAÑADO, EN REPARACIÓN
    val estudiantePrestamoId: String = "",       // Código del estudiante si se encuentra en préstamo
    val estudiantePrestamoNombre: String = "",   // Nombre del estudiante si se encuentra en préstamo
    val cantidadTotal: Int = 1,                  // 1 unidad física por cada registro individual
    val cantidadDisponible: Int = 1,             // 1 si DISPONIBLE, 0 si PRESTADO o DAÑADO
    val descripcion: String = "",                // Notas de estado físico, serial o detalles
    val fechaCreacion: Timestamp? = null,
    val fechaActualizacion: Timestamp? = null
)

