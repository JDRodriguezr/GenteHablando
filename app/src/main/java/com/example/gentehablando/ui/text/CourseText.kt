package com.example.gentehablando.ui.text

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.example.gentehablando.R
import com.example.gentehablando.domain.model.Course

data class QuestionTextResources(
    @StringRes val question: Int,
    @ArrayRes val options: Int,
    @StringRes val explanation: Int
)

data class CourseTextResources(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val content: Int,
    val questions: List<QuestionTextResources>
)

fun Course.textResources(): CourseTextResources = when (id) {
    "course_1" -> CourseTextResources( // i18n: technical Stable course ID, not its display title.
        R.string.course_1_title, R.string.course_1_description, R.string.course_1_content,
        listOf(
            QuestionTextResources(R.string.course_1_q1_question, R.array.course_1_q1_options, R.string.course_1_q1_explanation),
            QuestionTextResources(R.string.course_1_q2_question, R.array.course_1_q2_options, R.string.course_1_q2_explanation),
            QuestionTextResources(R.string.course_1_q3_question, R.array.course_1_q3_options, R.string.course_1_q3_explanation),
            QuestionTextResources(R.string.course_1_q4_question, R.array.course_1_q4_options, R.string.course_1_q4_explanation),
            QuestionTextResources(R.string.course_1_q5_question, R.array.course_1_q5_options, R.string.course_1_q5_explanation),
            QuestionTextResources(R.string.course_1_q6_question, R.array.course_1_q6_options, R.string.course_1_q6_explanation),
            QuestionTextResources(R.string.course_1_q7_question, R.array.course_1_q7_options, R.string.course_1_q7_explanation),
            QuestionTextResources(R.string.course_1_q8_question, R.array.course_1_q8_options, R.string.course_1_q8_explanation),
            QuestionTextResources(R.string.course_1_q9_question, R.array.course_1_q9_options, R.string.course_1_q9_explanation),
            QuestionTextResources(R.string.course_1_q10_question, R.array.course_1_q10_options, R.string.course_1_q10_explanation)
        )
    )
    "course_2" -> CourseTextResources( // i18n: technical Stable course ID, not its display title.
        R.string.course_2_title, R.string.course_2_description, R.string.course_2_content,
        listOf(
            QuestionTextResources(R.string.course_2_q1_question, R.array.course_2_q1_options, R.string.course_2_q1_explanation),
            QuestionTextResources(R.string.course_2_q2_question, R.array.course_2_q2_options, R.string.course_2_q2_explanation),
            QuestionTextResources(R.string.course_2_q3_question, R.array.course_2_q3_options, R.string.course_2_q3_explanation),
            QuestionTextResources(R.string.course_2_q4_question, R.array.course_2_q4_options, R.string.course_2_q4_explanation),
            QuestionTextResources(R.string.course_2_q5_question, R.array.course_2_q5_options, R.string.course_2_q5_explanation),
            QuestionTextResources(R.string.course_2_q6_question, R.array.course_2_q6_options, R.string.course_2_q6_explanation),
            QuestionTextResources(R.string.course_2_q7_question, R.array.course_2_q7_options, R.string.course_2_q7_explanation),
            QuestionTextResources(R.string.course_2_q8_question, R.array.course_2_q8_options, R.string.course_2_q8_explanation),
            QuestionTextResources(R.string.course_2_q9_question, R.array.course_2_q9_options, R.string.course_2_q9_explanation),
            QuestionTextResources(R.string.course_2_q10_question, R.array.course_2_q10_options, R.string.course_2_q10_explanation)
        )
    )
    "course_3" -> CourseTextResources( // i18n: technical Stable course ID, not its display title.
        R.string.course_3_title, R.string.course_3_description, R.string.course_3_content,
        listOf(
            QuestionTextResources(R.string.course_3_q1_question, R.array.course_3_q1_options, R.string.course_3_q1_explanation),
            QuestionTextResources(R.string.course_3_q2_question, R.array.course_3_q2_options, R.string.course_3_q2_explanation),
            QuestionTextResources(R.string.course_3_q3_question, R.array.course_3_q3_options, R.string.course_3_q3_explanation),
            QuestionTextResources(R.string.course_3_q4_question, R.array.course_3_q4_options, R.string.course_3_q4_explanation),
            QuestionTextResources(R.string.course_3_q5_question, R.array.course_3_q5_options, R.string.course_3_q5_explanation),
            QuestionTextResources(R.string.course_3_q6_question, R.array.course_3_q6_options, R.string.course_3_q6_explanation),
            QuestionTextResources(R.string.course_3_q7_question, R.array.course_3_q7_options, R.string.course_3_q7_explanation),
            QuestionTextResources(R.string.course_3_q8_question, R.array.course_3_q8_options, R.string.course_3_q8_explanation),
            QuestionTextResources(R.string.course_3_q9_question, R.array.course_3_q9_options, R.string.course_3_q9_explanation),
            QuestionTextResources(R.string.course_3_q10_question, R.array.course_3_q10_options, R.string.course_3_q10_explanation)
        )
    )
    else -> error("Unknown bundled course: $id") // i18n: technical Programmer invariant; the screen rejects missing courses before rendering.
}
