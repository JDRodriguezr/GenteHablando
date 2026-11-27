package com.example.gentehablando.ui.screens

import com.example.gentehablando.domain.model.QuizQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizQuestionStateTest {
    @Test
    fun gradesOriginalOptionIdentityAfterShuffling() {
        val question = QuizQuestion(id = 7, correctOptionIndex = 0)
        val session = QuizQuestionState(question, listOf(2, 0, 3, 1))

        session.optionIndices.forEachIndexed { displayedIndex, originalIndex ->
            assertEquals(originalIndex == question.correctOptionIndex, session.copy(selectedIndex = displayedIndex).isCorrect)
        }
    }

    @Test
    fun correctAnswerDoesNotDependOnFirstDisplayedOptionOrLabel() {
        val question = QuizQuestion(id = 2, correctOptionIndex = 3)
        val firstOrder = QuizQuestionState(question, listOf(0, 1, 2, 3), selectedIndex = 3)
        val secondOrder = QuizQuestionState(question, listOf(3, 2, 0, 1), selectedIndex = 0)

        assertTrue(firstOrder.isCorrect)
        assertTrue(secondOrder.isCorrect)
        assertFalse(secondOrder.copy(selectedIndex = 3).isCorrect)
    }

    @Test
    fun unansweredQuestionIsNotCorrect() {
        val question = QuizQuestion(id = 1, correctOptionIndex = 0)
        assertFalse(QuizQuestionState(question, listOf(0, 1, 2, 3)).isCorrect)
    }
}
