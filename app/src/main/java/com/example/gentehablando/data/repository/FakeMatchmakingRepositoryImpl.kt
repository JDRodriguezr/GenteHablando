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

        val matchAlias = if (role == UserRole.SPEAKER) "Oyente_Amable_42" else "Hablante_Sincero_07"
        emit(
            MatchmakingUiState.Matched(
                partnerAlias = matchAlias,
                roomId = "room_fake_999"
            )
        )
    }

    override suspend fun cancelMatchmaking(userId: String) {
        // En implementación real, elimina la solicitud en Firestore
    }
}
