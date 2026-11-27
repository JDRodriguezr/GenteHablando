package com.example.gentehablando.domain.model

/**
 * Modelo de datos del Dominio para representar un mensaje individual de chat.
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val senderId: String,
    val isFromMe: Boolean,
    val timestamp: Long, // Unix epoch milliseconds; format only at the display boundary.
    val demoText: DemoMessage? = null
)

enum class DemoMessage {
    CONNECTED,
    REPLY_1,
    REPLY_2,
    REPLY_3,
    REPLY_4,
    REPLY_5
}
