package com.example.gentehablando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gentehablando.domain.model.Course
import com.example.gentehablando.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    val courses: List<Course> = courseRepository.getCourses()

    private val _completedCourses = MutableStateFlow<Map<String, Int>>(emptyMap())
    val completedCourses: StateFlow<Map<String, Int>> = _completedCourses.asStateFlow()

    init {
        viewModelScope.launch {
            courseRepository.observeCompletedCourses().collect { completedMap ->
                _completedCourses.value = completedMap
            }
        }
    }

    fun markCourseCompleted(courseId: String, score: Int) {
        viewModelScope.launch {
            val currentScore = _completedCourses.value[courseId] ?: -1
            if (score > currentScore) {
                courseRepository.markCourseCompleted(courseId, score)
                val updatedMap = _completedCourses.value.toMutableMap()
                updatedMap[courseId] = score
                _completedCourses.value = updatedMap
            }
        }
    }

    fun getCooldownEndTime(courseId: String): Long {
        return courseRepository.getQuizCooldown(courseId)
    }

    fun setQuizCooldown(courseId: String, timeMs: Long) {
        courseRepository.setQuizCooldown(courseId, timeMs)
    }
}
