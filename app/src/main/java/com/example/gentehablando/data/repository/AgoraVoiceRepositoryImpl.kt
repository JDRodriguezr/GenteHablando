package com.example.gentehablando.data.repository

import android.content.Context
import android.util.Log
import com.example.gentehablando.R
import com.example.gentehablando.domain.repository.VoiceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class AgoraVoiceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceRepository {

    private var rtcEngine: RtcEngine? = null
    private val isMutedFlow = MutableStateFlow(true) // Entramos muteados por defecto por privacidad
    
    private val isLocalSpeakingFlow = MutableStateFlow(false)
    private val isRemoteSpeakingFlow = MutableStateFlow(false)
    private var localUid: Int = 0 // Guardaremos el ID único que nos tocó
    
    // Variables para evitar el parpadeo de las luces (Debounce de apagado de 1 segundo)
    private var lastLocalSpeakingTime: Long = 0
    private var lastRemoteSpeakingTime: Long = 0

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            Log.d("AgoraVoice", "Conectado al canal de voz: $channel con uid local $uid") // i18n: technical Diagnostic log tag/message; never displayed.
            localUid = uid // Agora nos confirma cuál fue nuestro ID numérico aleatorio
            // Cuando entramos exitosamente, aplicamos el estado inicial (silenciado)
            rtcEngine?.muteLocalAudioStream(isMutedFlow.value)
        }
        
        override fun onError(err: Int) {
            super.onError(err)
            Log.e("AgoraVoice", "Error en Agora SDK: $err") // i18n: technical Diagnostic log tag/message; never displayed.
        }

        override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
            val now = System.currentTimeMillis()
            var anyRemoteSpeaking = false
            var anyLocalSpeaking = false

            speakers?.forEach { speaker ->
                // Bajamos el umbral para asegurar que capture incluso respiración fuerte o susurros
                if (speaker.volume > 1) { 
                    // Agora reporta uid = 0 SIEMPRE para el usuario local, sin importar el ID con el que entró
                    if (speaker.uid == 0) {
                        // Solo actualiza el estado local si NO estamos muteados.
                        // (A veces Agora reporta ruido de hardware incluso estando en mute).
                        if (!isMutedFlow.value) {
                             anyLocalSpeaking = true
                        }
                    } else {
                        // Si el uid NO es 0, significa que la otra persona está hablando y mandando audio
                        anyRemoteSpeaking = true
                    }
                }
            }
            
            // Si detectamos voz, actualizamos el tiempo; si no, dejamos el tiempo viejo
            if (anyLocalSpeaking) lastLocalSpeakingTime = now
            if (anyRemoteSpeaking) lastRemoteSpeakingTime = now
            
            // La luz se mantiene encendida si habló en el último medio segundo (500 ms)
            // Esto hace que el LED sea muy reactivo
            isLocalSpeakingFlow.value = (now - lastLocalSpeakingTime) < 500
            isRemoteSpeakingFlow.value = (now - lastRemoteSpeakingTime) < 500
        }
    }

    private fun initializeEngineIfNeeded() {
        if (rtcEngine != null) return

        try {
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = context.getString(R.string.agora_app_id)
            config.mEventHandler = rtcEventHandler
            rtcEngine = RtcEngine.create(config)
            
            // Perfil de audio de alta calidad, orientado a conversaciones puras
            rtcEngine?.setAudioProfile(
                Constants.AUDIO_PROFILE_SPEECH_STANDARD,
                Constants.AUDIO_SCENARIO_CHATROOM
            )
            
            // FILTROS AVANZADOS DE AUDIO (Cancelación de ruido y eco)
            // i18n: technical Agora SDK parameter JSON; not display content.
            rtcEngine?.setParameters("{\"che.audio.enable.ns\":true}") // Habilitar Supresión de Ruido
            // i18n: technical Agora SDK parameter JSON; not display content.
            rtcEngine?.setParameters("{\"che.audio.enable.aec\":true}") // Habilitar Cancelación de Eco
            // i18n: technical Agora SDK parameter JSON; not display content.
            rtcEngine?.setParameters("{\"che.audio.enable.agc\":true}") // Habilitar Control de Ganancia Automática (nivela el volumen)
            
            // Filtro de Inteligencia Artificial para supresión de ruido fuerte (AINS - AI Noise Suppression)
            // Esto elimina ruido de teclado, tráfico, ventiladores, manteniendo solo la voz humana
            rtcEngine?.setParameters("{\"che.audio.ains_mode\": 1}") // i18n: technical Agora SDK parameter JSON; not display content.

            Log.d("AgoraVoice", "Motor de Agora inicializado con Filtros de Audio AINS. App ID: ${config.mAppId.take(5)}...") // i18n: technical Diagnostic log tag/message; never displayed.
        } catch (e: Exception) {
            Log.e("AgoraVoice", "Fallo crítico inicializando Agora", e) // i18n: technical Diagnostic log tag/message; never displayed.
        }
    }

    override suspend fun joinChannel(channelId: String, userId: String) {
        initializeEngineIfNeeded()

        val options = ChannelMediaOptions()
        // Modo comunicación: Menos latencia, ideal para chat de voz 1 a 1
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        options.publishMicrophoneTrack = true
        options.autoSubscribeAudio = true

        // 1. INTERVALO Y SENSIBILIDAD:
        // Reportar volumen cada 200ms.
        // smoothing = 3 (suaviza la curva para evitar parpadeos).
        // report_vad = true (reporta la actividad de voz local).
        rtcEngine?.enableAudioVolumeIndication(200, 3, true)

        // --- INICIO MODO PRUEBA DE TOKEN ---
        // Generamos un ID pseudo-aleatorio para cada dispositivo para que ambos puedan entrar.
        val randomUid = (1..9999).random() 
        // i18n: technical Existing Agora test credential; consumed by the SDK, never displayed.
        val token = "007eJxTYFj3+d3vwuePV36+9415s3GN9RPexYsXfryhEB1X8FUv9NUqBYYUEyNTkyRzM8vURCDTwDwx0cLUMtEw0cLEPCXJxNDILuBXVkMgI8N070UsTAyMYAjiczMUJ+YkxhcUlaYmJTIwMMElGBkMAbmJJ04="
        
        // 2. UNIRSE AL CANAL CON EL UID:
        val joinResult = rtcEngine?.joinChannel(token, "sala_prueba", randomUid, options) // i18n: technical Existing shared test-channel name; protocol identity, not a display label.
        Log.d("AgoraVoice", "Intentando unirse a sala_prueba con UID $randomUid. Código de resultado: $joinResult") // i18n: technical Diagnostic log tag/message; never displayed.
        // --- FIN MODO PRUEBA DE TOKEN ---
        
        isMutedFlow.value = true
        isLocalSpeakingFlow.value = false
        isRemoteSpeakingFlow.value = false
        rtcEngine?.muteLocalAudioStream(true)
    }

    override suspend fun leaveChannel() {
        rtcEngine?.leaveChannel()
        isLocalSpeakingFlow.value = false
        isRemoteSpeakingFlow.value = false
    }

    override fun muteLocalAudio(muted: Boolean) {
        isMutedFlow.value = muted
        rtcEngine?.muteLocalAudioStream(muted)
        if (muted) {
            isLocalSpeakingFlow.value = false
        }
    }

    override fun observeMuteState(): Flow<Boolean> = isMutedFlow.asStateFlow()
    
    override fun observeLocalSpeakingState(): Flow<Boolean> = isLocalSpeakingFlow.asStateFlow()
    
    override fun observeRemoteSpeakingState(): Flow<Boolean> = isRemoteSpeakingFlow.asStateFlow()

    override fun destroyEngine() {
        RtcEngine.destroy()
        rtcEngine = null
    }

    override fun setAwaNoiseSuppression(enabled: Boolean) {
        try {
            if (enabled) {
                rtcEngine?.setParameters("{\"che.audio.enable.ns\":true}") // i18n: technical Agora SDK parameter JSON; not display content.
                rtcEngine?.setParameters("{\"che.audio.ains_mode\":1}") // i18n: technical Agora SDK parameter JSON; not display content.
                Log.d("AgoraVoice", "Filtro de Ruido IA: ACTIVADO") // i18n: technical Diagnostic log tag/message; never displayed.
            } else {
                rtcEngine?.setParameters("{\"che.audio.enable.ns\":false}") // i18n: technical Agora SDK parameter JSON; not display content.
                rtcEngine?.setParameters("{\"che.audio.ains_mode\":0}") // i18n: technical Agora SDK parameter JSON; not display content.
                Log.d("AgoraVoice", "Filtro de Ruido IA: DESACTIVADO") // i18n: technical Diagnostic log tag/message; never displayed.
            }
        } catch (e: Exception) {
            Log.e("AgoraVoice", "Error cambiando estado de filtro de ruido", e) // i18n: technical Diagnostic log tag/message; never displayed.
        }
    }
}
