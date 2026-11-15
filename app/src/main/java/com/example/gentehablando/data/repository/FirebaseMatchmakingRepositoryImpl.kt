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
        val targetRole = if (role == UserRole.SPEAKER) UserRole.LISTENER.name else UserRole.SPEAKER.name
        trySend(MatchmakingUiState.Searching(0))

        var queueListener: ListenerRegistration? = null
        var matchmakingJob: kotlinx.coroutines.Job? = null

        // 1. Me agrego a la cola
        val queueData = hashMapOf(
            "userId" to userId,
            "role" to role.name,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            firestore.collection("matchmaking_queue").document(userId).set(queueData)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Escucho MI PROPIO DOCUMENTO en la cola. 
        // Si el "SPEAKER" me encuentra, agregará el "matchedRoomId" aquí.
        queueListener = firestore.collection("matchmaking_queue").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                val matchedRoomId = snapshot.getString("matchedRoomId")
                if (matchedRoomId != null) {
                    val partnerAlias = if (role == UserRole.SPEAKER) "Oyente" else "Hablante"
                    trySend(MatchmakingUiState.Matched(partnerAlias, matchedRoomId))
                }
            }

        // 3. SOLO EL SPEAKER busca activamente un oyente
        if (role == UserRole.SPEAKER) {
            matchmakingJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    try {
                        val pendingQuery = firestore.collection("matchmaking_queue")
                            .whereEqualTo("role", targetRole)
                            .get().await()

                        // Buscamos un oyente que aún no tenga sala asignada
                        val partnerDoc = pendingQuery.documents.firstOrNull { it.id != userId && !it.contains("matchedRoomId") }
                        if (partnerDoc != null) {
                            val partnerId = partnerDoc.getString("userId") ?: partnerDoc.id
                            
                            // ID de Sala Seguro y Fresco 100%
                            val freshRoomId = "room_session_" + UUID.randomUUID().toString().take(12)

                            val newRoom = hashMapOf(
                                "active" to true,
                                "participants" to listOf(userId, partnerId),
                                "speakerId" to userId,
                                "listenerId" to partnerId,
                                "createdAt" to FieldValue.serverTimestamp()
                            )

                            // Operación Atómica: Crea sala y avisa a ambos usuarios (escribiendo en sus colas)
                            firestore.runTransaction { transaction ->
                                val roomRef = firestore.collection("rooms").document(freshRoomId)
                                val queueMyRef = firestore.collection("matchmaking_queue").document(userId)
                                val queuePartnerRef = firestore.collection("matchmaking_queue").document(partnerId)

                                transaction.set(roomRef, newRoom)
                                transaction.update(queueMyRef, "matchedRoomId", freshRoomId)
                                transaction.update(queuePartnerRef, "matchedRoomId", freshRoomId)
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
            firestore.collection("matchmaking_queue").document(userId).delete()
        } catch (_: Exception) {}
    }
}
