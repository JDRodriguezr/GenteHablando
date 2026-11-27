package com.example.gentehablando.data.repository

import com.example.gentehablando.domain.model.ChatMessage
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.domain.model.AppError
import com.example.gentehablando.domain.model.DemoMessage
import com.example.gentehablando.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class FakeChatRepositoryImpl @Inject constructor() : ChatRepository {

    private val messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val isTypingFlow = MutableStateFlow(false)
    private val isRoomActiveFlow = MutableStateFlow(true)
    private val scope = CoroutineScope(Dispatchers.Default)

    private val simulatedReplies = listOf(
        DemoMessage.REPLY_1,
        DemoMessage.REPLY_2,
        DemoMessage.REPLY_3,
        DemoMessage.REPLY_4,
        DemoMessage.REPLY_5
    )
    private var replyIndex = 0

    override fun observeMessages(roomId: String, currentUserId: String): Flow<List<ChatMessage>> {
        if (messagesFlow.value.isEmpty()) {
            val currentTime = System.currentTimeMillis()
            messagesFlow.value = listOf(
                ChatMessage(
                    id = "system_1", // i18n: technical Synthetic message/sender ID; not a display label.
                    text = "",
                    senderId = "system", // i18n: technical Synthetic message/sender ID; not a display label.
                    isFromMe = false,
                    timestamp = currentTime,
                    demoText = DemoMessage.CONNECTED
                )
            )
        }
        return messagesFlow.asStateFlow()
    }

    override suspend fun sendMessage(roomId: String, senderId: String, text: String): Resource<Unit> {
        if (text.isBlank()) return Resource.Error(AppError.EmptyMessage)

        val currentTime = System.currentTimeMillis()
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
                text = "",
                senderId = "fake_partner", // i18n: technical Synthetic message/sender ID; not a display label.
                isFromMe = false,
                timestamp = System.currentTimeMillis(),
                demoText = replyText
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
