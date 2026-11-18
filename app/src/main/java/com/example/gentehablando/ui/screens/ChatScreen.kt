package com.example.gentehablando.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentehablando.domain.model.ChatMessage
import com.example.gentehablando.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomId: String,
    partnerAlias: String,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    onLeaveChat: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isPartnerTyping by viewModel.isPartnerTyping.collectAsState()
    val isRoomActive by viewModel.isRoomActive.collectAsState()
    val isMuted by viewModel.isLocalAudioMuted.collectAsState()
    val isLocalSpeaking by viewModel.isLocalSpeaking.collectAsState()
    val isPartnerSpeaking by viewModel.isPartnerSpeaking.collectAsState()
    val isNoiseFilterOn by viewModel.isNoiseSuppressionEnabled.collectAsState()

    val showInactivityWarning by viewModel.showInactivityWarning.collectAsState()
    val inactivitySecondsLeft by viewModel.inactivitySecondsLeft.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showExitDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> 
            hasMicPermission = isGranted 
            viewModel.initRoom(roomId = roomId, partnerAlias = partnerAlias)
        }
    )

    // Al usar Unit, le decimos a Compose: "ejecuta esto SÍ O SÍ la primera vez que se dibuja la pantalla".
    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        } else {
            // Ya tiene permisos concedidos
            viewModel.initRoom(roomId = roomId, partnerAlias = partnerAlias)
        }
    }

    // Interceptar el botón/gesto de "Atrás" del celular estando en la sala
    androidx.activity.compose.BackHandler {
        // En lugar de salir directo o devolverlo al menú (lo que rompería la sala),
        // forzamos al usuario a ver el cuadro de diálogo de salida formal.
        showExitDialog = true
    }

    val listState = rememberLazyListState()

    // Auto-scroll al recibir o enviar mensaje
    LaunchedEffect(messages.size, isPartnerTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Cuadro de diálogo de confirmación para salir
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(text = "Salir de la conversación") },
            text = { Text(text = "¿Deseas finalizar y volver al menú principal?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.leaveRoom()
                        onLeaveChat()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sí, salir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Cuadro de diálogo de inactividad
    if (showInactivityWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.extendSession() },
            title = { Text(text = "¿Sigues ahí?") },
            text = { Text(text = "La sala se cerrará por inactividad en $inactivitySecondsLeft segundos.") },
            confirmButton = {
                Button(onClick = { viewModel.extendSession() }) {
                    Text("Sí, seguir conversando")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.leaveRoom()
                        onLeaveChat()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Finalizar ahora")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // 1. Barra superior
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = partnerAlias, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        if (isPartnerSpeaking && isRoomActive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF4CAF50), shape = androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    }
                    Text(
                        text = when {
                            !isRoomActive -> "Sesión finalizada por el compañero"
                            isPartnerTyping -> "Escribiendo..."
                            else -> "En línea"
                        },
                        fontSize = 12.sp,
                        color = when {
                            !isRoomActive -> MaterialTheme.colorScheme.error
                            isPartnerTyping -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            },
            actions = {
                OutlinedButton(
                    onClick = { showExitDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isRoomActive) "Finalizar" else "Salir", fontSize = 13.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        )

        // 2. Interfaz de Chat de Voz (Barra de Control Mute/Unmute)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMuted) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMuted) "🎙️ Micrófono silenciado" else "🎤 Canal de voz activo",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    if (!isMuted && isLocalSpeaking) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF4CAF50), shape = androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.toggleMute() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isMuted) "Activar Mic" else "Mutear",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sub-fila para controles secundarios (Filtro de Ruido)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Aislamiento de voz con IA",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Switch(
                    checked = isNoiseFilterOn,
                    onCheckedChange = { viewModel.toggleNoiseSuppression() },
                    modifier = Modifier.height(24.dp)
                )
            }
        }

        // 3. Aviso Informativo cuando el compañero finaliza la sesión (Sin botones, solo informativo)
        if (!isRoomActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "⚠️ Tu compañero ha finalizado la sesión. Puedes revisar los mensajes anteriores. Estás solo en la sala.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 4. Feed de mensajes
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(messages.size) { index ->
                val currentMessage = messages[index]
                val nextMessage = messages.getOrNull(index + 1)

                val isNextFromSameSender = nextMessage != null && nextMessage.isFromMe == currentMessage.isFromMe
                val isSameMinute = nextMessage != null && nextMessage.timestamp == currentMessage.timestamp
                val isGroupedWithNext = isNextFromSameSender && isSameMinute
                val showTimestamp = !isGroupedWithNext

                MessageBubble(
                    message = currentMessage,
                    showTimestamp = showTimestamp,
                    isGroupedWithNext = isGroupedWithNext,
                    isRoomActive = isRoomActive
                )

                Spacer(modifier = Modifier.height(if (isGroupedWithNext) 3.dp else 10.dp))
            }

            if (isPartnerTyping && isRoomActive) {
                item {
                    Text(
                        text = "$partnerAlias está escribiendo...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }

        // 5. Barra inferior para escribir mensaje
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    viewModel.onInputTextChanged(it)
                },
                placeholder = {
                    Text(if (isRoomActive) "Escribe un mensaje..." else "La conversación ha finalizado")
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                enabled = isRoomActive,
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                shape = RoundedCornerShape(24.dp),
                enabled = isRoomActive && inputText.isNotBlank()
            ) {
                Text("Enviar")
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    showTimestamp: Boolean,
    isGroupedWithNext: Boolean,
    isRoomActive: Boolean
) {
    // Alineación derecha para mis mensajes, izquierda para la otra persona
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start

    // Colores diferenciados para los mensajes de cada usuario
    val bubbleColor = when {
        !isRoomActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) // Tono apagado cuando finaliza
        message.isFromMe -> MaterialTheme.colorScheme.primary // Mi mensaje: Color primario
        else -> MaterialTheme.colorScheme.secondaryContainer // Mensaje del compañero: Color secundario diferenciado
    }

    val textColor = when {
        !isRoomActive -> MaterialTheme.colorScheme.onSurfaceVariant
        message.isFromMe -> Color.White
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val shape = if (message.isFromMe) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = if (isGroupedWithNext) 4.dp else 16.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isGroupedWithNext) 4.dp else 16.dp,
            bottomEnd = 16.dp
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(color = bubbleColor, shape = shape)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontSize = 15.sp
            )
        }

        if (showTimestamp) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message.timestamp,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
