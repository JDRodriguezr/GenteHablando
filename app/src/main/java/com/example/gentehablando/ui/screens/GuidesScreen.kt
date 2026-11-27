package com.example.gentehablando.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentehablando.domain.model.Course
import com.example.gentehablando.viewmodel.CourseViewModel
import androidx.compose.ui.res.stringResource
import com.example.gentehablando.R
import com.example.gentehablando.ui.text.textResources

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidesScreen(
    viewModel: CourseViewModel,
    modifier: Modifier = Modifier,
    onCourseClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val completedCourses by viewModel.completedCourses.collectAsState()
    val courses = viewModel.courses

    // Separamos los completados (que pasaron con 4 o 5) de los no completados para el orden
    val uncompletedList = courses.filter { !completedCourses.containsKey(it.id) || completedCourses[it.id]!! < 4 }
    val completedList = courses.filter { completedCourses.containsKey(it.id) && completedCourses[it.id]!! >= 4 }
    val sortedCourses = uncompletedList + completedList

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.academy_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.available_courses),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.courses_summary),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(sortedCourses.size) { index ->
                    val course = sortedCourses[index]
                    val score = completedCourses[course.id]
                    
                    CourseCard(
                        course = course,
                        score = score,
                        onClick = { onCourseClick(course.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun CourseCard(
    course: Course,
    score: Int?,
    onClick: () -> Unit
) {
    val isCompleted = score != null && score >= 4
    val isPerfect = score == 5
    val textResources = course.textResources()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        border = if (isCompleted) null else BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de estado a la izquierda
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPerfect) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = stringResource(R.string.perfect),
                        tint = Color(0xFFFFC107), // Dorado para el 5/5
                        modifier = Modifier.size(36.dp)
                    )
                } else if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.completed),
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = stringResource(R.string.start),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Textos descriptivos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(textResources.title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(textResources.description),
                    fontSize = 13.sp,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Indicador de Progreso (X/5)
            val scoreText = stringResource(R.string.score_fraction, score?.coerceAtLeast(0) ?: 0, 5)
            val scoreColor = when {
                isPerfect -> Color(0xFFFFC107) // Dorado TOC
                isCompleted -> Color(0xFF4CAF50) // Verde normal
                else -> MaterialTheme.colorScheme.secondary // Gris
            }

            Text(
                text = scoreText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
        }
    }
}
