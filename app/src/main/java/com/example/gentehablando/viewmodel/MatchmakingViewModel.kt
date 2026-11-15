package com.example.gentehablando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    data class Matched(val partnerAlias: String, val roomId: String) : MatchmakingUiState // ¡Pareja encontrada!
}

@HiltViewModel
class MatchmakingViewModel @Inject constructor(
    private val matchmakingRepository: MatchmakingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchmakingUiState>(MatchmakingUiState.Idle)
    val uiState: StateFlow<MatchmakingUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    private suspend fun getOrSignInUserId(): String {
        val existingId = authRepository.getCurrentUserId()
        if (existingId != null) return existingId

        return when (val result = authRepository.signInAnonymously()) {
            is Resource.Success -> result.data
            else -> "user_" + System.currentTimeMillis()
        }
    }

    fun startSearching(role: UserRole) {
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            val currentUserId = getOrSignInUserId()
            matchmakingRepository.observeMatchmaking(role, currentUserId).collect { state ->
                _uiState.value = state
            }
        }
    }

    fun cancelSearching() {
        searchJob?.cancel()
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
                matchmakingRepository.cancelMatchmaking(currentUserId)
            }
            _uiState.value = MatchmakingUiState.Idle
        }
    }
}
