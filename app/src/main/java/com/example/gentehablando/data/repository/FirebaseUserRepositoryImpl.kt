package com.example.gentehablando.data.repository

import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.domain.model.AppError
import com.example.gentehablando.domain.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

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
            val roomDoc = firestore.collection("rooms").document(roomId).get().await() // i18n: technical Firestore collection/field key; preserves the backend schema.
            if (!roomDoc.exists()) return Resource.Error(AppError.RoomNotFound)

            val speakerId = roomDoc.getString("speakerId") ?: "" // i18n: technical Firestore collection/field key; preserves the backend schema.
            val listenerId = roomDoc.getString("listenerId") ?: "" // i18n: technical Firestore collection/field key; preserves the backend schema.

            // Calculamos quién era la pareja en esa sala
            val targetId = when (currentUserId) {
                speakerId -> listenerId
                listenerId -> speakerId
                else -> return Resource.Error(AppError.NotRoomMember)
            }
            if (targetId.isBlank()) return Resource.Error(AppError.InvalidPartner)

            // Si es un reporte, marcamos la sala como "Reportada" para que no se borre en las limpiezas futuras
            if (isReport) {
                firestore.collection("rooms").document(roomId).update("hasBeenReported", true).await() // i18n: technical Firestore collection/field key; preserves the backend schema.
            }

            // 2. Guardamos el registro individual de Feedback
            val feedbackData = hashMapOf(
                "roomId" to roomId, // i18n: technical Firestore collection/field key; preserves the backend schema.
                "raterId" to currentUserId, // i18n: technical Firestore collection/field key; preserves the backend schema.
                "targetId" to targetId, // i18n: technical Firestore collection/field key; preserves the backend schema.
                "rating" to rating, // i18n: technical Firestore collection/field key; preserves the backend schema.
                "isReport" to isReport, // i18n: technical Firestore collection/field key; preserves the backend schema.
                "issueType" to issueType, // i18n: technical Firestore collection/field key; preserves the backend schema.
                "issueText" to issueText, // i18n: technical Firestore collection/field key; preserves the backend schema.
                "timestamp" to FieldValue.serverTimestamp() // i18n: technical Firestore collection/field key; preserves the backend schema.
            )
            firestore.collection("feedbacks").add(feedbackData).await() // i18n: technical Firestore collection/field key; preserves the backend schema.

            // 3. Transacción atómica para recalcular la reputación global de la pareja
            val userRef = firestore.collection("users").document(targetId) // i18n: technical Firestore collection/field key; preserves the backend schema.
            firestore.runTransaction { transaction ->
                val userSnapshot = transaction.get(userRef)
                
                val currentScore = userSnapshot.getDouble("reputationScore") ?: 0.0 // i18n: technical Firestore collection/field key; preserves the backend schema.
                val totalRatings = userSnapshot.getLong("totalRatings") ?: 0L // i18n: technical Firestore collection/field key; preserves the backend schema.
                val currentReports = userSnapshot.getLong("reportsCount") ?: 0L // i18n: technical Firestore collection/field key; preserves the backend schema.

                val updates = hashMapOf<String, Any>()

                // Si fue un reporte, sumamos al contador histórico y a la lista temporal
                if (isReport) {
                    updates["reportsCount"] = currentReports + 1 // i18n: technical Firestore collection/field key; preserves the backend schema.
                    val newReport = mapOf(
                        "roomId" to roomId, // i18n: technical Firestore collection/field key; preserves the backend schema.
                        "timestamp" to System.currentTimeMillis() // i18n: technical Firestore collection/field key; preserves the backend schema.
                    )
                    updates["recentReports"] = FieldValue.arrayUnion(newReport) // i18n: technical Firestore collection/field key; preserves the backend schema.
                }

                // Si dejó una calificación (1 al 4), recalculamos el promedio
                if (rating > 0) {
                    val newTotal = totalRatings + 1
                    val newScore = ((currentScore * totalRatings) + rating) / newTotal
                    updates["reputationScore"] = newScore // i18n: technical Firestore collection/field key; preserves the backend schema.
                    updates["totalRatings"] = newTotal // i18n: technical Firestore collection/field key; preserves the backend schema.
                }

                transaction.set(userRef, updates, SetOptions.merge())
            }.await()

            // 4. Limpieza automática de la sala (Anti-Zombies y ahorro de Base de Datos)
            // Si el feedback fue enviado y NO hubo reporte, podemos borrar la sala de forma segura,
            // ya que el chat fue normal y no necesitamos evidencia.
            // (Si hubo reporte, la sala se deja intacta para que soporte pueda leerla).
            if (!isReport && issueType == null) {
                try {
                    // Firebase no borra subcolecciones automáticamente, así que debemos borrar
                    // primero los mensajes si queremos limpiar bien el espacio.
                    val messagesSnapshot = firestore.collection("rooms").document(roomId) // i18n: technical Firestore collection/field key; preserves the backend schema.
                        .collection("messages").get().await() // i18n: technical Firestore collection/field key; preserves the backend schema.
                        
                    firestore.runTransaction { transaction ->
                        messagesSnapshot.documents.forEach { doc ->
                            transaction.delete(doc.reference)
                        }
                        transaction.delete(firestore.collection("rooms").document(roomId)) // i18n: technical Firestore collection/field key; preserves the backend schema.
                    }.await()
                } catch (e: Exception) {
                    // Si falla el borrado no bloqueamos el feedback exitoso
                    e.printStackTrace()
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(AppError.SendFeedback, e)
        }
    }

    override suspend fun submitAppeal(
        userId: String,
        explanation: String,
        reportedRoomIds: List<String>
    ): Resource<Unit> {
        return try {
            val appealData = hashMapOf(
                "userId" to userId, // i18n: technical Firestore collection/field key; preserves the backend schema.
                "explanation" to explanation, // i18n: technical Firestore collection/field key; preserves the backend schema.
                "reportedRoomIds" to reportedRoomIds, // i18n: technical Firestore collection/field key; preserves the backend schema.
                "status" to "PENDING", // i18n: technical Appeal status wire value; not its display label.
                "timestamp" to FieldValue.serverTimestamp() // i18n: technical Firestore collection/field key; preserves the backend schema.
            )
            firestore.collection("appeals").add(appealData).await() // i18n: technical Firestore collection/field key; preserves the backend schema.
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(AppError.SendAppeal, e)
        }
    }

    override fun observeUserProfile(userId: String): kotlinx.coroutines.flow.Flow<com.example.gentehablando.domain.model.UserProfile> = kotlinx.coroutines.flow.callbackFlow {
        val listener = firestore.collection("users").document(userId) // i18n: technical Firestore collection/field key; preserves the backend schema.
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val score = snapshot.getDouble("reputationScore") ?: 0.0 // i18n: technical Firestore collection/field key; preserves the backend schema.
                    val ratings = snapshot.getLong("totalRatings") ?: 0L // i18n: technical Firestore collection/field key; preserves the backend schema.
                    val reports = snapshot.getLong("reportsCount") ?: 0L // i18n: technical Firestore collection/field key; preserves the backend schema.
                    
                    val rawRecent = snapshot.get("recentReports") as? List<Map<String, Any>> // i18n: technical Firestore collection/field key; preserves the backend schema.
                    val recentList = rawRecent?.mapNotNull {
                        val rId = it["roomId"] as? String ?: return@mapNotNull null // i18n: technical Firestore collection/field key; preserves the backend schema.
                        val ts = it["timestamp"] as? Long ?: return@mapNotNull null // i18n: technical Firestore collection/field key; preserves the backend schema.
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
