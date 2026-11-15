package com.example.gentehablando.domain.model

/**
 * Modelo de datos del Dominio para representar un mensaje individual de chat.
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val senderId: String,
    val isFromMe: Boolean,
    val timestamp: String
)
