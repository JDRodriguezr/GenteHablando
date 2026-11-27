package com.example.gentehablando.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gentehablando.model.UserRole
import com.example.gentehablando.viewmodel.MatchmakingUiState
import com.example.gentehablando.viewmodel.MatchmakingViewModel
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.gentehablando.R

@Composable
fun MatchmakingScreen(
    role: UserRole,
    viewModel: MatchmakingViewModel,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onMatchFound: (roomId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Apenas la pantalla entra, inicia la búsqueda
    LaunchedEffect(Unit) {
        viewModel.startSearching(role)
    }

    // En cuanto se encuentra un emparejamiento, navega de inmediato a la sala de chat
    LaunchedEffect(uiState) {
        if (uiState is MatchmakingUiState.Matched) {
            val matchedState = uiState as MatchmakingUiState.Matched
            onMatchFound(matchedState.roomId)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        when (val state = uiState) {
            is MatchmakingUiState.Idle -> {
                Text(text = stringResource(R.string.preparing_search), fontSize = 16.sp)
            }

            is MatchmakingUiState.Searching -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 5.dp
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = stringResource(if (role == UserRole.SPEAKER) R.string.searching_listener else R.string.searching_speaker),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = pluralStringResource(R.plurals.waiting_time, state.secondsWaiting, state.secondsWaiting),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is MatchmakingUiState.Matched -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 5.dp
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = stringResource(R.string.match_found),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.entering_room,
                            stringResource(
                                when {
                                    state.isDemo && state.partnerRole == UserRole.LISTENER -> R.string.demo_listener_alias
                                    state.isDemo -> R.string.demo_speaker_alias
                                    state.partnerRole == UserRole.LISTENER -> R.string.listener_label
                                    else -> R.string.speaker_label
                                }
                            )
                        ),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                viewModel.cancelSearching()
                onCancel()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.cancel_search))
        }
    }
}
