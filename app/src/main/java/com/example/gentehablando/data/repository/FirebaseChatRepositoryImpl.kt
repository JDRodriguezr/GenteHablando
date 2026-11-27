package com.example.gentehablando.data.repository

import com.example.gentehablando.domain.model.ChatMessage
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.domain.model.AppError
import com.example.gentehablando.domain.repository.ChatRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class FirebaseChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    override fun observeMessages(roomId: String, currentUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        val messagesRef = firestore.collection("rooms") // i18n: technical Firestore collection/field key; preserves the backend schema.
            .document(roomId)
            .collection("messages") // i18n: technical Firestore collection/field key; preserves the backend schema.
            // i18n: technical Firestore collection/field key; preserves the backend schema.
            .orderBy("createdAt", Query.Direction.ASCENDING) // Ordenamos por tiempo de Servidor

        val listener = messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val text = doc.getString("text") ?: "" // i18n: technical Firestore collection/field key; preserves the backend schema.
                    val senderId = doc.getString("senderId") ?: "" // i18n: technical Firestore collection/field key; preserves the backend schema.
                    
                    // Si el createdAt es null (está en tránsito hacia Firebase), usamos la fecha de hoy
                    val createdAt = doc.getDate("createdAt") ?: Date() // i18n: technical Firestore collection/field key; preserves the backend schema.
                    ChatMessage(
                        id = id,
                        text = text,
                        senderId = senderId,
                        isFromMe = senderId == currentUserId,
                        timestamp = createdAt.time
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
                "text" to text.trim(), // i18n: technical Firestore collection/field key; preserves the backend schema.
                "senderId" to senderId, // i18n: technical Firestore collection/field key; preserves the backend schema.
                // i18n: technical Firestore collection/field key; preserves the backend schema.
                "createdAt" to FieldValue.serverTimestamp() // Sello universal de Google
            )

            firestore.collection("rooms") // i18n: technical Firestore collection/field key; preserves the backend schema.
                .document(roomId)
                .collection("messages") // i18n: technical Firestore collection/field key; preserves the backend schema.
                .add(messageMap)
                .await()

            // Al enviar mensaje, limpiar estado de escritura
            firestore.collection("rooms").document(roomId).update("typingUserId", null) // i18n: technical Firestore collection/field key; preserves the backend schema.

            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(AppError.SendMessage, e)
        }
    }

    override fun observePartnerTypingStatus(roomId: String, currentUserId: String): Flow<Boolean> = callbackFlow {
        val roomRef = firestore.collection("rooms").document(roomId) // i18n: technical Firestore collection/field key; preserves the backend schema.

        val listener = roomRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(false)
                return@addSnapshotListener
            }

            val typingUserId = snapshot.getString("typingUserId") // i18n: technical Firestore collection/field key; preserves the backend schema.
            val isPartnerTyping = typingUserId != null && typingUserId != currentUserId
            trySend(isPartnerTyping)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun setTypingStatus(roomId: String, userId: String, isTyping: Boolean): Resource<Unit> {
        return try {
            val roomRef = firestore.collection("rooms").document(roomId) // i18n: technical Firestore collection/field key; preserves the backend schema.
            if (isTyping) {
                roomRef.update("typingUserId", userId).await() // i18n: technical Firestore collection/field key; preserves the backend schema.
            } else {
                roomRef.update("typingUserId", null).await() // i18n: technical Firestore collection/field key; preserves the backend schema.
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(AppError.UpdateTyping, e)
        }
    }

    override fun observeRoomActiveStatus(roomId: String): Flow<Boolean> = callbackFlow {
        val roomRef = firestore.collection("rooms").document(roomId) // i18n: technical Firestore collection/field key; preserves the backend schema.

        val listener = roomRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(true)
                return@addSnapshotListener
            }

            val isActive = snapshot.getBoolean("active") ?: true // i18n: technical Firestore collection/field key; preserves the backend schema.
            trySend(isActive)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun leaveRoom(roomId: String, userId: String): Resource<Unit> {
        return try {
            firestore.collection("rooms") // i18n: technical Firestore collection/field key; preserves the backend schema.
                .document(roomId)
                .update("active", false, "closedBy", userId, "typingUserId", null) // i18n: technical Firestore collection/field key; preserves the backend schema.
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(AppError.LeaveRoom, e)
        }
    }
}
