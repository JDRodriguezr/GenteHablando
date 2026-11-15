package com.example.gentehablando.domain.repository

import com.example.gentehablando.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del Repositorio de Autenticación.
 * Define las operaciones requeridas para gestionar la identidad del usuario
 * (anónimo) sin acoplar la aplicación a Firebase Auth u otros proveedores.
 */
interface AuthRepository {
    /**
     * Inicia sesión de forma anónima. Retorna el UID asignado al usuario.
     */
    suspend fun signInAnonymously(): Resource<String>

    /**
     * Inicia sesión con Google (Autenticación real).
     * @param idToken Token JWT obtenido del Google Credential Manager.
     */
    suspend fun signInWithGoogle(idToken: String): Resource<String>

    /**
     * Retorna el UID del usuario actualmente autenticado, o null si no existe.
     */
    fun getCurrentUserId(): String?
    
    /**
     * Retorna true si el usuario tiene una cuenta real (no es anónimo).
     */
    fun isUserLoggedInWithAccount(): Boolean

    /**
     * Cierra la sesión del usuario.
     */
    suspend fun signOut()

    /**
     * Flujo reactivo del estado de autenticación (UID del usuario o null si cerró sesión).
     */
    fun observeAuthState(): Flow<String?>
}
