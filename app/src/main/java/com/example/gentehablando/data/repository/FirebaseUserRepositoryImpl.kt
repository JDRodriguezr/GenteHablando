package com.example.gentehablando.data.repository

import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.domain.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseUserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override suspend fun submitFeedback(
        roomId: String,
        currentUserId: String,
        rating: Int,
        isReport: Boolean,
        issueType: String?,
        issueText: String?
    ): Resource<Unit> {
        return try {
            // 1. Recuperamos la sala para saber con quién estaba hablando
            val roomDoc = firestore.collection("rooms").document(roomId).get().await()
            if (!roomDoc.exists()) return Resource.Error("Sala no encontrada")

            val speakerId = roomDoc.getString("speakerId") ?: ""
            val listenerId = roomDoc.getString("listenerId") ?: ""

            // Calculamos quién era la pareja en esa sala
            val targetId = when (currentUserId) {
                speakerId -> listenerId
                listenerId -> speakerId
                else -> return Resource.Error("El usuario no pertenece a la sala")
            }
            if (targetId.isBlank()) return Resource.Error("ID de compañero inválido")

            // Si es un reporte, marcamos la sala como "Reportada" para que no se borre en las limpiezas futuras
            if (isReport) {
                firestore.collection("rooms").document(roomId).update("hasBeenReported", true).await()
            }

            // 2. Guardamos el registro individual de Feedback
            val feedbackData = hashMapOf(
                "roomId" to roomId,
                "raterId" to currentUserId,
                "targetId" to targetId,
                "rating" to rating,
                "isReport" to isReport,
                "issueType" to issueType,
                "issueText" to issueText,
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection("feedbacks").add(feedbackData).await()

            // 3. Transacción atómica para recalcular la reputación global de la pareja
            val userRef = firestore.collection("users").document(targetId)
            firestore.runTransaction { transaction ->
                val userSnapshot = transaction.get(userRef)
                
                val currentScore = userSnapshot.getDouble("reputationScore") ?: 0.0
                val totalRatings = userSnapshot.getLong("totalRatings") ?: 0L
                val currentReports = userSnapshot.getLong("reportsCount") ?: 0L

                val updates = hashMapOf<String, Any>()

                // Si fue un reporte, sumamos al contador histórico y a la lista temporal
                if (isReport) {
                    updates["reportsCount"] = currentReports + 1
                    val newReport = mapOf(
                        "roomId" to roomId,
                        "timestamp" to System.currentTimeMillis()
                    )
                    updates["recentReports"] = FieldValue.arrayUnion(newReport)
                }

                // Si dejó una calificación (1 al 4), recalculamos el promedio
                if (rating > 0) {
                    val newTotal = totalRatings + 1
                    val newScore = ((currentScore * totalRatings) + rating) / newTotal
                    updates["reputationScore"] = newScore
                    updates["totalRatings"] = newTotal
                }

                transaction.set(userRef, updates, SetOptions.merge())
            }.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al enviar el feedback al servidor")
        }
    }

    override suspend fun submitAppeal(
        userId: String,
        explanation: String,
        reportedRoomIds: List<String>
    ): Resource<Unit> {
        return try {
            val appealData = hashMapOf(
                "userId" to userId,
                "explanation" to explanation,
                "reportedRoomIds" to reportedRoomIds,
                "status" to "PENDING",
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection("appeals").add(appealData).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al enviar apelación")
        }
    }

    override fun observeUserProfile(userId: String): kotlinx.coroutines.flow.Flow<com.example.gentehablando.domain.model.UserProfile> = kotlinx.coroutines.flow.callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val score = snapshot.getDouble("reputationScore") ?: 0.0
                    val ratings = snapshot.getLong("totalRatings") ?: 0L
                    val reports = snapshot.getLong("reportsCount") ?: 0L
                    
                    val rawRecent = snapshot.get("recentReports") as? List<Map<String, Any>>
                    val recentList = rawRecent?.mapNotNull {
                        val rId = it["roomId"] as? String ?: return@mapNotNull null
                        val ts = it["timestamp"] as? Long ?: return@mapNotNull null
                        com.example.gentehablando.domain.model.ReportRecord(rId, ts)
                    } ?: emptyList()

                    trySend(com.example.gentehablando.domain.model.UserProfile(userId, score, ratings, reports, recentList))
                } else {
                    // Perfil nuevo
                    trySend(com.example.gentehablando.domain.model.UserProfile(userId, 0.0, 0L, 0L, emptyList()))
                }
            }
        awaitClose { listener.remove() }
    }
}
