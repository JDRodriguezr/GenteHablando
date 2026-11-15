package com.example.gentehablando.data.repository

import com.example.gentehablando.domain.model.ChatMessage
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class FakeChatRepositoryImpl @Inject constructor() : ChatRepository {

    private val messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val isTypingFlow = MutableStateFlow(false)
    private val isRoomActiveFlow = MutableStateFlow(true)
    private val scope = CoroutineScope(Dispatchers.Default)

    private val simulatedReplies = listOf(
        "Te entiendo perfectamente, tómate el tiempo que necesites.",
        "Gracias por compartir eso conmigo. ¿Desde hace cuánto te sientes así?",
        "Es completamente comprensible que te afecte de esa manera.",
        "Aquí sigo escuchándote con calma, continúa cuando quieras.",
        "A veces solo ponerlo en palabras ya quita un poco de peso de encima."
    )
    private var replyIndex = 0

    override fun observeMessages(roomId: String, currentUserId: String): Flow<List<ChatMessage>> {
        if (messagesFlow.value.isEmpty()) {
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            messagesFlow.value = listOf(
                ChatMessage(
                    id = "system_1",
                    text = "Conectado. Este es un espacio seguro para conversar.",
                    senderId = "system",
                    isFromMe = false,
                    timestamp = currentTime
                )
            )
        }
        return messagesFlow.asStateFlow()
    }

    override suspend fun sendMessage(roomId: String, senderId: String, text: String): Resource<Unit> {
        if (text.isBlank()) return Resource.Error("Mensaje vacío")

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = text.trim(),
            senderId = senderId,
            isFromMe = true,
            timestamp = currentTime
        )

        messagesFlow.value = messagesFlow.value + userMsg

        // Simular respuesta automática en segundo plano
        scope.launch {
            delay(1200L)
            isTypingFlow.value = true
            delay(2000L)
            isTypingFlow.value = false

            val replyText = simulatedReplies[replyIndex % simulatedReplies.size]
            replyIndex++
            val partnerMsg = ChatMessage(
                id = System.currentTimeMillis().toString(),
                text = replyText,
                senderId = "fake_partner",
                isFromMe = false,
                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            )
            messagesFlow.value = messagesFlow.value + partnerMsg
        }

        return Resource.Success(Unit)
    }

    override fun observePartnerTypingStatus(roomId: String, currentUserId: String): Flow<Boolean> {
        return isTypingFlow.asStateFlow()
    }

    override suspend fun setTypingStatus(roomId: String, userId: String, isTyping: Boolean): Resource<Unit> {
        // Simulación
        return Resource.Success(Unit)
    }

    override fun observeRoomActiveStatus(roomId: String): Flow<Boolean> {
        return isRoomActiveFlow.asStateFlow()
    }

    override suspend fun leaveRoom(roomId: String, userId: String): Resource<Unit> {
        isRoomActiveFlow.value = false
        isTypingFlow.value = false
        return Resource.Success(Unit)
    }
}
