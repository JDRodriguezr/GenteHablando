package com.example.gentehablando.data.repository

import com.example.gentehablando.domain.model.ChatMessage
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.domain.repository.ChatRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class FirebaseChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    override fun observeMessages(roomId: String, currentUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        val messagesRef = firestore.collection("rooms")
            .document(roomId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING) // Ordenamos por tiempo de Servidor

        val listener = messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val text = doc.getString("text") ?: ""
                    val senderId = doc.getString("senderId") ?: ""
                    
                    // Si el createdAt es null (está en tránsito hacia Firebase), usamos la fecha de hoy
                    val createdAt = doc.getDate("createdAt") ?: Date()
                    val timeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(createdAt)

                    ChatMessage(
                        id = id,
                        text = text,
                        senderId = senderId,
                        isFromMe = senderId == currentUserId,
                        timestamp = timeFormatted
                    )
                }
                trySend(messages)
            }
        }

        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(roomId: String, senderId: String, text: String): Resource<Unit> {
        return try {
            val messageMap = hashMapOf(
                "text" to text.trim(),
                "senderId" to senderId,
                "createdAt" to FieldValue.serverTimestamp() // Sello universal de Google
            )

            firestore.collection("rooms")
                .document(roomId)
                .collection("messages")
                .add(messageMap)
                .await()

            // Al enviar mensaje, limpiar estado de escritura
            firestore.collection("rooms").document(roomId).update("typingUserId", null)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al enviar mensaje", e)
        }
    }

    override fun observePartnerTypingStatus(roomId: String, currentUserId: String): Flow<Boolean> = callbackFlow {
        val roomRef = firestore.collection("rooms").document(roomId)

        val listener = roomRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(false)
                return@addSnapshotListener
            }

            val typingUserId = snapshot.getString("typingUserId")
            val isPartnerTyping = typingUserId != null && typingUserId != currentUserId
            trySend(isPartnerTyping)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun setTypingStatus(roomId: String, userId: String, isTyping: Boolean): Resource<Unit> {
        return try {
            val roomRef = firestore.collection("rooms").document(roomId)
            if (isTyping) {
                roomRef.update("typingUserId", userId).await()
            } else {
                roomRef.update("typingUserId", null).await()
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al actualizar escritura", e)
        }
    }

    override fun observeRoomActiveStatus(roomId: String): Flow<Boolean> = callbackFlow {
        val roomRef = firestore.collection("rooms").document(roomId)

        val listener = roomRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(true)
                return@addSnapshotListener
            }

            val isActive = snapshot.getBoolean("active") ?: true
            trySend(isActive)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun leaveRoom(roomId: String, userId: String): Resource<Unit> {
        return try {
            firestore.collection("rooms")
                .document(roomId)
                .update("active", false, "closedBy", userId, "typingUserId", null)
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al abandonar la sala", e)
        }
    }
}
