package com.example.gentehablando.data.repository

import com.example.gentehablando.domain.repository.MatchmakingRepository
import com.example.gentehablando.model.UserRole
import com.example.gentehablando.viewmodel.MatchmakingUiState
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class FirebaseMatchmakingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MatchmakingRepository {

    override fun observeMatchmaking(role: UserRole, userId: String): Flow<MatchmakingUiState> = callbackFlow {
        val partnerRole = if (role == UserRole.SPEAKER) UserRole.LISTENER else UserRole.SPEAKER
        val targetRole = partnerRole.name
        trySend(MatchmakingUiState.Searching(0))

        var queueListener: ListenerRegistration? = null
        var matchmakingJob: kotlinx.coroutines.Job? = null

        // 1. Me agrego a la cola
        val queueData = hashMapOf(
            "userId" to userId, // i18n: technical Firestore collection/field key; preserves the backend schema.
            "role" to role.name, // i18n: technical Firestore collection/field key; preserves the backend schema.
            "createdAt" to FieldValue.serverTimestamp() // i18n: technical Firestore collection/field key; preserves the backend schema.
        )
        try {
            firestore.collection("matchmaking_queue").document(userId).set(queueData) // i18n: technical Firestore collection/field key; preserves the backend schema.
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Escucho MI PROPIO DOCUMENTO en la cola. 
        // Si el "SPEAKER" me encuentra, agregará el "matchedRoomId" aquí.
        queueListener = firestore.collection("matchmaking_queue").document(userId) // i18n: technical Firestore collection/field key; preserves the backend schema.
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                val matchedRoomId = snapshot.getString("matchedRoomId") // i18n: technical Firestore collection/field key; preserves the backend schema.
                if (matchedRoomId != null) {
                    trySend(MatchmakingUiState.Matched(partnerRole, matchedRoomId))
                }
            }

        // 3. SOLO EL SPEAKER busca activamente un oyente
        if (role == UserRole.SPEAKER) {
            matchmakingJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    try {
                        val pendingQuery = firestore.collection("matchmaking_queue") // i18n: technical Firestore collection/field key; preserves the backend schema.
                            .whereEqualTo("role", targetRole) // i18n: technical Firestore collection/field key; preserves the backend schema.
                            .get().await()

                        // Buscamos un oyente que aún no tenga sala asignada
                        val partnerDoc = pendingQuery.documents.firstOrNull { it.id != userId && !it.contains("matchedRoomId") } // i18n: technical Firestore collection/field key; preserves the backend schema.
                        if (partnerDoc != null) {
                            val partnerId = partnerDoc.getString("userId") ?: partnerDoc.id // i18n: technical Firestore collection/field key; preserves the backend schema.
                            
                            // ID de Sala Seguro y Fresco 100%
                            val freshRoomId = "room_session_" + UUID.randomUUID().toString().take(12) // i18n: technical Generated room-ID prefix; not a display alias.

                            val newRoom = hashMapOf(
                                "active" to true, // i18n: technical Firestore collection/field key; preserves the backend schema.
                                "participants" to listOf(userId, partnerId), // i18n: technical Firestore collection/field key; preserves the backend schema.
                                "speakerId" to userId, // i18n: technical Firestore collection/field key; preserves the backend schema.
                                "listenerId" to partnerId, // i18n: technical Firestore collection/field key; preserves the backend schema.
                                "createdAt" to FieldValue.serverTimestamp() // i18n: technical Firestore collection/field key; preserves the backend schema.
                            )

                            // Operación Atómica: Crea sala y avisa a ambos usuarios (escribiendo en sus colas)
                            firestore.runTransaction { transaction ->
                                val roomRef = firestore.collection("rooms").document(freshRoomId) // i18n: technical Firestore collection/field key; preserves the backend schema.
                                val queueMyRef = firestore.collection("matchmaking_queue").document(userId) // i18n: technical Firestore collection/field key; preserves the backend schema.
                                val queuePartnerRef = firestore.collection("matchmaking_queue").document(partnerId) // i18n: technical Firestore collection/field key; preserves the backend schema.

                                transaction.set(roomRef, newRoom)
                                transaction.update(queueMyRef, "matchedRoomId", freshRoomId) // i18n: technical Firestore collection/field key; preserves the backend schema.
                                transaction.update(queuePartnerRef, "matchedRoomId", freshRoomId) // i18n: technical Firestore collection/field key; preserves the backend schema.
                            }.await()
                            break // Terminamos la búsqueda porque ya lo emparejamos
                        }
                    } catch (_: Exception) {}
                    
                    delay(2000) // Reintentar cada 2 segundos
                }
            }
        }

        awaitClose {
            queueListener?.remove()
            matchmakingJob?.cancel()
        }
    }

    override suspend fun cancelMatchmaking(userId: String) {
        try {
            firestore.collection("matchmaking_queue").document(userId).delete() // i18n: technical Firestore collection/field key; preserves the backend schema.
        } catch (_: Exception) {}
    }
}
