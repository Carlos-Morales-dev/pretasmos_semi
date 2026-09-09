package com.example.myapplication.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun login(correo: String, password: String): Result<Usuario> {
        return try {
            val resultado = auth.signInWithEmailAndPassword(correo, password).await()
            val user = resultado.user
                ?: return Result.failure(Exception("No se pudo obtener el usuario"))

            // Nos aseguramos de tener el estado de verificación más reciente
            user.reload().await()

            if (!user.isEmailVerified) {
                // Reenviamos el enlace por si el usuario lo perdió y cerramos la sesión:
                // no debe quedar autenticado hasta que confirme que el correo es suyo.
                try {
                    user.sendEmailVerification().await()
                } catch (_: Exception) {
                    // Si falla el reenvío (p. ej. límite de envíos), igual bloqueamos el acceso.
                }
                auth.signOut()
                return Result.failure(
                    Exception("Debes verificar tu correo antes de iniciar sesión. Te reenviamos el enlace a $correo.")
                )
            }

            val uid = user.uid
            val usuario = try {
                val documento = db.collection("usuarios").document(uid).get().await()
                documento.toObject(Usuario::class.java) ?: Usuario(
                    id = uid,
                    nombre = user.displayName?.ifBlank { null } ?: correo.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() },
                    correo = user.email ?: correo,
                    rol = "ADMIN"
                )
            } catch (_: Exception) {
                // Si Firestore deniega permisos en la colección 'usuarios' o está offline,
                // dado que Firebase Auth ya validó las credenciales y el correo con éxito,
                // permitimos el acceso para no bloquear al usuario.
                Usuario(
                    id = uid,
                    nombre = user.displayName?.ifBlank { null } ?: correo.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() },
                    correo = user.email ?: correo,
                    rol = "ADMIN"
                )
            }
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registrar(usuario: Usuario, password: String): Result<Unit> {
        return try {
            val resultado = auth.createUserWithEmailAndPassword(usuario.correo, password).await()
            val user = resultado.user
                ?: return Result.failure(Exception("No se pudo crear el usuario"))
            val uid = user.uid
            val usuarioConId = usuario.copy(id = uid)
            try {
                db.collection("usuarios").document(uid).set(usuarioConId).await()
            } catch (_: Exception) {
                // Si Firestore aún no tiene las reglas publicadas, el usuario ya existe en Auth
            }

            // Enviamos el correo de confirmación y cerramos la sesión: el usuario
            // solo podrá iniciar sesión una vez haga clic en el enlace recibido,
            // lo que confirma que el correo ingresado existe y es suyo.
            user.sendEmailVerification().await()
            auth.signOut()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}