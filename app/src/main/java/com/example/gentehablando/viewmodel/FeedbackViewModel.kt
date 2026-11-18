package com.example.gentehablando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.domain.repository.AuthRepository
import com.example.gentehablando.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _feedbackState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val feedbackState: StateFlow<Resource<Unit>> = _feedbackState.asStateFlow()

    fun submitFeedback(roomId: String, rating: Int, isReport: Boolean, issueType: String? = null, issueText: String? = null) {
        viewModelScope.launch {
            _feedbackState.value = Resource.Loading
            val currentUserId = authRepository.getCurrentUserId()
            
            if (currentUserId != null) {
                val result = userRepository.submitFeedback(roomId, currentUserId, rating, isReport, issueType, issueText)
                _feedbackState.value = result
            } else {
                _feedbackState.value = Resource.Error("Sesión expirada o usuario no autenticado")
            }
        }
    }
}
