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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentehablando.model.UserRole
import kotlinx.coroutines.delay

@Composable
fun RoleGuidelinesScreen(
    role: UserRole,
    modifier: Modifier = Modifier,
    onProceedToMatchmaking: () -> Unit
) {
    // Usamos saveable para que el estado del temporizador sobreviva a las rotaciones
    var secondsLeft by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(10) }

    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }

    val isSpeaker = role == UserRole.SPEAKER

    val title = if (isSpeaker) "Antes de hablar..." else "Antes de escuchar..."
    val subtitle = if (isSpeaker) {
        "Al otro lado hay una persona voluntaria dispuesta a conversar. Ten en cuenta estas pautas:"
    } else {
        "Al otro lado hay una persona buscando un espacio seguro para conversar. Ten en cuenta estas pautas:"
    }

    val guidelines = if (isSpeaker) {
        listOf(
            "1" to ("No es terapia profesional" to "La persona viene a acompañarte, no a resolver tus problemas ni a dar diagnósticos. El alivio está en la conexión humana."),
            "2" to ("Toda emoción es bienvenida, la agresión no" to "Puedes sentir rabia, tristeza o frustración. Vuelca tu sentir sobre tu historia, sin atacar a quien te escucha."),
            "3" to ("A tu propio ritmo" to "No tienes que comprimir todo en un solo mensaje. Tómate pausas para respirar y expresarte como necesites."),
            "4" to ("Cuidado mutuo" to "Si sientes que la charla no te hace bien o cruza límites, tienes derecho a terminar la sesión con respeto en cualquier momento.")
        )
    } else {
        listOf(
            "1" to ("Escucha activa y sin juzgar" to "Tu presencia y atención libre de prejuicios es el mayor acompañamiento. Permite que la conversación fluya."),
            "2" to ("Evita dar consejos no solicitados" to "El objetivo no es solucionar la vida de la otra persona, sino brindarle un espacio empático y libre de presión."),
            "3" to ("Respeto e intimidad anónima" to "Todo lo compartido es confidencial. Mantén la amabilidad y respeta el anonimato de la sesión."),
            "4" to ("Cuida tu propio bienestar" to "Si la conversación sobrepasa tus límites emocionales o resulta incómoda, puedes terminarla con amabilidad.")
        )
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
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            guidelines.forEach { (number, pair) ->
                val (itemTitle, itemBody) = pair
                GuidelineItem(
                    number = number,
                    title = itemTitle,
                    body = itemBody
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                text = if (isReady) "Entendido, buscar conversación" else "Lee con calma (${secondsLeft}s)...",
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
