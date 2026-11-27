package com.example.gentehablando.domain.model

data class QuizQuestion(
    val id: Int,
    val correctOptionIndex: Int,
    val optionCount: Int = 4
)

data class Course(
    val id: String,
    val questionsPool: List<QuizQuestion>
)
