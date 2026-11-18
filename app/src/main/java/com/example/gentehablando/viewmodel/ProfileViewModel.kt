package com.example.gentehablando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.domain.model.UserProfile
import com.example.gentehablando.domain.repository.AuthRepository
import com.example.gentehablando.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()
    
    private val _actionState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val actionState: StateFlow<Resource<Unit>> = _actionState.asStateFlow()

    private var profileJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { uid ->
                profileJob?.cancel()
                if (uid != null && authRepository.isUserLoggedInWithAccount()) {
                    profileJob = viewModelScope.launch {
                        userRepository.observeUserProfile(uid).collect { profile ->
                            _userProfile.value = profile
                        }
                    }
                } else {
                    _userProfile.value = UserProfile() // Usuario limpio si desloguea
                }
            }
        }
    }

    fun submitAppeal(explanation: String, reportedRoomIds: List<String>) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val uid = authRepository.getCurrentUserId()
            if (uid != null) {
                val result = userRepository.submitAppeal(uid, explanation, reportedRoomIds)
                _actionState.value = result
            } else {
                _actionState.value = Resource.Error("No estás autenticado")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = Resource.Idle
    }

    fun signOut() {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            authRepository.signOut()
            _actionState.value = Resource.Success(Unit)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = authRepository.deleteAccount()
            _actionState.value = result
        }
    }
}
