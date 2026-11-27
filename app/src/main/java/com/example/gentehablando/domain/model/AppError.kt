package com.example.gentehablando.domain.model

sealed interface AppError {
    data object EmptyGoogleToken : AppError
    data object GoogleConfiguration : AppError
    data object GoogleCancelled : AppError
    data class GooglePopup(val statusCode: Int) : AppError
    data object InvalidGoogleUser : AppError
    data object GoogleAuthentication : AppError
    data object DeleteAccount : AppError
    data object SendMessage : AppError
    data object UpdateTyping : AppError
    data object LeaveRoom : AppError
    data object RoomNotFound : AppError
    data object NotRoomMember : AppError
    data object InvalidPartner : AppError
    data object SendFeedback : AppError
    data object SendAppeal : AppError
    data object NotAuthenticated : AppError
    data object EmptyMessage : AppError
}
