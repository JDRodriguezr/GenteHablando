package com.example.gentehablando.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gentehablando.R
import com.example.gentehablando.domain.model.AppError
import com.example.gentehablando.domain.model.Resource

@Composable
fun Resource.Error.localizedMessage(): String = when (val error = reason) {
    AppError.EmptyGoogleToken -> stringResource(R.string.error_google_empty_token)
    AppError.GoogleConfiguration -> stringResource(R.string.error_google_configuration)
    AppError.GoogleCancelled -> stringResource(R.string.error_google_cancelled)
    is AppError.GooglePopup -> stringResource(R.string.error_google_popup, error.statusCode)
    AppError.InvalidGoogleUser -> stringResource(R.string.error_google_user)
    AppError.GoogleAuthentication -> stringResource(R.string.error_google_authentication)
    AppError.DeleteAccount -> stringResource(R.string.error_delete_account)
    AppError.SendMessage -> stringResource(R.string.error_send_message)
    AppError.UpdateTyping -> stringResource(R.string.error_update_typing)
    AppError.LeaveRoom -> stringResource(R.string.error_leave_room)
    AppError.RoomNotFound -> stringResource(R.string.error_room_not_found)
    AppError.NotRoomMember -> stringResource(R.string.error_not_room_member)
    AppError.InvalidPartner -> stringResource(R.string.error_invalid_partner)
    AppError.SendFeedback -> stringResource(R.string.error_send_feedback)
    AppError.SendAppeal -> stringResource(R.string.error_send_appeal)
    AppError.NotAuthenticated -> stringResource(R.string.error_not_authenticated)
    AppError.EmptyMessage -> stringResource(R.string.error_empty_message)
}
