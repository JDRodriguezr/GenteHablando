package com.example.gentehablando.domain.repository

import com.example.gentehablando.model.UserRole
import com.example.gentehablando.viewmodel.MatchmakingUiState
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del Repositorio para la sala de espera y emparejamiento.
 */
interface MatchmakingRepository {
    /**
     * Inicia la búsqueda de un compañero según el rol asignado (SPEAKER o LISTENER).
     * Emite los cambios de estado (Searching, Matched, Idle) a través de un Flow reactivo.
     */
    fun observeMatchmaking(role: UserRole, userId: String): Flow<MatchmakingUiState>

    /**
     * Cancela la búsqueda activa y retira al usuario de la cola.
     */
    suspend fun cancelMatchmaking(userId: String)
}
