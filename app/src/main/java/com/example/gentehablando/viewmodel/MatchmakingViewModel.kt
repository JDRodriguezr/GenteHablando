package com.example.gentehablando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.isActive
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.domain.repository.AuthRepository
import com.example.gentehablando.domain.repository.MatchmakingRepository
import com.example.gentehablando.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Los posibles estados en los que puede estar la pantalla
sealed interface MatchmakingUiState {
    object Idle : MatchmakingUiState                         // Reposo / No ha empezado
    data class Searching(val secondsWaiting: Int) : MatchmakingUiState // Buscando activamente
    data class Matched(val partnerRole: UserRole, val roomId: String, val isDemo: Boolean = false) : MatchmakingUiState
}

@HiltViewModel
class MatchmakingViewModel @Inject constructor(
    private val matchmakingRepository: MatchmakingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchmakingUiState>(MatchmakingUiState.Idle)
    val uiState: StateFlow<MatchmakingUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var timerJob: Job? = null

    private suspend fun getOrSignInUserId(): String {
        val existingId = authRepository.getCurrentUserId()
        if (existingId != null) return existingId

        return when (val result = authRepository.signInAnonymously()) {
            is Resource.Success -> result.data
            else -> "user_" + System.currentTimeMillis() // i18n: technical Fallback user-ID prefix; not display content.
        }
    }

    fun startSearching(role: UserRole) {
        searchJob?.cancel()
        timerJob?.cancel()

        searchJob = viewModelScope.launch {
            val currentUserId = getOrSignInUserId()
            matchmakingRepository.observeMatchmaking(role, currentUserId).collect { state ->
                _uiState.value = state
                if (state is MatchmakingUiState.Matched) {
                    timerJob?.cancel() // Detener timer si encontramos pareja
                }
            }
        }
        
        // Manejo del temporizador de espera en pantalla (180 segundos = 3 minutos max)
        timerJob = viewModelScope.launch {
            var elapsedSeconds = 0
            while (isActive && _uiState.value is MatchmakingUiState.Searching) {
                kotlinx.coroutines.delay(1000L)
                elapsedSeconds++
                
                // Si pasaron 3 minutos (180 segundos)
                if (elapsedSeconds >= 180) {
                    // TODO: Implementar unión universal sin importar el rol
                    _uiState.value = MatchmakingUiState.Searching(elapsedSeconds)
                } else {
                    _uiState.value = MatchmakingUiState.Searching(elapsedSeconds)
                }
            }
        }
    }

    fun cancelSearching() {
        searchJob?.cancel()
        timerJob?.cancel()
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
                matchmakingRepository.cancelMatchmaking(currentUserId)
            }
            _uiState.value = MatchmakingUiState.Idle
        }
    }
}
