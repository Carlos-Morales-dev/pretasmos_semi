package com.example.myapplication.data

// data/Usuario.kt
data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val codigo: String = "",
    val correo: String = "",
    val rol: String = "ESTUDIANTE"
)
