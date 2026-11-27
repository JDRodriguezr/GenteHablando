package com.example.gentehablando.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentehablando.viewmodel.CourseViewModel
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.example.gentehablando.R
import com.example.gentehablando.domain.model.QuizQuestion
import com.example.gentehablando.ui.text.textResources

data class QuizQuestionState(
    val question: QuizQuestion,
    val optionIndices: List<Int>,
    val selectedIndex: Int = -1
) {
    val isCorrect: Boolean
        get() = optionIndices.getOrNull(selectedIndex) == question.correctOptionIndex
}

private enum class CourseMode { READING, QUIZ, COOLDOWN, PASSED, QUIZ_RESULTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: String,
    viewModel: CourseViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    val course = viewModel.courses.find { it.id == courseId }
    val completedCourses by viewModel.completedCourses.collectAsState()
    val isCompleted = completedCourses.contains(courseId)

    if (course == null) {
        onNavigateBack()
        return
    }
    val textResources = course.textResources()

    // --- LÓGICA DE COOLDOWN Y ESTADO DEL QUIZ ---
    val cooldownEndTime = remember { mutableLongStateOf(viewModel.getCooldownEndTime(courseId)) }
    var timeRemaining by remember { mutableLongStateOf(0L) }
    
    // El modo puede ser: "READING", "QUIZ", "COOLDOWN", "PASSED"
    var currentMode by remember { 
        mutableStateOf(
            when {
                cooldownEndTime.longValue > System.currentTimeMillis() -> CourseMode.COOLDOWN
                isCompleted -> CourseMode.PASSED
                else -> CourseMode.READING
            }
        ) 
    }

    // Timer reactivo para el cooldown
    LaunchedEffect(cooldownEndTime.longValue) {
        while (cooldownEndTime.longValue > System.currentTimeMillis()) {
            timeRemaining = cooldownEndTime.longValue - System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
        if (currentMode == CourseMode.COOLDOWN && timeRemaining <= 0) {
            currentMode = CourseMode.READING // Se acabó el castigo
        }
    }

    // --- LÓGICA DE LAS PREGUNTAS ---
    var quizSession by remember { mutableStateOf<List<QuizQuestionState>?>(null) }
    var showQuizError by remember { mutableStateOf(false) }

    fun generateQuiz() {
        val selectedQuestions = course.questionsPool.shuffled().take(5)
        val session = selectedQuestions.map { q ->
            QuizQuestionState(q, (0 until q.optionCount).shuffled())
        }
        quizSession = session
        currentMode = CourseMode.QUIZ
        showQuizError = false
    }

    // --- UI PRINCIPAL ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { }, // Título vacío para que no persiga en el scroll y no se repita
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Título
            Text(
                text = stringResource(textResources.title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (currentMode) {
                CourseMode.PASSED -> {
                    // MODO: CURSO COMPLETADO
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.completed), tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.lesson_mastered), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.lesson_passed), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                                Text(stringResource(R.string.back_to_academy))
                            }
                        }
                    }
                }

                CourseMode.COOLDOWN -> {
                    // MODO: CASTIGO 10 MINUTOS
                    val mins = (timeRemaining / 1000) / 60
                    val secs = (timeRemaining / 1000) % 60
                    val timeString = stringResource(R.string.countdown_time, mins, secs)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.review_lesson), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.review_lesson_instructions), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(timeString, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(stringResource(textResources.content), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp)
                }

                CourseMode.READING -> {
                    // MODO: LECTURA DEL CONTENIDO
                    Text(stringResource(textResources.content), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { generateQuiz() },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.start_quiz), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                CourseMode.QUIZ -> {
                    // MODO: EVALUACIÓN (5 Preguntas)
                    Text(pluralStringResource(R.plurals.quiz_instructions, 5, 5), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(24.dp))

                    quizSession?.forEachIndexed { questionIndex, qState ->
                        val questionResources = textResources.questions[qState.question.id - 1]
                        val optionTexts = stringArrayResource(questionResources.options)
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.numbered_question, questionIndex + 1, stringResource(questionResources.question)), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                qState.optionIndices.forEachIndexed { optIndex, originalIndex ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { 
                                            // Actualizamos el estado interno
                                            val newList = quizSession!!.toMutableList()
                                            newList[questionIndex] = qState.copy(selectedIndex = optIndex)
                                            quizSession = newList
                                        }.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = qState.selectedIndex == optIndex,
                                            onClick = {
                                                val newList = quizSession!!.toMutableList()
                                                newList[questionIndex] = qState.copy(selectedIndex = optIndex)
                                                quizSession = newList
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(optionTexts[originalIndex], fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    val allAnswered = quizSession?.all { it.selectedIndex != -1 } == true

                    Button(
                        onClick = { currentMode = CourseMode.QUIZ_RESULTS },
                        enabled = allAnswered,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.submit_answers), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                CourseMode.QUIZ_RESULTS -> {
                    val correctCount = quizSession?.count { it.isCorrect } ?: 0
                    val passed = correctCount >= 4
                    
                    Text(
                        text = stringResource(R.string.quiz_result, correctCount, 5),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (passed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    quizSession?.forEachIndexed { questionIndex, qState ->
                        val isCorrect = qState.isCorrect
                        val questionResources = textResources.questions[qState.question.id - 1]
                        val optionTexts = stringArrayResource(questionResources.options)
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.numbered_question, questionIndex + 1, stringResource(questionResources.question)), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Tu respuesta
                                Text(
                                    text = stringResource(R.string.your_answer, optionTexts[qState.optionIndices[qState.selectedIndex]]),
                                    color = if (isCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                // Si está mal, mostramos la lógica sin dar la respuesta explícitamente
                                if (!isCorrect) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = stringResource(R.string.hint_label),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = stringResource(questionResources.explanation),
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (passed) {
                        LaunchedEffect(Unit) {
                            viewModel.markCourseCompleted(course.id, correctCount)
                        }
                        
                        Button(
                            onClick = {
                                if (correctCount == 4) {
                                    // Puede reintentar enseguida si quiere el 5/5
                                    generateQuiz()
                                } else {
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(if (correctCount == 4) R.string.retry_perfect else R.string.back_to_academy), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        if (correctCount == 4) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Text(stringResource(R.string.accept_four))
                            }
                        }
                    } else {
                        // Castigo aplicado instantáneamente si saca menos de 4
                        val penaltyTime = System.currentTimeMillis() + (10 * 60 * 1000L)
                        LaunchedEffect(Unit) {
                            viewModel.setQuizCooldown(course.id, penaltyTime)
                            cooldownEndTime.longValue = penaltyTime
                        }
                        
                        Button(
                            onClick = {
                                currentMode = CourseMode.COOLDOWN
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(pluralStringResource(R.plurals.accept_cooldown, 10, 10), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Interceptar el botón "Atrás" físico/gestual para que, si está en resultados y falló, se aplique el modo cooldown antes de salir
    androidx.activity.compose.BackHandler(enabled = currentMode == CourseMode.QUIZ_RESULTS && (quizSession?.count { it.isCorrect } ?: 0) < 4) {
        currentMode = CourseMode.COOLDOWN
    }
}
