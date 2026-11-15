package com.example.gentehablando.domain.repository

import com.example.gentehablando.domain.model.ChatMessage
import com.example.gentehablando.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del Repositorio de Chat para la mensajería en tiempo real.
 */
interface ChatRepository {
    /**
     * Escucha en tiempo real la lista de mensajes de una sala específica.
     */
    fun observeMessages(roomId: String, currentUserId: String): Flow<List<ChatMessage>>

    /**
     * Envía un nuevo mensaje a la sala.
     */
    suspend fun sendMessage(roomId: String, senderId: String, text: String): Resource<Unit>

    /**
     * Escucha si el otro integrante de la sala está escribiendo.
     */
    fun observePartnerTypingStatus(roomId: String, currentUserId: String): Flow<Boolean>

    /**
     * Actualiza el estado de escritura del usuario actual en la sala.
     */
    suspend fun setTypingStatus(roomId: String, userId: String, isTyping: Boolean): Resource<Unit>

    /**
     * Escucha en tiempo real si la sala sigue activa o si el compañero la finalizó.
     */
    fun observeRoomActiveStatus(roomId: String): Flow<Boolean>

    /**
     * Notifica el abandono de la sala de chat.
     */
    suspend fun leaveRoom(roomId: String, userId: String): Resource<Unit>
}
