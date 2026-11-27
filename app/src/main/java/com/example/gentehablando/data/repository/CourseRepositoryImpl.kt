package com.example.gentehablando.data.repository

import android.content.Context
import com.example.gentehablando.domain.model.Course
import com.example.gentehablando.domain.model.QuizQuestion
import com.example.gentehablando.domain.repository.CourseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CourseRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CourseRepository {

    private val prefs = context.getSharedPreferences("app_courses_prefs", Context.MODE_PRIVATE) // i18n: technical Course progress storage key; preserves existing local/Firestore records.

    private val courses = listOf(
        Course(id = "course_1", questionsPool = List(10) { QuizQuestion(id = it + 1, correctOptionIndex = 0) }), // i18n: technical Stable course ID used by existing progress storage.
        Course(id = "course_2", questionsPool = List(10) { QuizQuestion(id = it + 1, correctOptionIndex = 0) }), // i18n: technical Stable course ID used by existing progress storage.
        Course(id = "course_3", questionsPool = List(10) { QuizQuestion(id = it + 1, correctOptionIndex = 0) }), // i18n: technical Stable course ID used by existing progress storage.
    )

    override fun getCourses(): List<Course> = courses

    override fun getCourseById(courseId: String): Course? = courses.find { it.id == courseId }

    override fun observeCompletedCourses(): Flow<Map<String, Int>> = callbackFlow {
        // Leemos primero el caché local. Estructura: "score_courseId" -> Int (puntaje)
        val currentScores = mutableMapOf<String, Int>()
        courses.forEach { course ->
            val score = prefs.getInt("score_${course.id}", -1) // i18n: technical Course progress storage key; preserves existing local/Firestore records.
            if (score != -1) currentScores[course.id] = score
        }
        trySend(currentScores)

        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            val listener = firestore.collection("users").document(user.uid) // i18n: technical Course progress storage key; preserves existing local/Firestore records.
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        @Suppress("UNCHECKED_CAST") // i18n: technical Compiler suppression identifier; not display content.
                        val remoteScores = snapshot.get("courseScores") as? Map<String, Long> ?: emptyMap() // i18n: technical Course progress storage key; preserves existing local/Firestore records.

                        var changed = false
                        remoteScores.forEach { (cId, rScore) ->
                            val currentLocalScore = currentScores[cId] ?: -1
                            if (rScore.toInt() > currentLocalScore) {
                                currentScores[cId] = rScore.toInt()
                                prefs.edit().putInt("score_$cId", rScore.toInt()).apply() // i18n: technical Course progress storage key; preserves existing local/Firestore records.
                                changed = true
                            }
                        }

                        if (changed) trySend(currentScores)
                    }
                }
            awaitClose { listener.remove() }
        } else {
            awaitClose { }
        }
    }

    override suspend fun markCourseCompleted(courseId: String, score: Int) {
        val currentScore = prefs.getInt("score_$courseId", -1) // i18n: technical Course progress storage key; preserves existing local/Firestore records.
        if (score > currentScore) {
            prefs.edit().putInt("score_$courseId", score).apply() // i18n: technical Course progress storage key; preserves existing local/Firestore records.
        } else return // Si saco peor puntaje que antes, no lo sobreescribimos

        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            try {
                firestore.collection("users").document(user.uid) // i18n: technical Course progress storage key; preserves existing local/Firestore records.
                    .set(mapOf("courseScores" to mapOf(courseId to score)), SetOptions.merge()) // i18n: technical Course progress storage key; preserves existing local/Firestore records.
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun syncLocalProgressToRemote() {
        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            val currentScores = mutableMapOf<String, Int>()
            courses.forEach { course ->
                val score = prefs.getInt("score_${course.id}", -1) // i18n: technical Course progress storage key; preserves existing local/Firestore records.
                if (score != -1) currentScores[course.id] = score
            }
            if (currentScores.isNotEmpty()) {
                try {
                    firestore.collection("users").document(user.uid) // i18n: technical Course progress storage key; preserves existing local/Firestore records.
                        .set(mapOf("courseScores" to currentScores), SetOptions.merge()) // i18n: technical Course progress storage key; preserves existing local/Firestore records.
                        .await()
                } catch (_: Exception) {}
            }
        }
    }

    override fun getQuizCooldown(courseId: String): Long {
        return prefs.getLong("cooldown_$courseId", 0L) // i18n: technical Course progress storage key; preserves existing local/Firestore records.
    }

    override fun setQuizCooldown(courseId: String, cooldownEndTimeMs: Long) {
        prefs.edit().putLong("cooldown_$courseId", cooldownEndTimeMs).apply() // i18n: technical Course progress storage key; preserves existing local/Firestore records.
    }
}
