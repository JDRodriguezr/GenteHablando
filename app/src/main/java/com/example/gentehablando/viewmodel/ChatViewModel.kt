package com.example.gentehablando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gentehablando.domain.model.ChatMessage
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.domain.repository.AuthRepository
import com.example.gentehablando.domain.repository.ChatRepository
import com.example.gentehablando.domain.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isPartnerTyping = MutableStateFlow(false)
    val isPartnerTyping: StateFlow<Boolean> = _isPartnerTyping.asStateFlow()

    private val _isRoomActive = MutableStateFlow(true)
    val isRoomActive: StateFlow<Boolean> = _isRoomActive.asStateFlow()
    
    // Temporizador de inactividad
    private val _showInactivityWarning = MutableStateFlow(false)
    val showInactivityWarning: StateFlow<Boolean> = _showInactivityWarning.asStateFlow()

    private val _inactivitySecondsLeft = MutableStateFlow(30)
    val inactivitySecondsLeft: StateFlow<Int> = _inactivitySecondsLeft.asStateFlow()

    val isLocalAudioMuted: StateFlow<Boolean> = kotlinx.coroutines.flow.flow { 
        voiceRepository.observeMuteState().collect { emit(it) } 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isLocalSpeaking: StateFlow<Boolean> = voiceRepository.observeLocalSpeakingState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPartnerSpeaking: StateFlow<Boolean> = voiceRepository.observeRemoteSpeakingState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isNoiseSuppressionEnabled = MutableStateFlow(true)
    val isNoiseSuppressionEnabled: StateFlow<Boolean> = _isNoiseSuppressionEnabled.asStateFlow()

    private var currentRoomId: String = ""
    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    private var roomStateJob: Job? = null
    private var voiceJob: Job? = null
    private var speakingJob: Job? = null
    private var inactivityTimerJob: Job? = null
    private var warningCountdownJob: Job? = null

    private suspend fun getOrSignInUserId(): String {
        val existingId = authRepository.getCurrentUserId()
        if (existingId != null) return existingId
        return when (val result = authRepository.signInAnonymously()) {
            is Resource.Success -> result.data
            else -> "user_" + System.currentTimeMillis()
        }
    }

    fun initRoom(roomId: String, partnerAlias: String) {
        android.util.Log.d("ChatViewModel", "initRoom ejecutándose. Preparando sala $roomId")
        currentRoomId = roomId

        _messages.value = emptyList()
        _isPartnerTyping.value = false
        _isRoomActive.value = true
        _showInactivityWarning.value = false

        messagesJob?.cancel()
        typingJob?.cancel()
        roomStateJob?.cancel()
        voiceJob?.cancel()
        speakingJob?.cancel()

        messagesJob = viewModelScope.launch {
            val currentUserId = getOrSignInUserId()
            chatRepository.observeMessages(roomId, currentUserId).collect { newMessages ->
                _messages.value = newMessages
                resetInactivityTimer()
            }
        }

        typingJob = viewModelScope.launch {
            val currentUserId = getOrSignInUserId()
            chatRepository.observePartnerTypingStatus(roomId, currentUserId).collect { isTyping ->
                _isPartnerTyping.value = isTyping
                if (isTyping) resetInactivityTimer()
            }
        }

        roomStateJob = viewModelScope.launch {
            chatRepository.observeRoomActiveStatus(roomId).collect { isActive ->
                _isRoomActive.value = isActive
                if (!isActive) {
                    voiceRepository.leaveChannel()
                    inactivityTimerJob?.cancel()
                    warningCountdownJob?.cancel()
                }
            }
        }

        // Conectar automáticamente al canal de voz de Agora
        voiceJob = viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "Intentando unirse a Agora...")
            val currentUserId = getOrSignInUserId()
            android.util.Log.d("ChatViewModel", "ID Obtenido: $currentUserId. Llamando a joinChannel.")
            voiceRepository.joinChannel(roomId, currentUserId)
        }
        
        speakingJob = viewModelScope.launch {
            voiceRepository.observeLocalSpeakingState().collect { isSpeaking ->
                if (isSpeaking) resetInactivityTimer()
            }
        }
        
        resetInactivityTimer()
    }

    fun toggleMute() {
        val currentMuteState = isLocalAudioMuted.value
        voiceRepository.muteLocalAudio(!currentMuteState)
        resetInactivityTimer()
    }

    fun toggleNoiseSuppression() {
        val newState = !_isNoiseSuppressionEnabled.value
        _isNoiseSuppressionEnabled.value = newState
        voiceRepository.setAwaNoiseSuppression(newState)
    }

    fun onInputTextChanged(text: String) {
        if (currentRoomId.isBlank() || !_isRoomActive.value) return
        viewModelScope.launch {
            val currentUserId = getOrSignInUserId()
            chatRepository.setTypingStatus(currentRoomId, currentUserId, text.isNotBlank())
            resetInactivityTimer()
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || currentRoomId.isBlank() || !_isRoomActive.value) return
        viewModelScope.launch {
            val currentUserId = getOrSignInUserId()
            chatRepository.sendMessage(currentRoomId, currentUserId, text)
            resetInactivityTimer()
        }
    }
    
    fun extendSession() {
        resetInactivityTimer()
    }

    private fun resetInactivityTimer() {
        if (!_isRoomActive.value) return
        _showInactivityWarning.value = false
        warningCountdownJob?.cancel()
        inactivityTimerJob?.cancel()
        
        inactivityTimerJob = viewModelScope.launch {
            delay(300_000L) // 5 Minutos (300,000 ms)
            
            _showInactivityWarning.value = true
            _inactivitySecondsLeft.value = 30
            
            warningCountdownJob = viewModelScope.launch {
                while (_inactivitySecondsLeft.value > 0) {
                    delay(1000L)
                    _inactivitySecondsLeft.value -= 1
                }
                leaveRoom() // Expiró el tiempo, lo sacamos
            }
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null && currentRoomId.isNotBlank()) {
                chatRepository.leaveRoom(currentRoomId, currentUserId)
                voiceRepository.leaveChannel()
            }
            // Limpieza
            messagesJob?.cancel()
            typingJob?.cancel()
            roomStateJob?.cancel()
            voiceJob?.cancel()
            speakingJob?.cancel()
            inactivityTimerJob?.cancel()
            warningCountdownJob?.cancel()
            
            _isPartnerTyping.value = false
            _isRoomActive.value = false
            _showInactivityWarning.value = false
            _messages.value = emptyList()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        voiceRepository.destroyEngine()
    }
}
