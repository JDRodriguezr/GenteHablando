package com.example.gentehablando.domain.repository

import com.example.gentehablando.domain.model.Course
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    /** Obtiene la lista de cursos disponibles */
    fun getCourses(): List<Course>
    
    /** Obtiene un curso específico por su ID */
    fun getCourseById(courseId: String): Course?

    /** Observa un mapa con el ID del curso y la calificación más alta obtenida (4 o 5) */
    fun observeCompletedCourses(): Flow<Map<String, Int>>

    /** Guarda el puntaje si es mayor al que ya tenía (Localmente y en Firebase) */
    suspend fun markCourseCompleted(courseId: String, score: Int)

    /** Sincroniza el progreso local (invitado) con Firebase tras un login exitoso */
    suspend fun syncLocalProgressToRemote()

    fun getQuizCooldown(courseId: String): Long

    fun setQuizCooldown(courseId: String, cooldownEndTimeMs: Long)
}
