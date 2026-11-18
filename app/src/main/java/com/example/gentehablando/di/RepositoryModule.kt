package com.example.gentehablando.di

import com.example.gentehablando.data.repository.FirebaseAuthRepositoryImpl
import com.example.gentehablando.data.repository.FirebaseChatRepositoryImpl
import com.example.gentehablando.data.repository.FirebaseMatchmakingRepositoryImpl
import com.example.gentehablando.domain.repository.AuthRepository
import com.example.gentehablando.domain.repository.ChatRepository
import com.example.gentehablando.domain.repository.MatchmakingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que define cómo resolver las dependencias de los Repositorios.
 * Mapea las interfaces del Dominio a sus implementaciones reales en Firebase.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMatchmakingRepository(
        impl: FirebaseMatchmakingRepositoryImpl
    ): MatchmakingRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: FirebaseChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindVoiceRepository(
        impl: com.example.gentehablando.data.repository.AgoraVoiceRepositoryImpl
    ): com.example.gentehablando.domain.repository.VoiceRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: com.example.gentehablando.data.repository.FirebaseUserRepositoryImpl
    ): com.example.gentehablando.domain.repository.UserRepository

    @Binds
    @Singleton
    abstract fun bindCourseRepository(
        impl: com.example.gentehablando.data.repository.CourseRepositoryImpl
    ): com.example.gentehablando.domain.repository.CourseRepository
}
