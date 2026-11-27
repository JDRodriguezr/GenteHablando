package com.example.gentehablando.domain.model

/**
 * Contenedor sellado (Sealed Interface) para envolver respuestas asíncronas.
 * Permite gestionar de manera tipo-segura (Type-Safe) los estados de carga,
 * éxito y error en las llamadas a repositorios/servicios de red.
 */
sealed interface Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val reason: AppError, val cause: Throwable? = null) : Resource<Nothing>
    object Loading : Resource<Nothing>
    object Idle : Resource<Nothing>
}
