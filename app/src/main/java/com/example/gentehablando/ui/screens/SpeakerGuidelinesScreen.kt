package com.example.gentehablando.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.gentehablando.R

@Composable
fun SpeakerGuidelinesScreen(
    modifier: Modifier = Modifier,
    onProceedToMatchmaking: () -> Unit
) {
    // Estado del temporizador: arranca en 3 segundos
    var secondsLeft by remember { mutableIntStateOf(3) }

    // LaunchedEffect corre de fondo de forma segura en Compose
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L) // Espera 1 segundo
            secondsLeft--
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.before_speaking),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.legacy_speaker_intro),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            GuidelineItem(
                number = stringResource(R.string.number, 1),
                title = stringResource(R.string.speaker_guideline_1_title),
                body = stringResource(R.string.legacy_speaker_guideline_1)
            )

            GuidelineItem(
                number = stringResource(R.string.number, 2),
                title = stringResource(R.string.speaker_guideline_2_title),
                body = stringResource(R.string.legacy_speaker_guideline_2)
            )

            GuidelineItem(
                number = stringResource(R.string.number, 3),
                title = stringResource(R.string.speaker_guideline_3_title),
                body = stringResource(R.string.legacy_speaker_guideline_3)
            )

            GuidelineItem(
                number = stringResource(R.string.number, 4),
                title = stringResource(R.string.speaker_guideline_4_title),
                body = stringResource(R.string.speaker_guideline_4_body)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón condicionado por el temporizador
        val isReady = secondsLeft == 0

        Button(
            onClick = onProceedToMatchmaking,
            enabled = isReady,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (isReady) stringResource(R.string.guidelines_continue)
                else pluralStringResource(R.plurals.guidelines_countdown, secondsLeft, secondsLeft),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GuidelineItem(
    number: String,
    title: String,
    body: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = body,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}