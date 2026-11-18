package com.example.gentehablando.domain.model

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String // Explicación de la lógica sin decir explícitamente cuál era la correcta
)

data class Course(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val questionsPool: List<QuizQuestion>
)
