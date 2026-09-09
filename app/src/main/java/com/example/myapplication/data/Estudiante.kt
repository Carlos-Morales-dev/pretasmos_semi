package com.example.myapplication.data

/**
 * Directorio de estudiantes que han pedido prestado alguna vez.
 * Se guarda una sola vez por estudiante para no repetir el registro
 * cada vez que vuelve a pedir un préstamo.
 */
data class Estudiante(
    val id: String = "",              // mismo valor que codigoEstudiante o cedula, lo que exista
    val codigoEstudiante: String = "",
    val cedula: String = "",
    val nombre: String = "",
    val programa: String = "",        // opcional
    val telefono: String = ""         // opcional, útil para hacer seguimiento en caso de mora
) {
    val identificador: String
        get() = codigoEstudiante.ifBlank { cedula }
}
