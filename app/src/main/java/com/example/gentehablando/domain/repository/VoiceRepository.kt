package com.example.gentehablando.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Contrato para el manejo de comunicación de voz en tiempo real.
 */
interface VoiceRepository {
    /**
     * Inicializa el motor de audio y se une al canal de voz de la sala.
     */
    suspend fun joinChannel(channelId: String, userId: String)

    /**
     * Abandona el canal de voz actual y libera los recursos del micrófono.
     */
    suspend fun leaveChannel()

    /**
     * Silencia o activa el micrófono del usuario local.
     */
    fun muteLocalAudio(muted: Boolean)

    /**
     * Observa el estado actual del micrófono (true si está silenciado).
     */
    fun observeMuteState(): Flow<Boolean>

    /**
     * Observa si el usuario local está hablando activamente por el micrófono.
     */
    fun observeLocalSpeakingState(): Flow<Boolean>

    /**
     * Observa si el compañero remoto está hablando activamente.
     */
    fun observeRemoteSpeakingState(): Flow<Boolean>

    /**
     * Limpia y destruye el motor de Agora para liberar memoria de forma segura.
     */
    fun destroyEngine()
}
