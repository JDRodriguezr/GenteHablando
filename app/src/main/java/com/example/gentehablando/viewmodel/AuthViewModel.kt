package com.example.gentehablando.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gentehablando.domain.model.Resource
import com.example.gentehablando.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val loginState: StateFlow<Resource<String>> = _loginState.asStateFlow()

    // Solo se usará la app si se está logueado de verdad (no anónimo)
    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedInWithAccount()
    }

    fun setLoading() {
        _loginState.value = Resource.Loading
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            _loginState.value = result
        }
    }
    
    fun setError(msg: String) {
        _loginState.value = Resource.Error(msg)
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
