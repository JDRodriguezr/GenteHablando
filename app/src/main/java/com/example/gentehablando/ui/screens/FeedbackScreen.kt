package com.example.gentehablando.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.model.UserRole
import com.example.gentehablando.utils.AdManager
import android.app.Activity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.example.gentehablando.R
import com.example.gentehablando.ui.text.localizedMessage

@Composable
fun FeedbackScreen(
    partnerAlias: String,
    modifier: Modifier = Modifier,
    role: UserRole = UserRole.SPEAKER,
    roomId: String = "",
    viewModel: com.example.gentehablando.viewmodel.FeedbackViewModel,
    adManager: AdManager,
    onFinish: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    var rating by remember { mutableIntStateOf(0) }
    val feedbackState by viewModel.feedbackState.collectAsState()

    // Control de selección de problemas técnicos
    var showIssueDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var issueTextInput by remember { androidx.compose.runtime.mutableStateOf("") }
    var isOtherSelected by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Observar cuando Firebase termine de guardar exitosamente
    LaunchedEffect(feedbackState) {
        if (feedbackState is Resource.Success) {
            // Antes de devolver al menú, si pasaron 5 minutos mostramos un anuncio
            if (activity != null) {
                adManager.showAdIfCoolDownPassed(activity) {
                    onFinish()
                }
            } else {
                onFinish()
            }
        }
    }
    
    // Interceptar el botón "Atrás" del celular
    var showBackWarning by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler {
        showBackWarning = true
    }

    if (showBackWarning) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2500)
            showBackWarning = false
        }
    }

    // Cuadro de diálogo para reportar problemas técnicos
    if (showIssueDialog) {
        AlertDialog(
            onDismissRequest = { 
                showIssueDialog = false 
                isOtherSelected = false
                issueTextInput = ""
            },
            title = { Text(stringResource(R.string.connection_issue_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.connection_issue_question))
                    if (isOtherSelected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = issueTextInput,
                            onValueChange = { issueTextInput = it },
                            placeholder = { Text(stringResource(R.string.connection_issue_placeholder)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (isOtherSelected) {
                    Button(
                        onClick = {
                            viewModel.submitFeedback(roomId, 0, false, "OTHER", issueTextInput) // i18n: technical Feedback issue code sent unchanged to Firestore.
                            showIssueDialog = false
                            isOtherSelected = false
                            issueTextInput = ""
                        },
                        enabled = issueTextInput.isNotBlank()
                    ) {
                        Text(stringResource(R.string.send_issue_detail))
                    }
                }
            },
            dismissButton = {
                if (!isOtherSelected) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { 
                            viewModel.submitFeedback(roomId, 0, false, "AUDIO_BAD") // i18n: technical Feedback issue code sent unchanged to Firestore.
                            showIssueDialog = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.audio_issue))
                        }
                        TextButton(onClick = { 
                            viewModel.submitFeedback(roomId, 0, false, "DISCONNECTED") // i18n: technical Feedback issue code sent unchanged to Firestore.
                            showIssueDialog = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.disconnection_issue))
                        }
                        TextButton(onClick = { 
                            isOtherSelected = true
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.other_issue))
                        }
                        TextButton(onClick = { 
                            showIssueDialog = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    TextButton(onClick = { 
                        isOtherSelected = false
                        issueTextInput = ""
                    }) {
                        Text(stringResource(R.string.back))
                    }
                }
            }
        )
    }

    // Snackbar de Compose
    if (showBackWarning) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.feedback_reminder),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }
        }
    }

    // Pregunta unificada para ambos roles: El enfoque es la conexión humana
    val questionText = stringResource(R.string.feedback_question)

    // Opciones 100% neutrales
    val options = stringArrayResource(R.array.feedback_ratings).mapIndexed { index, label -> index + 1 to label }
    val thanksText = stringResource(R.string.feedback_thanks, partnerAlias)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.feedback_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                    append(thanksText)
                    val aliasStart = thanksText.lastIndexOf(partnerAlias)
                    if (aliasStart >= 0) {
                        addStyle(
                            androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold),
                            aliasStart,
                            aliasStart + partnerAlias.length
                        )
                    }
                },
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = questionText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            options.forEach { (value, label) ->
                val isSelected = rating == value
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { rating = value },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Acciones inferiores
        if (feedbackState is Resource.Loading) {
            androidx.compose.material3.CircularProgressIndicator()
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        viewModel.submitFeedback(roomId, rating, isReport = false)
                    },
                    enabled = rating > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.send_feedback_and_leave),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.unexpected_issue),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { showIssueDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.technical_issue), fontSize = 13.sp)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.submitFeedback(roomId, rating = 0, isReport = true)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.report_user), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }

                if (feedbackState is Resource.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (feedbackState as Resource.Error).localizedMessage(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
