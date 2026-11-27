package com.example.gentehablando.data.repository

import com.example.gentehablando.domain.repository.MatchmakingRepository
import com.example.gentehablando.model.UserRole
import com.example.gentehablando.viewmodel.MatchmakingUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FakeMatchmakingRepositoryImpl @Inject constructor() : MatchmakingRepository {

    override fun observeMatchmaking(role: UserRole, userId: String): Flow<MatchmakingUiState> = flow {
        var elapsed = 0
        emit(MatchmakingUiState.Searching(elapsed))

        while (elapsed < 4) {
            delay(1000L)
            elapsed++
            emit(MatchmakingUiState.Searching(elapsed))
        }

        val partnerRole = if (role == UserRole.SPEAKER) UserRole.LISTENER else UserRole.SPEAKER
        emit(
            MatchmakingUiState.Matched(
                partnerRole = partnerRole,
                roomId = "room_fake_999", // i18n: technical Synthetic room ID; not a display alias.
                isDemo = true
            )
        )
    }

    override suspend fun cancelMatchmaking(userId: String) {
        // En implementación real, elimina la solicitud en Firestore
    }
}
