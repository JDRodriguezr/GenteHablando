package com.example.gentehablando.domain.repository

import com.example.gentehablando.domain.model.Resource

interface UserRepository {
    /**
     * Envía la calificación o reporte de un usuario a Firebase y actualiza la reputación del objetivo.
     * @param roomId El ID de la sala donde ocurrió la conversación.
     * @param currentUserId El UID de quien envía el feedback.
     * @param rating La calificación del 1 al 4 (0 si es problema técnico o reporte).
     * @param isReport True si el usuario decidió reportar conducta indebida.
     * @param issueType String que indica si hubo problema técnico ("AUDIO_ISSUE", "DISCONNECTED", etc)
     */
    suspend fun submitFeedback(
        roomId: String,
        currentUserId: String,
        rating: Int,
        isReport: Boolean,
        issueType: String? = null,
        issueText: String? = null
    ): Resource<Unit>

    /**
     * Envía una apelación a la administración con el detalle y las salas implicadas.
     */
    suspend fun submitAppeal(
        userId: String,
        explanation: String,
        reportedRoomIds: List<String>
    ): Resource<Unit>

    /**
     * Observa el perfil de un usuario.
     */
    fun observeUserProfile(userId: String): kotlinx.coroutines.flow.Flow<com.example.gentehablando.domain.model.UserProfile>
}
